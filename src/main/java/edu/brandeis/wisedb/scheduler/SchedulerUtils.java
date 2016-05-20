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

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import edu.brandeis.wisedb.WorkloadSpecification;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;

public class SchedulerUtils {

	private static final Logger log = Logger.getLogger(AStarGraphSearch.class.getName());


	public static List<Action> schedule(InputStream trainingData, WorkloadSpecification wf, Set<ModelQuery> toSchedule) {
		Set<GraphSearcher> algos = new HashSet<GraphSearcher>();

		final QueryTimePredictor qtp = wf.getQueryTimePredictor();
		
		try {
			DTSearcher dt = new DTSearcher(trainingData, qtp, wf.getSLA());
			algos.add(dt);
		} catch (Exception e) {
			log.info("Could not construct decision tree searcher: " + e.toString());
		}
		
		algos.add(new FirstFitDecreasingGraphSearch(wf.getSLA(), qtp, false));
		algos.add(new FirstFitDecreasingGraphSearch(wf.getSLA(), qtp, true));
		algos.add(new EachTypeGraphSearch(qtp));
		
		
		Optional<List<Action>> min = algos.stream()
		.map(gs -> gs.schedule(toSchedule))
		.min((a, b) -> {
			int aCost = Math.abs(CostModelUtil.getCostForPlan(toSchedule, a, wf.getSLA(), qtp));
			int bCost = Math.abs(CostModelUtil.getCostForPlan(toSchedule, b, wf.getSLA(), qtp));
			
			return aCost - bCost;
		});
		
		if (!min.isPresent()) {
			log.severe("No searcher could schedule the workload!");
			return null;
		}
		
		return min.get();


	}
}
