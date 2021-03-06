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

import edu.brandeis.wisedb.WiSeDBUtils;
import edu.brandeis.wisedb.WorkloadSpecification;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;

public class SchedulerUtils {

	private static final Logger log = Logger.getLogger(AStarGraphSearch.class.getName());


	public static DTSearcher getDTModel(InputStream trainingData, WorkloadSpecification wf) {
		try {
			return new DTSearcher(trainingData, wf.getQueryTimePredictor(), wf.getSLA());
		} catch (Exception e) {
			return null;
		}
	}

	public static List<Action> schedule(InputStream trainingData, WorkloadSpecification wf, Set<ModelQuery> toSchedule) {
		return schedule(getDTModel(trainingData, wf), wf, toSchedule);
	}

	public static List<Action> schedule(DTSearcher dt, WorkloadSpecification wf, Set<ModelQuery> toSchedule) {
		Set<GraphSearcher> algos = new HashSet<GraphSearcher>();

		final QueryTimePredictor qtp = wf.getQueryTimePredictor();

		algos.add(dt);
		algos.add(new FirstFitDecreasingGraphSearch(wf.getSLA(), qtp, false));
		algos.add(new FirstFitDecreasingGraphSearch(wf.getSLA(), qtp, true));
		algos.add(new PackNGraphSearch(9, qtp, wf.getSLA()));
		algos.add(new EachTypeGraphSearch(qtp));
		if (wf.getSLA() instanceof MaxLatencySLA && WiSeDBUtils.GLPSOL_PATH != null && WiSeDBUtils.GLPSOL_ENABLED) {
			algos.add(new MLPGraphSearcher((MaxLatencySLA) wf.getSLA(), qtp, WiSeDBUtils.GLPSOL_PATH));
		}


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

		List<Action> bestActions = min.get();
		List<Action> ffdActions = (new FirstFitDecreasingGraphSearch(wf.getSLA(), qtp, false))
				.schedule(toSchedule);
		
		int bestCost = CostModelUtil.getCostForPlan(bestActions, wf.getSLA(), qtp);
		int ffdCost = CostModelUtil.getCostForPlan(ffdActions, wf.getSLA(), qtp);
		
		if (ffdCost == bestCost)
			return ffdActions;
		
		return bestActions;
		
	}
}
