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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.cost.TightenableSLA;
import edu.brandeis.wisedb.scheduler.training.decisiontree.Trainer;

/**
 * Utilities for using WiSeDB's adaptive modeling capabilities
 *
 */
public class AdaptiveModelingUtils {
	private static final Logger log = Logger.getLogger(AdaptiveModelingUtils.class.getName());

	/**
	 * Takes an initial SLA and tightening it repeatedly, returning the correct training
	 * data for each tightening.
	 * 
	 * @param wf the workload specification of the loosest SLA
	 * @param increm the amount to tigthen the SLA by each time
	 * @param numTightens the number of times to tighten the SLA
	 * @param workloadSize the size of each training workload
	 * @param numWorkloads the number of training workloads
	 * @return a list of training data, where the first element is the loosest SLA and the last element is the strictest
	 */
	public static List<WiSeDBCachedModel> tightenAndRetrain(WorkloadSpecification wf, int increm, int numTightens, int workloadSize, int numWorkloads) {
		List<WiSeDBCachedModel> toR = new ArrayList<>(numTightens + 1);
		List<String> trainingData = new ArrayList<>(numWorkloads);

		if (!(wf.getSLA() instanceof TightenableSLA)) {
			throw new WiSeDBRuntimeException("Can only use adaptive modeling on a tightenable SLA");
		}

		TightenableSLA sla = (TightenableSLA) wf.getSLA();

		List<Set<ModelQuery>> workloads = new ArrayList<>(numWorkloads);

		// Sample some workloads.
		Random r = new Random(42);
		int[] queryTypes = wf.getQueryTimePredictor().QUERY_TYPES;
		for (int i = 0; i < numWorkloads; i++) {
			Set<ModelQuery> toAdd = new HashSet<>();
			for (int j = 0; j < workloadSize; j++) {
				toAdd.add(new ModelQuery(queryTypes[r.nextInt(queryTypes.length)]));
			}
			workloads.add(toAdd);
		}

		// Now train for our loosest SLA.
		log.info("Starting training for the loosest SLA");
		Trainer t = new Trainer(wf.getQueryTimePredictor(), null, sla);
		trainingData = t.train(workloads);
		t.close();
		log.info("Finished initial training");


		// Figure out what the cost of each workload is under our current model
		// and SLA
		log.info("Costing each workload in the initial plan");
		List<Integer> costs = new ArrayList<>(numWorkloads);
		WiSeDBCachedModel model = WiSeDBUtils.getCachedModel(
				new ByteArrayInputStream(listToTrainingData(trainingData).getBytes()), 
				wf);
		toR.add(model);
		for (int i = 0; i < numWorkloads; i++) {
			List<AdvisorAction> sched = WiSeDBUtils.doPlacement(
					model, workloads.get(i));

			costs.add(CostUtils.getCostForPlan(wf, sched));
		}

		for (int i = 0; i < numTightens; i++) {
			// Build a new workload spec for the tightened SLA.
			WorkloadSpecification newWS = new WorkloadSpecification(wf.getQueryTimePredictor(),
					sla.tighten((i+1) * increm));

			// Build a list of indexes for each workload that has a new cost.
			List<Integer> changedIndexes = new LinkedList<>();
			for (int j = 0; j < workloads.size(); j++) {
				int oldCost = costs.get(j);
				List<AdvisorAction> sched = WiSeDBUtils.doPlacement(
						model, workloads.get(j));

				int newCost = CostUtils.getCostForPlan(newWS, sched);

				if (oldCost != newCost) {
					// Add this index.
					changedIndexes.add(j);
				}
			}

			log.info("After tightening, " + changedIndexes.size() + " training workloads have different costs. (SLA: " + newWS.getSLA() + ")");

			// Build a list of just these training workloads.
			List<Set<ModelQuery>> changedWorkloads = new ArrayList<>(changedIndexes.size());
			for (Integer idx : changedIndexes) {
				changedWorkloads.add(workloads.get(idx));
			}

			// Retrain these workloads.
			t = new Trainer(newWS.getQueryTimePredictor(), null, newWS.getSLA());
			List<String> newData = t.train(changedWorkloads);

			// Install the new training data into the original list.
			for (int idxCount = 0; idxCount < changedIndexes.size(); idxCount++) {
				trainingData.set(changedIndexes.get(idxCount), newData.get(idxCount));
			}

			// Update the cost array.
			model = WiSeDBUtils.getCachedModel(
					new ByteArrayInputStream(listToTrainingData(trainingData).getBytes()), 
					newWS);
			toR.add(model);
			for (Integer idx : changedIndexes) {
				List<AdvisorAction> sched = WiSeDBUtils.doPlacement(
						model, workloads.get(idx));
				int newCost = CostUtils.getCostForPlan(newWS, sched);
				costs.set(idx, newCost);
			}

		}

		return toR;
	}

	private static String listToTrainingData(List<String> trainingData) {
		// Remove the headers from all but the first entry.
		StringBuilder sb = new StringBuilder();
		sb.append(trainingData.get(0));

		for (int i = 1; i < trainingData.size(); i++) {
			String[] lines = trainingData.get(i).split("\n");
			for (int j = 1; j < lines.length; j++) {
				sb.append(lines[j]);
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	public static void main(String[] args) {

		long t = System.currentTimeMillis();

		WiSeDBUtils.setThreadCountForTraining(4);

		Map<Integer, Map<VMType, Integer>> latency = new HashMap<>();
		Map<VMType, Integer> forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 20000);
		latency.put(1, forMachine);

		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 30000);
		latency.put(2, forMachine);

		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 40000);
		latency.put(3, forMachine);


		Map<Integer, Map<VMType, Integer>> ios = new HashMap<>();
		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 10);
		ios.put(1, forMachine);

		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 10);
		ios.put(2, forMachine);

		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 10);
		ios.put(3, forMachine);

		WorkloadSpecification wf = new WorkloadSpecification(
				latency, 
				ios, 
				new VMType[] { VMType.T2_SMALL },
				new MaxLatencySLA(60000 + 100000, 1));

		List<WiSeDBCachedModel> models = AdaptiveModelingUtils.tightenAndRetrain(wf, 5000, 10, 9, 250);

		Map<Integer, Integer> freqs = new HashMap<>();
		freqs.put(1, 100);
		freqs.put(2, 100);
		freqs.put(3, 100);

		for (WiSeDBCachedModel model : models) {
			System.out.println(CostUtils.getCostForPlan(model.getWorkloadSpecification(),
					WiSeDBUtils.doPlacement(model, freqs)));
		}

		System.out.println("Time: " + (System.currentTimeMillis() - t));

	}
}
