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

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class SmallTreeGenerator {

	public static void main(String[] args) throws Exception {

	
		
		QueryTimePredictor qtp = new QueryTimePredictor();
		for (int i : qtp.QUERY_TYPES) {
			System.out.println(i + " // " + qtp.predict(new ModelQuery(i), VMType.T2_SMALL));
		}
		
		
//		System.out.print("90/10...");
//		compareSLAs("/Users/ryan/small/ntSLA.csv", PercentSLA.nintyTenSLA(), 10);
//		System.out.println(" done!");
		//System.out.print("per query...");

		//compareSLAs("/Users/ryan/small/pqSLA.csv", PerQuerySLA.getLatencyTimesN(2.0), 12);
		
		//System.out.println(" done!");
		System.out.print("total...");
		compareSLAs("/Users/ryan/small/tlSLA.csv", MaxLatencySLA.tenMinuteSLA(), 10);
		
		System.out.println(" done!");
//		System.out.print("average...");
//		
//		compareSLAs("/Users/ryan/small/agSLA.csv", AverageLatencyModelSLA.tenMinuteSLA(), 10);
//	
//		System.out.println(" done!");
	}
	
	
	
	
	public static void compareSLAs(String trainFile, ModelSLA sla, int tSize) throws Exception {
		
		// train the decision tree
		
		File f = new File(trainFile);
		if (f.exists())
			f.delete();
			
		Trainer t = new Trainer(trainFile, sla);
		t.train(3000, tSize);
		t.close();

		
//		GraphSearcher dt = new DTSearcher(trainFile, qtp, sla);
//		GraphSearcher ffd = new FirstFitDecreasingGraphSearch(sla, qtp);
//		GraphSearcher pu = new PackUntilViolationGraphSearch(qtp, sla);
//	
//
//		Cost dtCost = dt.getCostForRandom(42, 500, sla);
//		Cost ffdCost = ffd.getCostForRandom(42, 500, sla);
//		Cost puCost = pu.getCostForRandom(42, 500, sla);
//
//		System.out.print(sla.getClass().getSimpleName() + ",");
//		System.out.print(puCost.getTotalCost() + ",");
//		System.out.print(ffdCost.getTotalCost() + ",");
//		System.out.println(dtCost.getTotalCost());




			
		
		
	}
	
	

}
