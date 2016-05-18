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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.google.common.collect.MinMaxPriorityQueue;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class BestNFirstGraphSearch implements GraphSearcher {
	
	private Heuristic h;
	private QueryTimePredictor qtp;
	private ModelSLA sla;
	private int toTry;
	
	private static final Logger log = Logger.getLogger(BestNFirstGraphSearch.class.getName());

	
	private int maxFrontierSize;
	
	public BestNFirstGraphSearch(Heuristic h, QueryTimePredictor qtp, ModelSLA sla, int toTry) {
		this.h = h;
		this.qtp = qtp;
		this.sla = sla;
		this.toTry = toTry;
		this.maxFrontierSize = toTry * toTry;
	}

	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		
		FullGraphState first = new FullGraphState(new TreeSet<ModelVM>(), toSched, sla, qtp);
		MinMaxPriorityQueue<StateCost> frontier = MinMaxPriorityQueue.create();
		frontier.add(new StateCost(first, 0, null, null));
		
		while (!frontier.isEmpty()) {
			log.fine("Frontier size: " + frontier.size());
			
			PriorityQueue<Action> pq = new PriorityQueue<Action>(new ActionComparator());
			StateCost next = frontier.poll();
			
			if (next.s.isGoalState()) {
				// we're done
				List<Action> toR = new LinkedList<Action>();
				StateCost last = next;
				while (last.action != null) {
					toR.add(0, last.action);
					last = last.prev;
				}
				log.fine("Reached goal state with following actions: " + toR);

				return toR;
			}
			
			for (Action a : next.s.getPossibleActions()) {
				int cost = 0;
				FullGraphState nextState = next.s.getNewStateForAction(a);
				
				cost += h.predictCostToEnd(nextState);
				//cost += nextState.getExecutionCost();
				
				a.computedCost = cost;
				log.finer("Added action " + a + " to the frontier");
				pq.add(a);	
			}
			
			if (pq.isEmpty()) {
				log.severe("There was no selectable action for state: " + next);
				return null;
			}
			
			for (int i = 0; i < toTry; i++) {
				Action nextBest = pq.poll();
				if (nextBest == null) {
					log.fine("Unable to get " + (i+1) + "th action for state " + next);
					break;
				}
				FullGraphState c = next.s.getNewStateForAction(nextBest);
				StateCost candidate = new StateCost(c, c.getExecutionCost(), nextBest, next);
				frontier.add(candidate);
			}
			
			while (frontier.size() > maxFrontierSize) {
				frontier.removeLast();
			}
		}
		
		return null;
	}
	
	private class ActionComparator implements Comparator<Action> {

		@Override
		public int compare(Action o1, Action o2) {
			return o1.computedCost - o2.computedCost;
		}
		
	}
	
	private class StateCost implements Comparable<StateCost> {
		public FullGraphState s;
		public int cost;
		public Action action;
		public StateCost prev;
		
		public StateCost(FullGraphState state, int cost, Action a, StateCost prev) {
			s = state;
			this.cost = cost;
			action = a;
			this.prev = prev;
		}

		@Override
		public int compareTo(StateCost sc) {
			return  cost - sc.cost;
		}
		
		@Override
		public String toString() {
			return "(" + s.toString() + ", " + cost + ")";
		}
	}
}
