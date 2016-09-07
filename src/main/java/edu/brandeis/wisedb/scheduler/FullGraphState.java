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
import java.util.Comparator;
import java.util.HashSet;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class FullGraphState extends State {
	private SortedSet<ModelVM> vms;
	private Set<ModelQuery> unassigned;
	private PriorityQueue<ModelQuery> unassignedSorted;
	private ModelSLA sla;
	private QueryTimePredictor qtp;
	
	private static final Logger log = Logger.getLogger(FullGraphState.class.getName());
	public static final boolean SORTED_QUERIES = false;
	public static final boolean NO_PENALTY = false;

	
	public FullGraphState(SortedSet<ModelVM> vms, Set<ModelQuery> queries, ModelSLA sla, QueryTimePredictor qtp) {
		super(sla, qtp);
		this.vms = vms;
		this.unassigned = queries;
		this.sla = sla;
		this.qtp = qtp;
	}

	
	public FullGraphState(Set<ModelQuery> toSched, ModelSLA sla, QueryTimePredictor qtp) {
		this(new TreeSet<ModelVM>(), toSched, sla, qtp);
	}
	
	public FullGraphState(Collection<ModelVM> vms, ModelSLA sla, QueryTimePredictor qtp) {
		this(new TreeSet<ModelVM>(vms), new HashSet<ModelQuery>(), sla, qtp);
	}

	public Set<ModelQuery> getUnassignedQueries() {
		return unassigned;
	}
	
		
	@SuppressWarnings("unused")
	public SortedSet<Action> getPossibleActions() {
		SortedSet<Action> toR = new TreeSet<Action>();
		
		// if there are no more queries to assign, we're done.
		if (unassigned.isEmpty())
			return toR;
		
		// we can start a new VM only if we have created fewer VMs
		// then the total queries we need to process
		
		//VMType[] applicableTypes = new VMType[] { VMType.M3_LARGE, VMType.T2_MEDIUM };
		VMType[] applicableTypes = new VMType[] { VMType.T2_MEDIUM };

		//VMType[] applicableTypes = VMType.values();
		toR.addAll(this.getCreationActions(applicableTypes));

			
		
		// we can assign a query to any of the currently running VMs
		if (unassigned.size() != 0) {
			//ModelQuery q = unassigned.iterator().next();
			for (ModelVM vm : vms) {
				if(SORTED_QUERIES){
					// get a sorted by latency priority queue from the unassigned query set
					unassignedSorted = new PriorityQueue<ModelQuery>(new QueryLatencyComparator(vm));
					unassignedSorted.addAll(unassigned);
					ModelQuery q = unassignedSorted.peek();
					AssignQueryAction potential = new AssignQueryAction(q, vm);
					potential.stateAppliedTo = this;
					int penalty;
					synchronized (this) {
						vm.addQuery(q);
						penalty = this.getSLAPenalty();
						vm.removeQuery(q);
					}
					if (NO_PENALTY && penalty != 0)
						continue;
					toR.add(potential);
				} else {
					for (ModelQuery q : unassigned) {
						AssignQueryAction potential = new AssignQueryAction(q, vm);
						potential.stateAppliedTo = this;
						int penalty;
						synchronized (this) {
							vm.addQuery(q);
							penalty = this.getSLAPenalty();
							vm.removeQuery(q);
						}
						if (NO_PENALTY && penalty != 0)
							continue;
						toR.add(potential);
					}
				}
			}
		}
		
		return toR;
	}
	
	private int totalQueries() {
		int sum = 0;
		for (ModelVM vm : vms) {
			sum += vm.getNumberOfQueries();
		}
		
		sum += unassigned.size();
		return sum;
	}
	
	public boolean isGoalState() {
		return unassigned.isEmpty();
	}
	
	public int getCostOfAction(Action a) {
		FullGraphState next = getNewStateForAction(a);
		return next.getExecutionCost() - getExecutionCost();
	}
	
	public int getExecutionCost() {
		return this.getDetailedExecutionCost().getTotalCost();
	}
		
	
	
	private Set<Action> getCreationActions(VMType[] applicableTypes) {
		HashSet<Action> toR = new HashSet<Action>();
		
		// limit the total number of VMs to to the total number of queries
		if (vms.size() < totalQueries()) {
			// no new VMs if we have an empty VM
			for (ModelVM vm : vms) {
				if (vm.getNumberOfQueries() == 0)
					return toR;
			}
			
			for (VMType t : applicableTypes) {
				StartNewVMAction toAdd = new StartNewVMAction(new ModelVM(t));
				toAdd.stateAppliedTo = this;
				toR.add(toAdd);
			}
		}
		
		return toR;
	}
	
	public FullGraphState getNewStateForAction(Action a) {
		if (a instanceof StartNewVMAction) {
			FullGraphState toR = clone();
			toR.vms.add(((StartNewVMAction) a).getVM());
			return toR;
		}
		
		if (a instanceof AssignQueryAction) {
			synchronized (this) {
				AssignQueryAction aq = (AssignQueryAction) a;
				
				addQuery(aq.getVM(), aq.getQuery());
				if (!unassigned.remove(aq.getQuery()))
					log.warning("Could not remove query -- bad assignment action?");
				FullGraphState toR = clone();
				unassigned.add(aq.getQuery());				
				removeQuery(aq.getVM(), aq.getQuery());
				return toR;
			}

		}
		
		
		return null;
	}
	
	private void addQuery(ModelVM vm, ModelQuery q) {
		for (ModelVM v : vms) {
			if (vm.equals(v)) {
				v.addQuery(q);
				return;
			}
		}
		
		log.warning("Failed to add query because the VM could not be found");
	}
	
	private void removeQuery(ModelVM vm, ModelQuery q) {
		for (ModelVM v : vms) {
			if (vm.equals(v)) {
				if (!v.removeQuery(q))
					log.warning("Failed to remove query from correct VM!");
				return;
			}
		}
		
		log.warning("Failed to remove query because the VM could not be found");

	}
	
	public FullGraphState clone() {		
		SortedSet<ModelVM> newVMs = new TreeSet<ModelVM>();
		for (ModelVM vm : vms) {
			newVMs.add(vm.clone());
		}
		
		Set<ModelQuery> newQs = new HashSet<ModelQuery>();
		for (ModelQuery mq : unassigned) {
			newQs.add(mq.clone());
		}
		
		return new FullGraphState(newVMs, newQs, sla, qtp);
		
	}
	
	
	@Override
	public int hashCode() {
		// should only be based on the VMs and the queries
		int toR = 0;
		for (ModelVM vm : vms) {
			toR += vm.getType().hashCode();
		}
		
		for (ModelQuery mq : unassigned) {
			toR += mq.getType();
		}
		
		return toR;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FullGraphState))
			return false;
		
		FullGraphState s = (FullGraphState) o;
		return vms.equals(s.vms) && unassigned.equals(s.unassigned);
	}
	
	
	
	public SortedMap<String, String> getFeatures() {
		NavigableMap<String, String> features = new TreeMap<String, String>();
		
		features.put("remaining-queries", String.valueOf(unassigned.size()));
		
		
		
		int largestLatencyDiff = 0;
		if (this.vms.size() != 0) {
			for (ModelVM vm1 : vms) {
				for (ModelVM vm2 : vms) {
					if (vm1.equals(vm2))
						continue;
					
					int currDiff = vm1.getRemainingRunningTime(qtp);
					largestLatencyDiff = Math.max(largestLatencyDiff, currDiff);
				}
			}
		}
		
		features.put("latency-diffs", String.valueOf(largestLatencyDiff));
		
		//features.put("query-to-vm-ratio", (double)totalQueries() / (double)vms.size());
		
		
		
		/*double avgTime = vms.stream().mapToInt((ModelVM vm) -> vm.getRemainingRunningTime(qtp)).average().getAsDouble();
		features.put("average-latency", avgTime);*/
		
		return features;
	}
	
	private class QueryLatencyComparator implements Comparator<ModelQuery> {
		private QueryTimePredictor qtp = new QueryTimePredictor();
		private ModelVM vm;
		
		public QueryLatencyComparator(ModelVM vm){
			this.vm = vm;
		}
		
		@Override
		public int compare(ModelQuery q1, ModelQuery q2) {
			return qtp.predict(q1, vm) - qtp.predict(q2, vm);
		}
		
	}





	public SortedSet<ModelVM> getVMs() {
		return vms;
	}


	@Override
	public void noteBValue(int b) {
		// TODO Auto-generated method stub
		
	}

}
