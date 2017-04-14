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
 
 

package edu.brandeis.wisedb.scheduler.training.decisiontree;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.AssignQueryAction;
import edu.brandeis.wisedb.scheduler.StartNewVMAction;
import edu.brandeis.wisedb.scheduler.State;

public class SingularMachineState extends State {
	
	
	
	public static boolean ALLOW_VIOLATIONS = true;
	
	private Deque<ModelVM> vms;
	private Set<ModelQuery> unassigned;
	private ModelSLA sla;
	private QueryTimePredictor qtp;
	
	private int bValue;
	
	private static final Logger log = Logger.getLogger(SingularMachineState.class.getName());

	private static boolean GIVEN_WARNING_VIOLATIONS = false;
	
	public SingularMachineState(Set<ModelQuery> unassigned, QueryTimePredictor qtp, ModelSLA sla) {
		this(new LinkedList<ModelVM>(), unassigned, qtp, sla);
	}
	
	public SingularMachineState(Deque<ModelVM> vms, Set<ModelQuery> unassigned, QueryTimePredictor qtp, ModelSLA sla) {
		super(sla, qtp);
		this.vms = vms;
		this.unassigned = new HashSet<ModelQuery>(unassigned);
		this.sla = sla;
		this.qtp = qtp;
		

		
		if (!GIVEN_WARNING_VIOLATIONS && !ALLOW_VIOLATIONS) {
			log.warning("Not allowing violations!");
			GIVEN_WARNING_VIOLATIONS = true;
		}
	}
	
	public Set<Action> getPossibleActions() {
		Set<Action> toR = new TreeSet<Action>();
	
		
		// can only create a new VM if the last VM
		// has at least one query on it
		if (vms.peek() == null || !vms.peek().getQueries().isEmpty()) {
			for (ModelVM c : qtp.getNewVMs()) {
			
				
				toR.add(new StartNewVMAction(c, this));
			}
		}
		
		// if we haven't started a single VM, then we can't take
		// any other actions
		if (vms.peek() == null) {
			return toR;
		}
		
		// can assign any *single* query of a certain type
		for (Integer i : qtp.QUERY_TYPES) {
			Optional<ModelQuery> candidate = unassigned.stream()
					.filter(q -> q.getType() == i)
					.findAny();

			if (!candidate.isPresent())
				continue;
			
			Action proposed = new AssignQueryAction(candidate.get(), vms.peek(), this);
			
			if (!ALLOW_VIOLATIONS) {
				int penalty = getNewStateForAction(proposed).getSLAPenalty();
				if (penalty > 0)
					continue;
			}
			
			toR.add(proposed);
		}
		
		
		
		return toR;
	}
	
	public SingularMachineState getNewStateForAction(Action a) {
		SingularMachineState toR = clone();

		if (a instanceof StartNewVMAction) {
			ModelVM toAdd = new ModelVM((((StartNewVMAction) a).getVM().getType())); 
			toR.vms.push(toAdd);
			return toR;
		}
		
		if (a instanceof AssignQueryAction) {
			toR.vms.peek().addQuery(((AssignQueryAction) a).getQuery());
			toR.unassigned.remove(((AssignQueryAction) a).getQuery());
			
			if (!sla.queryOrderMatters()) {
				toR.vms.peek().sort(qtp);
			}
			
			return toR;
		}
		
		
		log.warning("Found unknown action type: " + a);
		
		return null;
		
	}
	
	public void applyAction(Action a) {
		if (a instanceof StartNewVMAction) {
			((StartNewVMAction) a).getVM().removeAllQueries();
			vms.push(((StartNewVMAction) a).getVM());
			return;
		}
		
		if (a instanceof AssignQueryAction) {
			vms.peek().addQuery(((AssignQueryAction) a).getQuery());
			unassigned.remove(((AssignQueryAction) a).getQuery());
			return;
		}
		
		log.warning("Found unknown action type: " + a);
	}
	
	public SingularMachineState clone() {
		SingularMachineState toR = new SingularMachineState(new LinkedList<ModelVM>(), new HashSet<ModelQuery>(), qtp, sla);
		
		for (ModelVM vm : vms) {
			toR.vms.add(vm.clone());
		}
		
		for (ModelQuery mq : unassigned) {
			toR.unassigned.add(mq);
		}
		
		toR.noteBValue(bValue);
		
		return toR;
		
	}

	@Override
	public Set<ModelQuery> getUnassignedQueries() {
		return unassigned;
	}

	@Override
	public Collection<ModelVM> getVMs() {
		return vms;
	}
	
	public ModelVM getLastVM() {
		if (vms.isEmpty()) return null;
		return vms.peek();
	}

	@Override
	public SortedMap<String, String> getFeatures() {
		SortedMap<String, String> toR = new TreeMap<String, String>();
		
		toR.put("is-empty", (vms.isEmpty() ? "Y" : "N"));
		if (vms.peek() == null || vms.peek().getQueries().isEmpty()) {
			toR.put("last-machine-empty", "Y");
		} else {
			toR.put("last-machine-empty", "N");
		}
		
		if (vms.peek() == null) {
			toR.put("last-vm-cost", "?");
		} else {
			toR.put("last-vm-cost", String.valueOf(vms.peek().getType().getCost()));
		}
		
		
		// for each query type, store its proportion of the current mix
		int totalQueries = (getLastVM() != null ? getLastVM().getQueries().size() : 0);
		for (Integer type : qtp.QUERY_TYPES) {
			if (getLastVM() != null) {
				long queriesOfType = getLastVM().getQueries().stream().filter(q -> q.getType() == type).count();
				toR.put("proportion-of-Q" + type, String.valueOf((float)queriesOfType/totalQueries));
				continue;
			}
			toR.put("proportion-of-Q" + type, "?");
		}
		
		
		
		// for each query type, indicate if assigning a new query of that type would violate the SLA
		
		for (Integer type : qtp.QUERY_TYPES) {

			
			if (vms.peek() == null) {
				toR.put("Q" + type + "-fits", "?");
				continue;
			}
			
			Optional<ModelQuery> largest = getUnassignedQueries().stream()
					.filter(q -> q.getType() == type)
					.max((a, b) -> qtp.predict(a, getLastVM()) - qtp.predict(b, getLastVM()));

			if (!largest.isPresent()) {
				toR.put("Q" + type + "-fits", "?");
				continue;
			}

			int oldPenalty = getSLAPenalty();
			State newState = getNewStateForAction(new AssignQueryAction(largest.get(), vms.peek()));
			int penalty = newState.getSLAPenalty();
			toR.put("Q" + type + "-fits", String.valueOf(penalty - oldPenalty));

		}
		
		
		for (Integer type : qtp.QUERY_TYPES) {
			boolean have = unassigned.stream().anyMatch(q -> q.getType() == type);
			toR.put("unassigned-Q" + type, (have ? "Y" : "N"));
		}
		
		// use the wait time of the most recent VM
		if (getLastVM() != null) {
			toR.put("wait-time", String.valueOf(vms.peek().getWaitingTime(qtp)));
		} else {
			toR.put("wait-time", "?");
		}
		
		// what queries can the most recent VM handle?
		Arrays.stream(qtp.QUERY_TYPES)
		.mapToObj(i -> new AbstractMap.SimpleImmutableEntry<Integer, Boolean>(i, (vms.peek() != null)))
		.map(o -> new AbstractMap.SimpleImmutableEntry<String, String>("supports-" + String.valueOf(o.getKey()), (o.getValue() ? "Y" : "N")))
		.forEach(o -> toR.put(o.getKey(), o.getValue()));

		return toR;
		
		
	}


	
//	private boolean haveQueriesOfType(Collection<ModelQuery> toCheck, int type, int num) {
//		if (toCheck == null)
//			return false;
//		
//		return toCheck.stream().filter(q -> q.getType() == type).count() == num;
//	}
//	
//	private boolean haveMoreQueriesOfType(Collection<ModelQuery> toCheck, int type, int num) {
//		if (toCheck == null)
//			return false;
//		
//		return toCheck.stream().filter(q -> q.getType() == type).count() > num;
//	}
//	
//	private int queriesOfType(int type) {
//		return (int) vms.peek().getQueries().stream().filter(q -> q.getType() == type).count();
//	}
//	
//	private int unassignedQueriesOfType(int type) {
//		return (int) getUnassignedQueries().stream().filter(q -> q.getType() == type).count();
//	}

	
	
	@Override
	public void noteBValue(int b) {
		bValue = b;
	}


	

	


}
