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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class BestFirstGraphSearch implements GraphSearcher {
	
	private Heuristic h;
	private QueryTimePredictor qtp;
	private ModelSLA sla;
	
	private static final Logger log = Logger.getLogger(BestFirstGraphSearch.class.getName());

	
	public BestFirstGraphSearch(Heuristic h, QueryTimePredictor qtp, ModelSLA sla) {
		this.h = h;
		this.qtp = qtp;
		this.sla = sla;
	}

	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		List<Action> toR = new LinkedList<Action>();
		
		FullGraphState s = new FullGraphState(new TreeSet<ModelVM>(), toSched, sla, qtp);
		
		while (!s.isGoalState()) {
			Action currentBest = null;
			int currentBestCost = Integer.MAX_VALUE;

			for (Action a : s.getPossibleActions()) {
				int cost = 0;
				FullGraphState nextState = s.getNewStateForAction(a);
				
				cost += h.predictCostToEnd(nextState);
				cost += nextState.getExecutionCost();
				
				if (cost <= currentBestCost) {
					log.finer("New best action: " + a);
					currentBestCost = cost;
					currentBest = a;
				}
			
			}
			
			if (currentBest == null) {
				log.severe("There was no selectable action for state: " + s);
				return null;
			}
			
			toR.add(currentBest);
			log.finer("Found best action: " + currentBest);
			log.fine("Actions selected: " + toR.size());
			s = s.getNewStateForAction(currentBest);
			
			if (s == null) {
				log.severe("Got null state during search after applying: " + currentBest);
				return null;
			}
		}
		
		return toR;
	}
}
