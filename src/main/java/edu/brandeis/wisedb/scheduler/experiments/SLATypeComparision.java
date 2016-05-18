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

import edu.brandeis.wisedb.cost.Cost;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.AverageLatencyModelSLA;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.cost.sla.PerQuerySLA;
import edu.brandeis.wisedb.cost.sla.PercentSLA;
import edu.brandeis.wisedb.scheduler.FirstFitDecreasingGraphSearch;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.PackUntilViolationGraphSearch;
import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class SLATypeComparision {

	public static void main(String[] args) throws Exception {

		
		compareSLAs("ntSLA.csv", PercentSLA.nintyTenSLA(), 14);
		compareSLAs("pqSLA.csv", PerQuerySLA.getLatencyTimesN(3.0), 12);
		compareSLAs("tlSLA.csv", MaxLatencySLA.tenMinuteSLA(), 6);
		compareSLAs("agSLA.csv", AverageLatencyModelSLA.tenMinuteSLA(), 20);
	}
	
	
	public static void compareSLAs(String trainFile, ModelSLA sla, int tSize) throws Exception {
		QueryTimePredictor qtp = new QueryTimePredictor();
		
		// train the decision tree
		
		File f = new File(trainFile);
		if (f.exists())
			f.delete();
			
		Trainer t = new Trainer(trainFile, sla);
		t.train(3000, tSize);
		t.close();

		
		GraphSearcher dt = new DTSearcher(trainFile, qtp, sla);
		GraphSearcher ffd = new FirstFitDecreasingGraphSearch(sla, qtp);
		GraphSearcher pu = new PackUntilViolationGraphSearch(qtp, sla);
	

		Cost dtCost = dt.getCostForRandom(qtp, 42, 500, sla);
		Cost ffdCost = ffd.getCostForRandom(qtp, 42, 500, sla);
		Cost puCost = pu.getCostForRandom(qtp, 42, 500, sla);

		System.out.print(sla.getClass().getSimpleName() + ",");
		System.out.print(puCost.getTotalCost() + ",");
		System.out.print(ffdCost.getTotalCost() + ",");
		System.out.println(dtCost.getTotalCost());




			
		
		
	}
	
	

}
