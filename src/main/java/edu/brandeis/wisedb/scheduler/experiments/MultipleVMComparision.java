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
import edu.brandeis.wisedb.cost.Cost;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.AverageLatencyModelSLA;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.cost.sla.PerQuerySLA;
import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class MultipleVMComparision {

	public static void main(String[] args) throws Exception {
		
		QueryTimePredictor qtp = new QueryTimePredictor(new VMType[] { VMType.T2_MEDIUM, VMType.T2_SMALL });
		runExperiment(qtp, "both");
		qtp = new QueryTimePredictor(new VMType[] { VMType.T2_SMALL });
		runExperiment(qtp, "small");
		qtp = new QueryTimePredictor(new VMType[] { VMType.T2_MEDIUM });
		runExperiment(qtp, "medium");
		

	}
	
	private static void runExperiment(QueryTimePredictor qtp, String label) throws Exception {
		ModelSLA[] slas = new ModelSLA[] {
				AverageLatencyModelSLA.tenMinuteSLA(),
				MaxLatencySLA.tenMinuteSLA(),
				PerQuerySLA.getLatencyTimesN(3.0)
		};
		
		for (ModelSLA sla : slas) {
			
			
			final String filename = "multivm_train_" + sla.getClass().getSimpleName() + label + ".csv";
			
			File f = new File(filename);
			if (f.exists())
				f.delete();
			
			Trainer t = new Trainer(filename, sla);
			
			long time = System.currentTimeMillis();
			if (sla instanceof MaxLatencySLA) {
				t.train(2000, 10);
			} else if (sla instanceof AverageLatencyModelSLA) {
				t.train(2000, 18);
			} else {
				t.train(2000, 18);
			}
			time = System.currentTimeMillis() - time;
			t.close();
			

			DTSearcher dt = new DTSearcher(filename, qtp, sla);
			
			
			Cost dtCost = dt.getCostForRandom(qtp, 42, 1000, sla);
			
			System.out.println(sla.getClass().getSimpleName() + ", " + label + ", " + dtCost.getTotalCost() + ", " + time);
		}
		
		
	}

}
