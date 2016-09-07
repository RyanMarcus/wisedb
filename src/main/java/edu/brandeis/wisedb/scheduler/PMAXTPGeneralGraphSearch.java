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
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Logger;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class PMAXTPGeneralGraphSearch implements GraphSearcher {

	
	private ModelSLA sla;
	private QueryTimePredictor qtp;
	
	private static final Logger log = Logger.getLogger(PMAXTPGeneralGraphSearch.class.getName());

	
	public PMAXTPGeneralGraphSearch(QueryTimePredictor qtp, ModelSLA sla) {
		this.sla = sla;
		this.qtp = qtp;
	}
	
	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		Set<ModelQuery> toAssign = new HashSet<ModelQuery>(toSched);
		Deque<ModelVM> vms = new LinkedList<ModelVM>();
		
		return TPGeneral(vms, toAssign);
	}

	protected List<Action> TPGeneral(Set<ModelVM> vms, Set<ModelQuery> toAssign) {
		return TPGeneral(new LinkedList<ModelVM>(vms), toAssign);
	}
	
	
	protected List<Action> TPGeneral(Deque<ModelVM> vms, Set<ModelQuery> qs) {
		PriorityQueue<ModelQuery> toAssign = new PriorityQueue<ModelQuery>(new QueryComparator());
		toAssign.addAll(qs);
		
		
		if (vms.isEmpty()) {
			// add our first VM
			vms.add(new ModelVM(VMType.T2_MEDIUM));
		}
		
		while (toAssign.size() != 0) {
			ModelQuery next = toAssign.poll();
			
			
			vms.getLast().addQuery(next);
			int assignToLastCost = (new FullGraphState(vms, sla, qtp)).getExecutionCost();
			
			vms.getLast().removeQuery(next);
			vms.add(new ModelVM(VMType.T2_MEDIUM));
			vms.getLast().addQuery(next);
			int assignToNewCost = (new FullGraphState(vms, sla, qtp)).getExecutionCost();

			vms.removeLast();
			
			if (assignToLastCost <= assignToNewCost && !seqSplit(vms)) {
				vms.getLast().addQuery(next);
			} else {
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
	
	private boolean seqSplit(Deque<ModelVM> currentVMs) {
		int currentCost = (new FullGraphState(currentVMs, sla, qtp)).getExecutionCost();
		
		int numOnLast = currentVMs.getLast().getNumberOfQueries();
		for (int i = 1; i < numOnLast; i++) {
			log.fine("Trying split " + i + " out of " + (numOnLast - 1));
			
			// try a sequential split at i
			List<ModelQuery> toMove = currentVMs.getLast().removeLastNQueries(numOnLast - i);
			currentVMs.add(new ModelVM(VMType.T2_MEDIUM));
			currentVMs.getLast().addQueries(toMove);
			
			int costOfSplit = (new FullGraphState(currentVMs, sla, qtp)).getExecutionCost();
			
			// reset 
			ModelVM failedNew = currentVMs.removeLast();
			currentVMs.getLast().addQueries(failedNew.getQueries());
			
			
			if (costOfSplit < currentCost) {
				// accept this split
				log.fine("Accepting split at " + i + " (old: " + currentCost + ", new: " + costOfSplit + ")");
				return true;
			}
			
			
		}
		
		return false;
	}

	private class QueryComparator implements Comparator<ModelQuery> {

		@Override
		public int compare(ModelQuery o1, ModelQuery o2) {
			ModelVM t = new ModelVM(VMType.T2_MEDIUM);
			return qtp.predict(o2, t) - qtp.predict(o1, t);
		}
		
	}

	
}
