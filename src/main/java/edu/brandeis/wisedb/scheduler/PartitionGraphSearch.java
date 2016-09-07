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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class PartitionGraphSearch implements GraphSearcher {

	private QueryTimePredictor qtp;
	private ModelSLA sla;
	private QueryComparator qc = new QueryComparator();
	
	private static final Logger log = Logger.getLogger(PartitionGraphSearch.class.getName());

	
	public PartitionGraphSearch(QueryTimePredictor qtp, ModelSLA sla) {
		this.qtp = qtp;
		this.sla = sla;
	}
	
	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		ModelVM vm = new ModelVM(VMType.T2_MEDIUM);
		PriorityQueue<ModelQuery> toAssign = new PriorityQueue<ModelQuery>(qc);
		toAssign.addAll(toSched);
		
		BlockingQueue<PriorityQueue<ModelQuery>> tooBig = new ArrayBlockingQueue<PriorityQueue<ModelQuery>>(2000);
		Set<PriorityQueue<ModelQuery>> complete = new HashSet<PriorityQueue<ModelQuery>>();
		
		tooBig.add(toAssign);
		
		while (!tooBig.isEmpty()) {
			PriorityQueue<ModelQuery> next = tooBig.poll();
			
			int s1sum = 0;
			int s2sum = 0;
			PriorityQueue<ModelQuery> s1 = new PriorityQueue<ModelQuery>(qc);
			PriorityQueue<ModelQuery> s2 = new PriorityQueue<ModelQuery>(qc);

			while (!next.isEmpty()) {
				ModelQuery q = next.poll();
				int queryTime = qtp.predict(q, VMType.T2_MEDIUM);
				if (s1sum < s2sum) {
					s1.add(q);
					s1sum += queryTime;
				} else {
					s2.add(q);
					s2sum += queryTime;
				}
			}
			
			vm.addQueries(s1);
			int s1pen = sla.calculatePenalty(vm.getQueryLatencies(qtp));
			vm.removeAllQueries();
			vm.addQueries(s2);
			int s2pen = sla.calculatePenalty(vm.getQueryLatencies(qtp));
			
			if (s1pen != 0) {
				tooBig.add(s1);
			} else {
				log.fine("Set complete with no violations at size: " + s1.size());
				complete.add(s1);
			}
			
			if (s2pen != 0) {
				tooBig.add(s2);
			} else {
				log.fine("Set complete with no violations at size: " + s2.size());
				complete.add(s2);
			}
				
			
		}
		
		List<Action> toR = new LinkedList<Action>();
		
		// now, complete contains all the query sets we are going to give
		// to individual VMs
		for (PriorityQueue<ModelQuery> forVM : complete) {
			ModelVM newVM = new ModelVM(VMType.T2_MEDIUM);
			StartNewVMAction start = new StartNewVMAction(newVM);
			toR.add(start);
			
			while (!forVM.isEmpty()) {
				toR.add(new AssignQueryAction(forVM.poll(), newVM));
			}
			
		}
		
		return toR;
		
		
	}

	
	private class QueryComparator implements Comparator<ModelQuery> {

		@Override
		public int compare(ModelQuery o1, ModelQuery o2) {
			ModelVM t = new ModelVM(VMType.T2_MEDIUM);
			return qtp.predict(o2, t) - qtp.predict(o1, t);
		}
		
	}
}
