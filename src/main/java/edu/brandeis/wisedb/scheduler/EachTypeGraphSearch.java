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
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.training.decisiontree.SingularMachineState;

public class EachTypeGraphSearch implements GraphSearcher {

	private QueryTimePredictor qtp;
	private ModelSLA sla;

	public EachTypeGraphSearch(QueryTimePredictor qtp, ModelSLA sla) {
		this.qtp = qtp;
		this.sla = sla;
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
		
		SingularMachineState sms = new SingularMachineState(vms, unassigned, qtp, sla);
		return sms.toActions(qtp, sla);


	}

}
