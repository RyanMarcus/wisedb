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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;

public class BudgetLimitedGraphSearch implements AnytimeGraphSearcher {

	
	private int budget;
	private QueryTimePredictor qtp;
	private ModelSLA sla;
	private Heuristic h;
	
	private static final Logger log = Logger.getLogger(BudgetLimitedGraphSearch.class.getName());

	private ActionComparator ac;
	
	
	public BudgetLimitedGraphSearch(int budget, QueryTimePredictor qtp, ModelSLA sla, Heuristic h) {
		this.budget = budget;
		this.qtp = qtp;
		this.sla = sla;
		this.h = h;
		this.ac = new ActionComparator();
		
	}
	
	private long start;
	private long time;
	private TimeUnit unit;
	
	@Override
	public List<Action> schedule(Set<ModelQuery> toSched, long time,
			TimeUnit unit) {
		
		start = System.currentTimeMillis();
		this.time = time;
		this.unit = unit;
		
		List<Action> currentBest = null;
		int bestCost = Integer.MAX_VALUE;
		
		while (!mustStop()) {
			List<Action> candidate = schedule(toSched);
			
			if (candidate != null) {
				currentBest = candidate;
				bestCost = CostModelUtil.getCostForPlan(toSched, currentBest, sla);
			} else {
				break;
			}
			
			log.info("Beat last budget, new best: " + bestCost);
			
			// try and do better
			budget = bestCost - 1;
		}
		
		if (mustStop()) {
			log.fine("Anytime search terminated due to timeout");
		}
		
		return currentBest;
	}
	
	private boolean mustStop() {
		long elapsed = System.currentTimeMillis() - start;
		return elapsed > unit.toMillis(time);
	}
		
	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		// do a DFS evaluation of the tree, ignoring edges that would put us over budget
		StatePrev start = new StatePrev(new FullGraphState(toSched, sla, qtp), null, null);
		StatePrev next = start;
		
		while (!next.s.isGoalState()) {
			Action[] possible =	next.s.getPossibleActions().toArray(new Action[] {});
			Arrays.sort(possible, ac);
			
			while (true) {
				if (mustStop())
					return null;
				
				if (next.nextRank >= possible.length) {
					// we need to backtrack
					log.finer("Backtracking from " + next.s);
					next = next.prev;
					
					if (next == null) // there is no path
						return null;
					
					log.fine("Candidates available after backtrack: " + next.s.getPossibleActions().size());

					
					
					next.nextRank++;
					break;
				}
				
				FullGraphState candidate = next.s.getNewStateForAction(possible[next.nextRank]);
				if (candidate.getExecutionCost() > budget) {
					log.finest("Skipping candidate " + candidate + " because it is over budget");
					next.nextRank++;
					continue;
				}
				
				log.finest("Accepting candidate " + candidate);
				next = new StatePrev(candidate, next, possible[next.nextRank]);
				break;
			}
		}
		
		List<Action> toR  = new LinkedList<Action>();
		
		while (next.prev != null) {
			toR.add(0, next.a);
			next = next.prev;
		}
		
		return toR;
	}

	
	private class StatePrev {
		public FullGraphState s;
		public Action a;
		public StatePrev prev;
		public int nextRank = 0;
		
		public StatePrev(FullGraphState s, StatePrev prev, Action a) {
			this.s = s;
			this.prev = prev;
			this.a = a;
		}
	}
	
	private class ActionComparator implements Comparator<Action> {
		

		@Override
		public int compare(Action o1, Action o2) {
			State s1 = o1.stateAppliedTo.getNewStateForAction(o1);
			State s2 = o2.stateAppliedTo.getNewStateForAction(o2);
			
			int cost1 = h.predictCostToEnd(s1);
			int cost2 = h.predictCostToEnd(s2);

			
			return cost1 - cost2;
		}
	}

	
	
}
