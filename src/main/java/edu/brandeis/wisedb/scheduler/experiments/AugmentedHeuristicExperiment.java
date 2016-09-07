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

import java.util.stream.IntStream;

import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.scheduler.AStarGraphSearch;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.Heuristic;
import edu.brandeis.wisedb.scheduler.training.UnassignedQueryTimeHeuristic;

public class AugmentedHeuristicExperiment {

	public static void main(String[] args) {
		ModelSLA sla = MaxLatencySLA.tenMinuteSLA();
		
		System.out.println("Size\tTime");
		IntStream.range(1, 21)
		.forEach(i -> measureTime(i, sla));
	}


	public static void measureTime(int size, ModelSLA sla) {
		QueryTimePredictor qtp = new QueryTimePredictor();
		Heuristic h = new UnassignedQueryTimeHeuristic(qtp);
		GraphSearcher gs = new AStarGraphSearch(h, sla, qtp);
		
		double time = IntStream.range(0, 5)
		.mapToLong(i -> {
			long t = System.currentTimeMillis();
			gs.getCostForRandom(qtp, i*100, size, sla);
			return System.currentTimeMillis() - t;
		}).average().getAsDouble();
		
		System.out.println(size + "\t" + time);
	}
}
