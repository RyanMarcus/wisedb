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

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.training.decisiontree.SingularMachineState;

public class PackUntilViolationGraphSearch implements GraphSearcher {


	private ModelSLA sla;
	private QueryTimePredictor qtp;

	private static final Logger log = Logger.getLogger(PackUntilViolationGraphSearch.class.getName());


	public PackUntilViolationGraphSearch(QueryTimePredictor qtp, ModelSLA sla) {
		this.sla = sla;
		this.qtp = qtp;
	}

	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		State state = new SingularMachineState(toSched, qtp, sla);
		QueryComparator qc = new QueryComparator();
		List<Action> toR = new LinkedList<>();

		while (!state.isGoalState()) {
			Optional<AssignQueryAction> place = state.getPossibleActions().stream()
					.filter(a -> a instanceof AssignQueryAction)
					.map(a -> (AssignQueryAction) a)
					.max(qc);

			if (place.isPresent()) {
				// see if it violates
				State possible = state.getNewStateForAction(place.get());
				if (possible.getDetailedExecutionCost().getPenaltyCost() == 0) {
					log.fine("Picking non-violating action: " + place.get());
					state = possible;
					toR.add(place.get());
					continue;
				}
			}

			// find the type of the largest query we still have left
			Optional<ModelQuery> mq = state.getUnassignedQueries().stream()
					.max((a, b) -> qtp.predict(a, VMType.T2_MEDIUM) - qtp.predict(b, VMType.T2_MEDIUM));
			
			if (!mq.isPresent()) {
				// no more queries to assign?
				log.severe("No maximum query found!");
				return null;
			}
			
			// otherwise, create a new VM that can handle our largest query
			Optional<StartNewVMAction> newVM = state.getPossibleActions().stream()
					.filter(a -> a instanceof StartNewVMAction)
					.map(a -> (StartNewVMAction) a)
					.min((a, b) -> a.getVM().compareTo(b.getVM()));
			
			toR.add(newVM.get());
			state = state.getNewStateForAction(newVM.get());
		}
		
		return toR;
	}






	private class QueryComparator implements Comparator<AssignQueryAction> {

		@Override
		public int compare(AssignQueryAction o1, AssignQueryAction o2) {
			ModelVM t = new ModelVM(VMType.T2_MEDIUM);
			return qtp.predict(o1.getQuery(), t) - qtp.predict(o2.getQuery(), t);
		}

	}


}
