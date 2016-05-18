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
import edu.brandeis.wisedb.scheduler.AStarGraphSearch;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.training.AverageLatencyModelSLA;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;
import edu.brandeis.wisedb.scheduler.training.UnassignedQueryTimeHeuristic;
import info.rmarcus.javautil.StreamUtilities;

public class BurnRateExperiment {

	public static void main(String[] args) {
		

//		QueryTimePredictor qtp = new QueryTimePredictor();
//		for (int i : QueryTimePredictor.QUERY_TYPES) {
//			System.out.println(i + " // " + (qtp.predict(new ModelQuery(i), VMType.T2_SMALL) / 1000 / 60));
//		}

		
		calculateBurn(20000);


	}

	public static void calculateBurn(int samples) {
		//TightenableSLA sla = PercentSLA.nintyTenSLA();
		//TightenableSLA sla = new SimpleLatencyModelSLA(9 * 60 * 1000, 1);
		//TightenableSLA sla = PerQuerySLA.getLatencyTimesN(2.0);
		TightenableSLA sla = new AverageLatencyModelSLA(9 * 60 * 1000, 1);
		QueryTimePredictor qtp = new QueryTimePredictor();



		List<List<Action>> solutions = IntStream.range(0, samples)
				.parallel()
				.mapToObj(i -> {
					GraphSearcher opt = new AStarGraphSearch(new UnassignedQueryTimeHeuristic(qtp), sla, qtp);
					Set<ModelQuery> q = ModelWorkloadGenerator.randomQueries(6);
					List<Action> a = opt.schedule(q);
					return a;
				}).collect(Collectors.toList());

		System.out.println("Shift\tBurn");

		String res = IntStream.range(1, 240)
		.map(i -> i * 500)
		.mapToObj(i -> {
			TightenableSLA tighter = sla.tighten(i);
			// count up the number of training points burned
			return new StreamUtilities.Pair<Integer, Long>(i,
					StreamUtilities.zip(
							solutions.stream()
							.map(a -> CostModelUtil.getCostForPlan(a, sla)),
							solutions.stream()
							.map(a -> CostModelUtil.getCostForPlan(a, tighter))
							)
					.filter(p -> p.getA() != p.getB())
					.count());
		}).map(p -> p.mutateA(i -> i / (double)1000))
		.map(p -> p.mutateB(i -> (i / (double) samples)))
		.map(p -> p.getA() + "\t" + p.getB())
		.collect(Collectors.joining("\n"));
		

		System.out.println(res);




	}



}
