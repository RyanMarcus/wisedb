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
 

package edu.brandeis.wisedb;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.SchedulerUtils;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

public class WiSeDBUtils {
	
	public static String constructTrainingData(
			WorkloadSpecification wf,
			int trainingSetSize,
			int numQueriesPerWorkload) {
		
		QueryTimePredictor qtp = wf.getQueryTimePredictor();
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(bos);
		
		
		Trainer t = new Trainer(qtp, pw, wf.getSLA());
		t.train(trainingSetSize, numQueriesPerWorkload);
		t.close();
		
		return bos.toString();
		
	}
	
	
	public static List<Action> doPlacement(
			InputStream trainingData, 
			WorkloadSpecification wf,
			Map<Integer, Integer> queryFrequencies) {
		 
		
		 Set<ModelQuery> toSched = queryFrequencies.entrySet().stream()
		.flatMap(e -> IntStream.range(0, e.getValue()).mapToObj(i -> new ModelQuery(e.getKey())))
		.collect(Collectors.toSet());
		 
		 return SchedulerUtils.schedule(trainingData, wf, toSched);
		
	}
	
}
