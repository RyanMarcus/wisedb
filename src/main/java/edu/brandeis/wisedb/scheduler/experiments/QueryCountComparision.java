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

import edu.brandeis.wisedb.cost.Cost;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.FirstFitDecreasingGraphSearch;
import edu.brandeis.wisedb.scheduler.PackUntilViolationGraphSearch;
import edu.brandeis.wisedb.scheduler.PackUntilViolationRandomGraphSearch;
import edu.brandeis.wisedb.scheduler.training.PerQuerySLA;
import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class QueryCountComparision {

	public static void main(String[] args) throws Exception {
		//int[] allTypes = new int[] {6, 14, 19, 8, 3, 4, 7, 10, 5, 18};
		int[] allTypes = new int[] {6, 18, 14, 5, 19, 10, 8, 7, 3, 4};

		//Logger.getLogger("edu.brandeis").setLevel(Level.OFF);

		System.out.println("Types,DT Boot, DT Query, DT Violation, FFD Boot, FFD Query, FFD Violation, PU Boot, PU Query, PU Violation, PUR Boot, PUR Query, PUR Violation");
		
		
		//runTestWithClasses(Arrays.copyOfRange(allTypes, 0, 4), 900, 8);
		//runTestWithClasses(Arrays.copyOfRange(allTypes, 0, 6), 1000, 8);
		//runTestWithClasses(Arrays.copyOfRange(allTypes, 0, 8), 2000, 9);
		runTestWithClasses(Arrays.copyOfRange(allTypes, 0, 10), 2000, 9);

	


	}

	
	private static void runTestWithClasses(int[] classes, int samples, int bfSize) throws Exception {
		
		final String filename = "qcc" + classes.length + ".csv";
		
		QueryTimePredictor qtp = new QueryTimePredictor();
		ModelSLA sla = PerQuerySLA.getLatencyTimesN(3.0);
		
		// delete it if it already exists
		File f = new File(filename);
		if (f.exists())
			f.delete();
		
		// first we need to train
		Trainer t = new Trainer(filename, sla);
		t.train(samples, bfSize);
		t.close();
		
		
		
		Cost dtCost = (new DTSearcher(filename, qtp, sla)).getCostForRandom(42, 1000, sla, classes);
		Cost ffdCost = (new FirstFitDecreasingGraphSearch(sla, qtp)).getCostForRandom(42, 1000, sla, classes);
		Cost puCost = (new PackUntilViolationGraphSearch(qtp, sla)).getCostForRandom(42, 1000, sla, classes);
		Cost purCost = (new PackUntilViolationRandomGraphSearch(qtp, sla)).getCostForRandom(42, 1000, sla, classes);
		
		System.out.println(classes.length + ", " +
				dtCost.toCSV() + ", " +
				ffdCost.toCSV() + ", " +
				puCost.toCSV() + ", " + 
				purCost.toCSV());
		
		
		
	}
}
