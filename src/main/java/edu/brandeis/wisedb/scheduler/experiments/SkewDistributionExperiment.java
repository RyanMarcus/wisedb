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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;

import edu.brandeis.wisedb.cost.Cost;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.TightenableSLA;
import edu.brandeis.wisedb.cost.sla.PercentSLA;
import edu.brandeis.wisedb.scheduler.AStarGraphSearch;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;
import edu.brandeis.wisedb.scheduler.training.UnassignedQueryTimeHeuristic;
import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class SkewDistributionExperiment {

	public static void main(String[] args) throws Exception {
	

//		QueryTimePredictor qtp = new QueryTimePredictor();
//		for (int i : QueryTimePredictor.QUERY_TYPES) {
//			System.out.println(i + " // " + (qtp.predict(new ModelQuery(i), VMType.T2_SMALL) / 1000 / 60));
//		}


		calculateBurn(1000);


	}

	public static void calculateBurn(int samples) throws Exception {
		TightenableSLA sla = PercentSLA.nintyTenSLA();
		//TightenableSLA sla = new SimpleLatencyModelSLA(9 * 60 * 1000, 1);
		//TightenableSLA sla = PerQuerySLA.getLatencyTimesN(2.0);
		//TightenableSLA sla = new AverageLatencyModelSLA(7 * 60 * 1000, 1);
		QueryTimePredictor qtp = new QueryTimePredictor();


		File f = new File("distSkew.csv");
		if (f.exists())
			f.delete();
		
		try (Trainer t = new Trainer("distSkew.csv", sla)) {
			t.train(2000, 12);
		}
		
		
		DTSearcher dt = new DTSearcher("distSkew.csv", qtp, sla);
		AStarGraphSearch astar = new AStarGraphSearch(new UnassignedQueryTimeHeuristic(qtp), sla, qtp);
		//FirstFitDecreasingGraphSearch astar = new FirstFitDecreasingGraphSearch(sla, qtp);
		
		ChiSquareTest cst = new ChiSquareTest();
		ChiSquaredDistribution cqd = new ChiSquaredDistribution(qtp.QUERY_TYPES.length - 1);
		double[] expceted = Arrays.stream(qtp.QUERY_TYPES)
				.mapToDouble(i -> 20.0/(qtp.QUERY_TYPES.length))
				.toArray();
		
		System.out.println("Chi\tDT\tOpt");

		for (int i = 0; i < samples; i++) {
			Set<ModelQuery> smp = ModelWorkloadGenerator.randomQueries(20);
			
			// reject samples that don't have at least one of each query type
			long repr = smp.stream().mapToInt(q -> q.getType()).distinct().count();
			if (repr != qtp.QUERY_TYPES.length) {
				i--;
				continue;
			}
			
			Map<Integer, List<ModelQuery>> groups = smp.stream()
					.collect(Collectors.groupingBy(q -> q.getType()));
			
			long obs[] = Arrays.stream(qtp.QUERY_TYPES)
					.mapToLong(v -> groups.get(v).size())
					.toArray();
			
			double chi = cst.chiSquare(expceted, obs);
			chi = cqd.cumulativeProbability(chi);
			
			Cost dtCost = dt.getCostForQueries(smp, sla);
			Cost optCost = astar.getCostForQueries(smp, sla);
			
			System.out.println(chi + "\t" + dtCost.getTotalCost() + "\t" + optCost.getTotalCost());
		}



	}

}
