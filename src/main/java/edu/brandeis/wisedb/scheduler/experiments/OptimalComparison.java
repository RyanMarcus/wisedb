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
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.AverageLatencyModelSLA;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.cost.sla.PerQuerySLA;
import edu.brandeis.wisedb.cost.sla.PercentSLA;
import edu.brandeis.wisedb.scheduler.AStarGraphSearch;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.FirstFitDecreasingGraphSearch;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;

public class OptimalComparison {


	public static void main(String[] args) throws Exception {

		
		
		ModelSLA sla1 = PerQuerySLA.getLatencyTimesN(3.0);
		ModelSLA sla2 = AverageLatencyModelSLA.tenMinuteSLA();
		ModelSLA sla3 = MaxLatencySLA.fifteenMinuteSLA();
		ModelSLA sla4 = PercentSLA.nintyTenSLA(15 * 60 * 1000);


		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.ALL);
		Logger.getLogger(AStarGraphSearch.class.getName()).setLevel(Level.INFO);
		Logger.getLogger(AStarGraphSearch.class.getName()).addHandler(ch);
		
		doCompare(sla1, 20, args[0]);
		doCompare(sla2, 20, args[0]);
		doCompare(sla3, 20, args[0]);
		doCompare(sla4, 18, args[0]);

	}

	
	public static void doCompare(ModelSLA sla, int solveSize, String file) throws Exception {
		QueryTimePredictor qtp = new QueryTimePredictor();
		Set<ModelQuery> toSolve = ModelWorkloadGenerator.randomQueries(5000, 42);

//		toSolve = new HashSet<ModelQuery>();
//		toSolve.add(new ModelQuery(100));
//		toSolve.add(new ModelQuery(100));
//		toSolve.add(new ModelQuery(100));
//		toSolve.add(new ModelQuery(100));
//		toSolve.add(new ModelQuery(100));
//		toSolve.add(new ModelQuery(100));
//		toSolve.add(new ModelQuery(100));
//		toSolve.add(new ModelQuery(100));
//		toSolve.add(new ModelQuery(100));
//		toSolve.add(new ModelQuery(102));


//		GraphSearcher pu = new PackUntilViolationGraphSearch(qtp, sla);
//		List<Action> puActions = pu.schedule(toSolve);
//		System.out.println(CostModelUtil.validate(toSolve, puActions, sla));
//		System.out.println(CostModelUtil.getFinalState(toSolve, puActions, sla));
		

		GraphSearcher ffd = new FirstFitDecreasingGraphSearch(sla, qtp, true);
		List<Action> ffdActions = ffd.schedule(toSolve);
//		System.out.println(CostModelUtil.validate(toSolve, ffdActions, sla));
		//System.out.println("FFD final: " + CostModelUtil.getFinalState(toSolve, ffdActions, sla));


		//Heuristic h = new UnassignedQueryTimeHeuristic(qtp);
		//Heuristic h = new NullHeuristic();

//		AStarGraphSearch optimal = new AStarGraphSearch(h, sla, qtp);
//
//
//		long start = System.currentTimeMillis();
//		List<Action> optimalActions = optimal.schedule(toSolve);
//		System.out.println("Finished optimal in " + (System.currentTimeMillis() - start));

		//System.out.println("Optimal final: " + CostModelUtil.getFinalState(toSolve, optimalActions, sla));
		

		// train the decision tree
//		File f = new File(file);
//		if (f.exists())
//			f.delete();
//		
//		Trainer t = new Trainer(file, sla);
//		t.train(200, solveSize);
//		t.close();


//		GraphSearcher dt = new DTSearcher(file, qtp, sla);
//		List<Action> dtActions = dt.schedule(toSolve);
//		System.out.println("DT final: " + CostModelUtil.getFinalState(toSolve, dtActions, sla));


		
		int ffdc = CostModelUtil.getCostForPlan(toSolve, ffdActions, sla);
		System.out.println(ffdc);
//		int optc = CostModelUtil.getCostForPlan(toSolve, optimalActions, sla);
//		int dtc = CostModelUtil.getCostForPlan(toSolve, dtActions, sla);
//		
//	
//		System.out.println(sla.getClass().getSimpleName() + "," + puc + "," + ffdc + "," + optc);
//		System.out.println(sla.getClass().getSimpleName() + "," + ffdc + "," + optc);
	}
}
