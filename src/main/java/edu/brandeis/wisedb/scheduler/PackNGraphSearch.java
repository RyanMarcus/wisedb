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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class PackNGraphSearch implements GraphSearcher {

	private final int n;
	private final QueryTimePredictor qtp;
	private final ModelSLA sla;

	public PackNGraphSearch(int n, QueryTimePredictor qtp, ModelSLA sla) {
		this.n = n;
		this.qtp = qtp;
		this.sla = sla;
	}

	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		List<ModelQuery> queries = new LinkedList<ModelQuery>(toSched);
		queries.sort((a, b) -> qtp.predict(a, qtp.getOneVM()) - qtp.predict(b, qtp.getOneVM()));
		Deque<ModelQuery> sorted = new LinkedList<ModelQuery>(queries);


		LinkedList<ModelVM> vms = new LinkedList<>();

		int i = 0;
		queryLoop: while (!sorted.isEmpty()) {
			ModelQuery q = (i++ % (n + 1) == n ? sorted.removeLast() : sorted.removeFirst());
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


			ModelVM toAdd = new ModelVM(qtp.getOneVM());
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

}
