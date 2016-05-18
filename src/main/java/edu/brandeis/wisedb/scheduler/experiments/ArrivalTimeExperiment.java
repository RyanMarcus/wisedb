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

import java.util.Set;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.AverageLatencyModelSLA;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.cost.sla.PerQuerySLA;
import edu.brandeis.wisedb.cost.sla.PercentSLA;
import edu.brandeis.wisedb.scheduler.AStarGraphSearch;
import edu.brandeis.wisedb.scheduler.FirstFitDecreasingGraphSearch;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;
import edu.brandeis.wisedb.scheduler.training.UnassignedQueryTimeHeuristic;

public class ArrivalTimeExperiment {

	public static void main(String[] args) {
//		for (int i = 660000; i < 8000000; i += 100000) {
//			testWithDeadline(i);
//		}
		
//		for (double d = 1.5; d < 20.0; d += 0.5) {
//			testWithMult(d);
//		}

//		for (int i = 660000; i < 8000000; i += 100000) {
//			testWithAvg(i);
//		}

		for (int i = 660000; i < 8000000; i += 100000) {
			testWithPerc(i);
		}
		
	}

	public static void testWithDeadline(int deadline) {
		ModelSLA sla = new MaxLatencySLA(deadline, 1);
		QueryTimePredictor qtp = new QueryTimePredictor();

		FirstFitDecreasingGraphSearch ffd = new FirstFitDecreasingGraphSearch(sla, qtp);
		AStarGraphSearch astar = new AStarGraphSearch(new UnassignedQueryTimeHeuristic(qtp), sla, qtp);

		Set<ModelQuery> q = ModelWorkloadGenerator.randomQueries(21, 42);

		System.out.println(deadline + "\t" 
				+ ffd.getCostForQueries(q, sla).getTotalCost() 
				+ "\t" + astar.getCostForQueries(q, sla).getTotalCost());

	}
	
	public static void testWithMult(double deadline) {
		ModelSLA sla = PerQuerySLA.getLatencyTimesN(deadline);
		QueryTimePredictor qtp = new QueryTimePredictor();

		FirstFitDecreasingGraphSearch ffd = new FirstFitDecreasingGraphSearch(sla, qtp, true);
		AStarGraphSearch astar = new AStarGraphSearch(new UnassignedQueryTimeHeuristic(qtp), sla, qtp);

		Set<ModelQuery> q = ModelWorkloadGenerator.randomQueries(20, 42);

		System.out.println(deadline + "\t" 
				+ ffd.getCostForQueries(q, sla).getTotalCost() 
				+ "\t" + astar.getCostForQueries(q, sla).getTotalCost());

	}

	public static void testWithAvg(int deadline) {
		ModelSLA sla = new AverageLatencyModelSLA(deadline, 1);
		QueryTimePredictor qtp = new QueryTimePredictor();

		FirstFitDecreasingGraphSearch ffd = new FirstFitDecreasingGraphSearch(sla, qtp, true);
		AStarGraphSearch astar = new AStarGraphSearch(new UnassignedQueryTimeHeuristic(qtp), sla, qtp);

		Set<ModelQuery> q = ModelWorkloadGenerator.randomQueries(20, 42);

		System.out.println(deadline + "\t" 
				+ ffd.getCostForQueries(q, sla).getTotalCost() 
				+ "\t" + astar.getCostForQueries(q, sla).getTotalCost());

	}
	
	public static void testWithPerc(int deadline) {
		ModelSLA sla = PercentSLA.nintyTenSLA(deadline);
		QueryTimePredictor qtp = new QueryTimePredictor();

		FirstFitDecreasingGraphSearch ffd = new FirstFitDecreasingGraphSearch(sla, qtp, true);
		AStarGraphSearch astar = new AStarGraphSearch(new UnassignedQueryTimeHeuristic(qtp), sla, qtp);

		Set<ModelQuery> q = ModelWorkloadGenerator.randomQueries(20, 42);

		System.out.println(deadline + "\t" 
				+ ffd.getCostForQueries(q, sla).getTotalCost() 
				+ "\t" + astar.getCostForQueries(q, sla).getTotalCost());

	}
	
}
