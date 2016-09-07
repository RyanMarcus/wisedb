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
 
 

package edu.brandeis.wisedb.scheduler.experiments;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.TightenableSLA;
import edu.brandeis.wisedb.cost.sla.PercentSLA;
import edu.brandeis.wisedb.scheduler.AStarGraphSearch;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;
import edu.brandeis.wisedb.scheduler.training.UnassignedQueryTimeHeuristic;

public class IncrementalSLAExperiment {

	public static void main(String[] args) {
		
//		QueryTimePredictor qtp = new QueryTimePredictor();
//		for (int i : QueryTimePredictor.QUERY_TYPES) {
//			System.out.println(i + " // " + (qtp.predict(new ModelQuery(i), VMType.T2_SMALL) / 1000 / 60));
//		}


		System.out.println("Shift\tRetrain");
		calculateBurn(10000);


	}

	public static void calculateBurn(int samples) {
		TightenableSLA sla = PercentSLA.nintyTenSLA();
		//TightenableSLA sla = new SimpleLatencyModelSLA(9 * 60 * 1000, 1);
		//TightenableSLA sla = PerQuerySLA.getLatencyTimesN(2.0);
		//TightenableSLA sla = new AverageLatencyModelSLA(9 * 60 * 1000, 1);
		QueryTimePredictor qtp = new QueryTimePredictor();



		List<List<Action>> solutions = IntStream.range(0, samples)
				.parallel()
				.mapToObj(i -> {
					GraphSearcher opt = new AStarGraphSearch(new UnassignedQueryTimeHeuristic(qtp), sla, qtp);
					Set<ModelQuery> q = ModelWorkloadGenerator.randomQueries(6);
					List<Action> a = opt.schedule(q);
					return a;
				}).collect(Collectors.toList());

		for (int i = 0; i <= 60; i++) {
			int shift = i * 2000;
			TightenableSLA tighter = sla.tighten(shift);

			// filter out the burned samples
			List<List<Action>> unburned = solutions.stream()
					.filter(a -> CostModelUtil.getCostForPlan(a, tighter) == CostModelUtil.getCostForPlan(a, sla))
					.collect(Collectors.toList());

			int newNeeded = solutions.size() - unburned.size();
			System.out.println((shift / 1000) + "\t" + (newNeeded / (double) samples));

			List<List<Action>> newSamples = IntStream.range(0, newNeeded)
					.parallel()
					.mapToObj(c -> {
						GraphSearcher opt = new AStarGraphSearch(new UnassignedQueryTimeHeuristic(qtp), tighter, qtp);
						Set<ModelQuery> q = ModelWorkloadGenerator.randomQueries(6);
						List<Action> a = opt.schedule(q);
						return a;
					})
					.collect(Collectors.toList());
			
			unburned.addAll(newSamples);
			solutions = unburned;

		}




	}

}
