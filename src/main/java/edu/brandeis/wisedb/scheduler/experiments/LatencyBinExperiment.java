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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.PercentSLA;
import edu.brandeis.wisedb.scheduler.AStarGraphSearch;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;
import edu.brandeis.wisedb.scheduler.training.QueryGenerator;
import edu.brandeis.wisedb.scheduler.training.UnassignedQueryTimeHeuristic;
import info.rmarcus.javautil.StreamUtilities;

public class LatencyBinExperiment {

	public static void main(String[] args) throws Exception {
		List<Integer> costs = new ArrayList<Integer>();
		List<Integer> spread = new ArrayList<Integer>();
		
		for (int i = 0; i < 30000; i += 500) {
			spread.add(i);
			
			System.out.println(i);
			int[] val = new int[1];
			for (int k = 0; k < val.length; k++)
				val[k] = getCostForSpread(i);
			
			Arrays.sort(val);
			
			// take the median. in reality, we would use
			// enough training samples to always get the right tree,
			// but for this experiment to go quickly we will sample from
			// the tree space
			costs.add(val[0]);
		}
		
		StreamUtilities.zip(costs.stream(), spread.stream()).forEach(p -> {
			System.out.println(p.getB() + "\t" + p.getA());
		});
	}
	
	private static int getCostForSpread(int spread) throws Exception {
		int min = 120000;
		int max = 300000;
		int types = 5;
		
		Set<ModelQuery> queries = ModelWorkloadGenerator.randomQueries(min, max, types, spread, 5000, 42);
		
		
//		printHistogram(queries.stream()
//				.map(q -> ((SetLatencyModelQuery)q).getLatency())
//				.collect(Collectors.toList()),
//				0, 120, 500);
		
		queries = ModelWorkloadGenerator.randomQueries(min, max, types, spread, 15);
		
		return getTreeCost(queries, new QueryGenerator() {

			@Override
			public Set<ModelQuery> generateQueries(int queries) {
				return ModelWorkloadGenerator.randomQueries(min, max, types, spread, queries);
			}
			
		});
	}
	
	private static int getTreeCost(Set<ModelQuery> queries, QueryGenerator qg) throws Exception {
		ModelSLA sla = new PercentSLA(0.8f, 400000);
		
//		File f = new File("/Users/ryan/spread.csv");
//		if (f.exists())
//			f.delete();
//		
//		Trainer t = new Trainer("/Users/ryan/spread.csv", sla, qg);
//		t.train(2000, 10);
//		t.close();
		
		QueryTimePredictor qtp = new QueryTimePredictor();
		//DTSearcher dt = new DTSearcher("/Users/ryan/spread.csv", qtp, sla);
		//FirstFitDecreasingGraphSearch ffd = new FirstFitDecreasingGraphSearch(sla, qtp);
		AStarGraphSearch astar = new AStarGraphSearch(new UnassignedQueryTimeHeuristic(qtp), sla, qtp);
		
		List<Action> dtActions = astar.schedule(queries);
		
//		for (ModelQuery q : queries) {
//			System.out.println( ((SetLatencyModelQuery) q).getLatency() / 1000 );
//		}
		
//		System.out.println(CostModelUtil.getFinalState(queries, dtActions, sla));
//		for (ModelVM vm : CostModelUtil.getFinalState(queries, dtActions, sla).getVMs()) {
//			System.out.println(vm.getQueryLatencies(qtp));
//			
//		}
		
		
		
		return CostModelUtil.getCostForPlan(dtActions, sla);
		
	}
	
//	private static void printHistogram(Collection<Integer> data, int min, int bins, int binSize) {
//		for (int b = 0; b < bins; b++) {
//			final int bin = b;
//			long count = data.stream()
//					.filter(i -> i <= bin * (binSize + 1))
//					.filter(i -> i > bin * binSize)
//					.count();
//			System.out.println((b * binSize) + "\t" + count);
//		}
//	}

}
