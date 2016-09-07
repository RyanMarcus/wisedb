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
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import edu.brandeis.wisedb.cost.Cost;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.FirstFitDecreasingGraphSearch;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.PackUntilViolationGraphSearch;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;
import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class StrategyComparision {

	private static Set<ModelQuery> toTest;

	private static DTSearcher decisionTree;
	private static PackUntilViolationGraphSearch packUntil;
	private static FirstFitDecreasingGraphSearch ffd;
	
	
	private static ModelSLA sla = MaxLatencySLA.tenMinuteSLA();

	public static void main(String[] args) throws Exception {
		String dataFile = args[0];
		String bVal = args[1];
		//String multiplier = args[2];
		
		Logger.getLogger("edu.brandeis").setLevel(Level.SEVERE);
	
		PrintWriter results = new PrintWriter(new File("strat_comp.csv"));
		
		results.println("Queries, BF Size, Training Size, Pack Until, FFD, DT");
		
		QueryTimePredictor qtp = new QueryTimePredictor();
	
		int b = Integer.valueOf(bVal);
		Trainer.numThreads = 12;

		packUntil = new PackUntilViolationGraphSearch(qtp, sla);
		ffd = new FirstFitDecreasingGraphSearch(sla, qtp);
		
		for (int bfSize = 2; bfSize <= 11; bfSize++) {
			try (Trainer t = new Trainer(dataFile + "bf" + bfSize + ".csv", sla)) {


				for (int trainingSize = 10; trainingSize <= 600; trainingSize += 10) {
					long start = System.currentTimeMillis();
					b = Math.max(b, t.train(trainingSize, bfSize));
					start = System.currentTimeMillis() - start;
					System.out.println(bfSize + ": " + trainingSize + " / 600 (" + start + ")");

					
					int tsToCapture = trainingSize;
					int bfSizeToCapture = bfSize;
					decisionTree = new DTSearcher(t.getFilePath(), 
							new QueryTimePredictor(), 
							sla);
					IntStream.range(0, 10).parallel().forEach(i -> {
						try {	
							

							comparisonOnNQueries(500, tsToCapture, bfSizeToCapture, results);
						} catch (Exception e) {
							e.printStackTrace();
						}

						
						
					});

					results.flush();

				}


			}
		}
		
		results.flush();
		results.close();
		
		
		
	}


	private static void comparisonOnNQueries(int q, int tSize, int bfSize, PrintWriter out) throws Exception {

		toTest = ModelWorkloadGenerator.randomQueries(q);
		
		
		int pu = runTestFor("pack-until", packUntil);
		int ff = runTestFor("ffd", ffd);
		int dt = runTestFor("dt", decisionTree);

		synchronized (out) {
			out.println(q + ", " + bfSize + ", " + tSize + ", " + pu + ", " + ff + ", " + dt);
		}
		
	}
	
	private static int runTestFor(String descrip, GraphSearcher gs) {
		Set<ModelQuery> toSched = new HashSet<ModelQuery>(toTest);
		long time = System.currentTimeMillis();
		List<Action> solution = gs.schedule(toSched);
		time = System.currentTimeMillis() - time;
		
		if (solution == null)
			return -1;

		Cost cost = CostModelUtil.getDetailedCostForPlan(toSched, solution, sla);
		
		//System.out.println(descrip + ", " + toSched.size() + ", " + time + ", " + cost);
		
		//System.out.println(CostModelUtil.getFinalState(toSched, solution, Trainer.CURRENT_SLA));
		return cost.getTotalCost();

	}
	

}
