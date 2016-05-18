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

import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.TightenableSLA;
import edu.brandeis.wisedb.scheduler.training.PercentSLA;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class TrainingTimeComparision {

	public static void main(String[] args) throws Exception {
		
		System.out.println("Looseness, Average, Total, Per Query");
		
//		for (double d = 1.0; d < 8.0; d += 0.02) {
//			ModelSLA sla1 = new AverageLatencyModelSLA((int) (d * 210000), 1);
//			ModelSLA sla2 = new SimpleLatencyModelSLA((int) (d  * 410000), 1);
//			ModelSLA sla3 = PerQuerySLA.getLatencyTimesN(d);
//			
//			System.out.print(d);
//			System.out.print(",");
//			timeAndPrint(sla1);
//			System.out.print(",");
//			timeAndPrint(sla2);
//			System.out.print(",");
//			timeAndPrint(sla3);
//			System.out.println();
//
//
//		}
		
		for (int i = 540; i > 420; i -= 2) {
			TightenableSLA sla = PercentSLA.nintyTenSLA();
			//TightenableSLA sla = new SimpleLatencyModelSLA(9 * 60 * 1000, 1);
			//TightenableSLA sla = PerQuerySLA.getLatencyTimesN(2.0);
			//TightenableSLA sla = new AverageLatencyModelSLA(9 * 60 * 1000, 1);
			timeAndPrint(sla);
		}
		
	}
	
	private static void timeAndPrint(ModelSLA sla) throws Exception {
		File f = new File("time_compare.csv");
		if (f.exists())
			f.delete();
		
		Trainer t = new Trainer("time_compare.csv", sla);
		long start = System.currentTimeMillis();
		t.train(10000, 6);
		System.out.println((System.currentTimeMillis() - start));
		System.out.println((System.currentTimeMillis() - start));
		System.out.println((System.currentTimeMillis() - start));
		System.out.println((System.currentTimeMillis() - start));

		t.close();
	}

}
