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
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

public class RandomWalkPrefixGraphSearch implements GraphSearcher {

	private static final Logger log = Logger.getLogger(RandomWalkPrefixGraphSearch.class.getName());

	private Heuristic h;
	private QueryTimePredictor qtp;
	private ModelSLA sla;
	
	private final int stepUntilQueriesLeft;
	private Random r;
	
	public RandomWalkPrefixGraphSearch(Heuristic h,  ModelSLA sla, QueryTimePredictor qtp, int queriesLeft) {
		this.h = h;
		this.qtp = qtp;
		this.sla = sla;
		this.stepUntilQueriesLeft = queriesLeft;
		this.r = new Random();
	}
	
	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		
		FullGraphState s = new FullGraphState(new TreeSet<ModelVM>(), toSched, sla, qtp);
		
		List<Action> toR = new LinkedList<Action>();
		
		while (s.getUnassignedQueries().size() != stepUntilQueriesLeft) {
			Action random = randomAction(s.getPossibleActions());
			s = s.getNewStateForAction(random);
			toR.add(random);
		}
		
		log.fine("Random walk finished. Starting A* search...");
		
		AStarGraphSearch astar = new AStarGraphSearch(h, sla, qtp);
		
		toR.addAll(astar.search(s));
		
		return toR;
		
		
	}
	
	private Action randomAction(Set<Action> from) {
		int sel = r.nextInt(from.size());
		for (Action a : from) {
			if (sel-- == 0)
				return a;
			
		}
		
		return null;
	}

}
