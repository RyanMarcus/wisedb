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
import edu.brandeis.wisedb.cost.sla.PerQuerySLA;
import edu.brandeis.wisedb.scheduler.FirstFitDecreasingGraphSearch;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.PackUntilViolationGraphSearch;
import edu.brandeis.wisedb.scheduler.PackUntilViolationRandomGraphSearch;
import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class WorkloadSizeComparision {

	public static void main(String[] args) throws Exception {
		
		QueryTimePredictor qtp = new QueryTimePredictor();
		ModelSLA sla = PerQuerySLA.getLatencyTimesN(3.0);
		
		// train the decision tree
		
		File f = new File(args[0]);
		if (f.exists())
			f.delete();
			
		Trainer t = new Trainer(args[0], sla);
		t.train(2000, 9);
		t.close();

		
		GraphSearcher dt = new DTSearcher(args[0], qtp, sla);
		GraphSearcher ffd = new FirstFitDecreasingGraphSearch(sla, qtp);
		GraphSearcher pu = new PackUntilViolationGraphSearch(qtp, sla);
		GraphSearcher pur = new PackUntilViolationRandomGraphSearch(qtp, sla);
		
		System.out.println("Queries,DT Boot, DT Query, DT Violation, FFD Boot, FFD Query, FFD Violation, PU Boot, PU Query, PU Violation, PUR Boot, PUR Query, PUR Violation");

		
		for (int i = 1000; i <= 2000; i += 500) {
			Cost dtCost = dt.getCostForRandom(qtp, 42, i, sla);
			Cost ffdCost = ffd.getCostForRandom(qtp, 42, i, sla);
			Cost puCost = pu.getCostForRandom(qtp, 42, i, sla);
			Cost purCost = pur.getCostForRandom(qtp, 42, i, sla);
			
			System.out.print(i + ", ");
			System.out.print(dtCost.toCSV() + ", ");
			System.out.print(ffdCost.toCSV() + ", ");
			System.out.print(puCost.toCSV() + ", ");
			System.out.println(purCost.toCSV());



			
		}
		
	}
	
	
	
	

}
