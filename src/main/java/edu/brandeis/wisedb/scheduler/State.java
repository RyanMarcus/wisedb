// { begin copyright } 
// Copyright Ryan Marcus 2016
// 
// This file is part of WiSeDB.
// 
// WiSeDB is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// WiSeDB is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with WiSeDB.  If not, see <http://www.gnu.org/licenses/>.
// 
// { end copyright } 
 
 

package edu.brandeis.wisedb.scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.Cost;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.training.decisiontree.SingularMachineState;

public abstract class State implements Cloneable {
	private ModelSLA sla;
	private QueryTimePredictor qtp;



	public State(ModelSLA sla, QueryTimePredictor qtp) {
		this.sla = sla;
		this.qtp = qtp;
	}	


	

	public abstract Set<Action> getPossibleActions();
	public abstract State getNewStateForAction(Action a);

	public abstract Set<ModelQuery> getUnassignedQueries();

	/**
	 * The collection of VMs at this state. Must have consistent iterable ordering.
	 * @return the VMs
	 */
	public abstract Collection<ModelVM> getVMs();

	public abstract SortedMap<String, String> getFeatures();

	public int getCostOfAction(Action a) {
		int currentCost = getDetailedExecutionCost().getTotalCost();

		State nextState = getNewStateForAction(a);
		Cost nextCost = nextState.getDetailedExecutionCost();

		return nextCost.getTotalCost() - currentCost;
	}

	public Cost getDetailedExecutionCost() {
		int currentCost = 0;
		Cost cost = new Cost();
		Map<ModelQuery, Integer> latencies = new HashMap<ModelQuery, Integer>();

		for (ModelVM vm : getVMs()) {
			latencies.putAll(vm.getQueryLatencies(qtp));
			currentCost += vm.getCostForQueries(qtp);
		}

		cost.setQueriesCost(currentCost);

		int penaltyCost = sla.calculatePenalty(latencies);
		cost.setPenaltyCost(penaltyCost);

		int VMBootCost = 0;
		for (ModelVM vm : getVMs()) {
			VMBootCost += vm.getCostToBoot();
		}
		cost.setVMBootCost(VMBootCost);

		return cost;
	}

	public int getSLAPenalty() {
		return getDetailedExecutionCost().getPenaltyCost();
	}

	public boolean isGoalState() {
		return this.getUnassignedQueries().isEmpty();
	}


	

	
	public abstract void noteBValue(int b);

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("< ");

		for (ModelVM vm : getVMs()) {
			sb.append("[");
			sb.append(vm.getQueries().stream()
					.map(q -> String.valueOf(q.getType()))
					.collect(Collectors.joining(",")));
			sb.append("] ");
		}

		sb.append("{");
		sb.append(this.getUnassignedQueries().stream()
				.sorted()
				.map(q -> String.valueOf(q.getType()))
				.collect(Collectors.joining(",")));
		sb.append("} >");
		return sb.toString();
	}

	
	
	@Override
	public boolean equals(Object other) {
		
		
		if (!(other instanceof State)) {
			return false;
		}

		State o = (State) other;

		if (!getUnassignedQueries().equals(o.getUnassignedQueries()))
			return false;
		
		if (!getVMs().equals(o.getVMs()))
			return false;

		return true;


	}

	@Override
	public int hashCode() {
		return (997 * getVMs().hashCode()) ^ (991 * getUnassignedQueries().hashCode());
	}


//	private boolean isOrderRelevant(QueryTimePredictor qtp, ModelSLA sla) {
//		// test to see if the order of the queries really matter.
//		
//	}
	
	public List<Action> toActions(QueryTimePredictor qtp, ModelSLA sla) {
		List<Action> toR = new LinkedList<Action>();
		Set<ModelQuery> allQ = getVMs().stream()
				.flatMap(vm -> vm.getQueries().stream())
				.collect(Collectors.toSet());

		SingularMachineState s = new SingularMachineState(allQ, qtp, sla);

		List<ModelVM> sorted = new LinkedList<ModelVM>(getVMs());
		sorted.sort(Collections.reverseOrder((a, b) -> {


			int aMax = a.getQueries().stream().mapToInt(q -> qtp.predict(q, VMType.T2_SMALL)).max().getAsInt();
			int bMax = b.getQueries().stream().mapToInt(q -> qtp.predict(q, VMType.T2_SMALL)).max().getAsInt();
			if (aMax != bMax)
				return aMax - bMax;


			return a.getQueries().size() - b.getQueries().size();


		}));

		for (ModelVM vm : sorted) {
			Action newVM = new StartNewVMAction(new ModelVM(vm.getType()), s);
			toR.add(newVM);
			s = s.getNewStateForAction(newVM);

			//System.out.println(s);
			List<ModelQuery> queries = new LinkedList<ModelQuery>(vm.getQueries());

			if (!sla.queryOrderMatters()) {
				Collections.sort(queries, Collections.reverseOrder((a, b) -> {
					return qtp.predict(a, VMType.T2_SMALL) - qtp.predict(b, VMType.T2_SMALL);
				}));
			}

			for (ModelQuery q : queries) {
				Action assign = new AssignQueryAction(q, vm, s);
				toR.add(assign);
				s = s.getNewStateForAction(assign);
			}
		}

		return toR;
	}


}
