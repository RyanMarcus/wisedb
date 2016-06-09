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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.AssignQueryAction;
import edu.brandeis.wisedb.scheduler.SchedulerUtils;
import edu.brandeis.wisedb.scheduler.StartNewVMAction;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

/**
 * A utility class that wraps the user-facing features of WiSeDB
 * 
 *
 */
public class WiSeDBUtils {

	// hide the default constructor
	private WiSeDBUtils() { }

	/**
	 * Constructs a training dataset (to train a decision tree model) from the given
	 * workload specification (containing query templates and an SLA).
	 * 
	 * The training set will contain trainingSetSize random workloads, each with
	 * numQueriesPerWorkload random queries. trainingSetSize should be large (say 2000)
	 * and numQueriesPerWorkload should be small (8 - 12).
	 * 
	 * The returned Future object will eventually contain the training data, and it
	 * can be cancelled, provided one passes the thread interrupt option.
	 * 
	 * @param wf the workload specification
	 * @param trainingSetSize the number of sample workloads to generate
	 * @param numQueriesPerWorkload the size of each training workload
	 * @return the training data, as a string
	 */
	public static Future<String> constructTrainingData(
			WorkloadSpecification wf,
			int trainingSetSize,
			int numQueriesPerWorkload) {

		Callable<String> c = () -> {
			QueryTimePredictor qtp = wf.getQueryTimePredictor();

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			PrintWriter pw = new PrintWriter(bos);


			Trainer t = new Trainer(qtp, pw, wf.getSLA());
			t.train(trainingSetSize, numQueriesPerWorkload);
			t.close();

			return bos.toString();
		};
		
		FutureTask<String> ft = new FutureTask<String>(c);
		(new Thread(ft)).start();
		return ft;

	}

	/**
	 * Sets the number of threads that will be used for training
	 * @param threads thread count
	 */
	public static void setThreadCountForTraining(int threads) {
		if (threads <= 0)
			throw new IllegalArgumentException("Number of threads must be greater than zero!");

		Trainer.numThreads = threads;
	}


	/**
	 * Given training data, a workload specification, and a map indicating how many queries
	 * of each type are in a workload, returns a list of actions that can be performed to schedule
	 * the workload.
	 * 
	 * @param trainingData the training data, produced from constructTrainingData
	 * @param wf the workload specification
	 * @param queryFrequencies a map where the keys are the query IDs and the values are the frequency of the query inside the workload
	 * @return actions to schedule the workload
	 */
	public static List<AdvisorAction> doPlacement(
			InputStream trainingData, 
			WorkloadSpecification wf,
			Map<Integer, Integer> queryFrequencies) {


		Set<ModelQuery> toSched = queryFrequencies.entrySet().stream()
				.flatMap(e -> IntStream.range(0, e.getValue()).mapToObj(i -> new ModelQuery(e.getKey())))
				.collect(Collectors.toSet());

		return SchedulerUtils.schedule(trainingData, wf, toSched)
				.stream().map(WiSeDBUtils::convertToAdvisorAction)
				.collect(Collectors.toList());

	}


	/**
	 * Given training data, a workload specification, and a set of ModelQuery objects, 
	 * this method returns a list of suggested actions to schedule the workload. 
	 * 
	 * @param training the training data, produced from constructTrainingData
	 * @param wf the workload specification
	 * @param workload a set of model queries representing the workload to schedule
	 * @return a list of actions to perform to schedule the workload
	 */
	public static List<AdvisorAction> doPlacement(InputStream training, WorkloadSpecification wf, Set<ModelQuery> workload) {
		return SchedulerUtils.schedule(training, wf, workload)
				.stream().map(WiSeDBUtils::convertToAdvisorAction)
				.collect(Collectors.toList());
	}

	private static AdvisorAction convertToAdvisorAction(Action a) {
		if (a instanceof AssignQueryAction)
			return new AdvisorActionAssign(((AssignQueryAction)a).getQuery().getType());

		if (a instanceof StartNewVMAction)
			return new AdvisorActionProvision(((StartNewVMAction)a).getType());

		throw new RuntimeException("Got unexpected action in convertToAdvisorAction: " + a);

	}

}
