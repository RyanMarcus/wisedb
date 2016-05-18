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
import edu.brandeis.wisedb.scheduler.training.PerQuerySLA;
import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class TemplateSuperSet {

	public static int[][] data = new int[10][10];
	
	public static void main(String[] args) throws Exception {
		//int[] allTypes = new int[] {6, 14, 19, 8, 3, 4, 7, 10, 5, 18};
		int[] allTypes = new int[] {6, 18, 14, 5, 19, 10, 8, 7, 3, 4};

		//Logger.getLogger("edu.brandeis").setLevel(Level.OFF);

		System.out.println("TrainedOn,TestWith,DT Boot, DT Query, DT Violation");
		
		
		for (int i = 10; i > 0; i--) {
			runTestWithClasses(Arrays.copyOfRange(allTypes, 0, i), 2000, 15);
		}
	
		System.out.println("\tT w/ 1\tT w/ 2\tT w/ 3\tT w/ 4\tT w/ 5\tT w/ 6\tT w/ 7\tT w/ 8\tT w/ 9\tT w/ 10");
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				if (j == 0)
					System.out.print("R w/ " + (i+1) + "\t");
				System.out.print(data[j][i] + "\t");
			}
			System.out.println();
		}


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
		
		
		
		DTSearcher dt = new DTSearcher(filename, qtp, sla);
		int numClasses = classes.length;
		while (numClasses > 0) {
			Cost dtCost = dt.getCostForRandom(42, 500, sla, Arrays.copyOf(classes, numClasses));
			System.out.println(classes.length + ", " + numClasses + ", " + dtCost.toCSV());
			data[classes.length - 1][numClasses - 1] = dtCost.getTotalCost();
			numClasses--;
		}
		
		
		
		
	}

}
