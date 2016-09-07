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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class EachTypeGraphSearch implements GraphSearcher {

	private QueryTimePredictor qtp;

	public EachTypeGraphSearch(QueryTimePredictor qtp) {
		this.qtp = qtp;
	}

	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		Set<ModelQuery> unassigned = new HashSet<ModelQuery>(toSched);
		Set<Integer> queryTypes = new HashSet<>();
		Deque<ModelVM> vms = new LinkedList<ModelVM>();


		toSched.stream().map(q -> q.getType()).forEach(queryTypes::add);

		while (!unassigned.isEmpty()) {
			vms.add(new ModelVM(qtp.getOneVM()));

			for (Integer type : queryTypes) {
				ModelQuery toAdd = null;
				for (ModelQuery q : unassigned) {
					if (q.getType() == type) {
						toAdd = q;
						break;
					}
				}

				if (toAdd == null)
					continue;

				unassigned.remove(toAdd);
				vms.getLast().addQuery(toAdd);
			}

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
