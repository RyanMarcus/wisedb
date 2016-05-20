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

/**
 * A utility class that wraps the user-facing features of WiSeDB
 * 
 * @author "Ryan Marcus <rcmarcus@brandeis.edu>"
 *
 */
public class WiSeDBUtils {
	
	/**
	 * Constructs a training dataset (to train a decision tree model) from the given
	 * workload specification (containing query templates and an SLA).
	 * 
	 * The training set will contain trainingSetSize random workloads, each with
	 * numQueriesPerWorkload random queries. trainingSetSize should be large (say 2000)
	 * and numQueriesPerWorkload should be small (8 - 12).
	 * 
	 * @param wf
	 * @param trainingSetSize
	 * @param numQueriesPerWorkload
	 * @return the training data, as a string
	 */
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
	
	
	/**
	 * Given training data, a workload specification, and a map indicating how many queries
	 * of each type are in a workload, returns a set of actions that can be performed to schedule
	 * the workload.
	 * 
	 * @param trainingData the training data, produced from constructTrainingData
	 * @param wf the workload specification
	 * @param queryFrequencies a map where the keys are the query IDs and the values are the frequency of the query inside the workload
	 * @return actions to schedule the workload
	 */
	public static List<Action> doPlacement(
			InputStream trainingData, 
			WorkloadSpecification wf,
			Map<Integer, Integer> queryFrequencies) {
		 
		
		 Set<ModelQuery> toSched = queryFrequencies.entrySet().stream()
		.flatMap(e -> IntStream.range(0, e.getValue()).mapToObj(i -> new ModelQuery(e.getKey())))
		.collect(Collectors.toSet());
		 
		 return SchedulerUtils.schedule(trainingData, wf, toSched);
		
	}


	public static List<Action> doPlacement(InputStream training, WorkloadSpecification wf, Set<ModelQuery> workload) {
		return SchedulerUtils.schedule(training, wf, workload);
	}
	
}
