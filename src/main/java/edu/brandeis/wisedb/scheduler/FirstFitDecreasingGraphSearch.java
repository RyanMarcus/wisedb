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

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class FirstFitDecreasingGraphSearch implements GraphSearcher {


	private ModelSLA sla;
	private QueryTimePredictor qtp;
	private boolean reverse;
	
	public FirstFitDecreasingGraphSearch(ModelSLA sla, QueryTimePredictor qtp) {
		this.sla = sla;
		this.qtp = qtp;
		this.reverse = false;
	}
	
	public FirstFitDecreasingGraphSearch(ModelSLA sla, QueryTimePredictor qtp, boolean reverse) {
		this.sla = sla;
		this.qtp = qtp;
		this.reverse = reverse;
	}

	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		ModelQuery[] sorted = toSched.toArray(new ModelQuery[] {});

		// sort the queries descending
		Comparator<ModelQuery> comp = new QueryComparator();
		if (reverse) {
			comp = comp.reversed();
		}
		Arrays.sort(sorted, comp);

		List<ModelVM> vms = new LinkedList<ModelVM>();

		
		queryLoop: for (ModelQuery q : sorted) {
			// look for a machine where the query will "fit"
			for (ModelVM vm : vms) {
				

				vm.addQuery(q);
				int penalty = sla.calculatePenalty(vm.getQueryLatencies(qtp));
				if (penalty > 0) {
					vm.removeQuery(q);
					continue;
				}

				// if we're here, the query was assigned to the VM!
				continue queryLoop;
			}

			// if we're here, there was no VM that could fit our query...
			// find a VM type that can handle our query
			Optional<ModelVM> vm = qtp.getNewVMs().stream()
					.min((a, b) -> a.compareTo(b));
			
			ModelVM toAdd = vm.get();
			toAdd.addQuery(q);
			vms.add(toAdd);
		}

		// now construct a sequence of actions that will get us where we need to go
		List<Action> toR = new LinkedList<Action>();
		for (ModelVM vm : vms) {
			toR.add(new StartNewVMAction(vm));
			for (ModelQuery q : vm.getQueries()) {
				toR.add(new AssignQueryAction(q, vm));
			}
		}

		return toR;

	}

	private class QueryComparator implements Comparator<ModelQuery> {

		@Override
		public int compare(ModelQuery o1, ModelQuery o2) {
			return qtp.predict(o2, VMType.T2_SMALL) - qtp.predict(o1, VMType.T2_SMALL);
		}

	}

}
