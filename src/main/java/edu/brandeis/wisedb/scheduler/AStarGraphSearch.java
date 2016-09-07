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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.training.decisiontree.SingularMachineState;
import info.rmarcus.javautil.IndexedPairingHeap;

public class AStarGraphSearch implements GraphSearcher {
	private Heuristic h;

	private ModelSLA sla;
	private QueryTimePredictor qtp;
	private int visitedNodes;

	public AStarGraphSearch(Heuristic h, ModelSLA sla, QueryTimePredictor qtp) {
		this.h = h;
		this.sla = sla;
		this.qtp = qtp;
	}

	public synchronized List<Action> schedule(Set<ModelQuery> toSched) {
		return schedule(toSched, null);
	}

	public synchronized List<Action> schedule(Set<ModelQuery> toSched, List<Action> likelyPath) {
		visitedNodes = 0;
		SingularMachineState startingState = new SingularMachineState(toSched, qtp, sla);
		return search(startingState);
	}

	public int getVisitedNodeCount() {
		return visitedNodes;
	}





	List<Action> search(State startingState) {

		IndexedPairingHeap<StateCost> frontier = new IndexedPairingHeap<>();
		Set<State> explored = new HashSet<State>();
		

		StateCost initial = new StateCost(startingState, 0, null, null);
		frontier.add(initial);

		while (!frontier.isEmpty()) {
			StateCost currentMin = frontier.poll();
			explored.add(currentMin.s);


			if (currentMin.s.isGoalState()) {
				// done!
				return currentMin.s.toActions(qtp, sla);
			}

			// otherwise, we need to investigate each child.
			for (Action a : currentMin.s.getPossibleActions()) {
				State child = currentMin.s.getNewStateForAction(a);



				if (explored.contains(child)) {
					continue;
				}

				int cost = child.getDetailedExecutionCost().getTotalCost();
				cost += h.predictCostToEnd(child);

				StateCost toAdd = new StateCost(child, cost, a, currentMin);

				if (frontier.contains(toAdd) && frontier.getValue(toAdd).cost > toAdd.cost) {
					frontier.updateItem(toAdd);
				} else {
					frontier.add(toAdd);
				}

			}

		}


		return null;
	}

	private class StateCost implements Comparable<StateCost> {
		public State s;
		public int cost;
		
		public StateCost(State state, int i, Action a, StateCost prev) {
			s = state;
			cost = i;
		}

		@Override
		public int compareTo(StateCost sc) {
			return  cost - sc.cost;


		}

		@Override
		public String toString() {
			return "(" + s.toString() + ", " + cost + ")";
		}

		@Override
		public int hashCode() {
			return s.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof StateCost))
				return false;
			
			StateCost other = (StateCost) o;
			return other.s.equals(s);
		}


	}
}
