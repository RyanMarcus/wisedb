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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class PackUntilViolationRandomGraphSearch implements GraphSearcher {

	
	private ModelSLA sla;
	private QueryTimePredictor qtp;
	
	private static final Logger log = Logger.getLogger(PackUntilViolationRandomGraphSearch.class.getName());

	
	public PackUntilViolationRandomGraphSearch(QueryTimePredictor qtp, ModelSLA sla) {
		this.sla = sla;
		this.qtp = qtp;
	}
	
	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		PriorityQueue<ModelQuery> toAssign = new PriorityQueue<ModelQuery>(new QueryComparator());
		toAssign.addAll(toSched);
		Deque<ModelVM> vms = new LinkedList<ModelVM>();
		
		return packUntil(vms, toAssign);
	}

	
	protected List<Action> packUntil(Set<ModelVM> vms, Set<ModelQuery> toSched) {
		PriorityQueue<ModelQuery> toAssign = new PriorityQueue<ModelQuery>(new QueryComparator());
		Deque<ModelVM> tvms = new LinkedList<ModelVM>(vms);

		return packUntil(tvms, toAssign);
	}
	
	protected List<Action> packUntil(Deque<ModelVM> vms, PriorityQueue<ModelQuery> toAssign) {
		if (vms.isEmpty()) {
			// add our first VM
			vms.add(new ModelVM(VMType.T2_MEDIUM));
		}
		
		Iterator<ModelQuery> it = toAssign.iterator();
		while (it.hasNext()) {
			ModelQuery next = it.next();
			
			// pick a VM randomly. If it has no penalty choose this VM, otherwise start a new one.
			ArrayList<ModelVM> vmsList = new ArrayList<ModelVM>(vms);
			ModelVM vm = vmsList.get(new Random().nextInt(vmsList.size()));
			
			vm.addQuery(next);
			int assignToRandomPenalty = sla.calculatePenalty(vm.getQueryLatencies(qtp));
			vm.removeQuery(next);
			
			
			if (assignToRandomPenalty == 0) {
				vm.addQuery(next);
			} else {
				log.fine("Got violation. Adding new VM");
				vms.add(new ModelVM(VMType.T2_MEDIUM));
				vms.getLast().addQuery(next);
			}
			
		}
		
		// next, reconstruct a series of actions to get us into this state
		List<Action> toR = new LinkedList<Action>();
		for (ModelVM vm : vms) {
			toR.add(new StartNewVMAction(vm));
			
			for (ModelQuery q : vm.getQueries()) {
				toR.add(new AssignQueryAction(q, vm));
			}
			
			vm.removeAllQueries();
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
