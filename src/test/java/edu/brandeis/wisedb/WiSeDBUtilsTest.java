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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Test;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.sla.AverageLatencyModelSLA;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.cost.sla.PerQuerySLA;
import edu.brandeis.wisedb.cost.sla.PercentSLA;
import edu.brandeis.wisedb.scheduler.FirstFitDecreasingGraphSearch;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.PackNGraphSearch;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;

public class WiSeDBUtilsTest {

	@Test
	public void ffdBustingTest() throws InterruptedException, ExecutionException {

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
				new MaxLatencySLA(60000 + 91000, 1));
		
		Future<String> ftraining = WiSeDBUtils.constructTrainingData(wf, 500, 8);
		
		String training = ftraining.get();
		
		Map<Integer, Integer> queryFreqs = new HashMap<>();
		queryFreqs.put(1, 2);
		queryFreqs.put(2, 2);
		queryFreqs.put(3, 2);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(training.getBytes());
		
		List<AdvisorAction> a = WiSeDBUtils.doPlacement(bis, wf, queryFreqs);
	
		GraphSearcher ffdSearch = new FirstFitDecreasingGraphSearch(wf.getSLA(), wf.getQueryTimePredictor(), false);
		System.out.println(CostUtils.getCostForSearcher(ffdSearch, wf, queryFreqs));
		
		System.out.println(a);
		
		int cost = CostUtils.getCostForPlan(wf, a);
		
		assertEquals(32, cost);
	}
	
	
	@Test
	public void reproduce7c() throws InterruptedException, ExecutionException {
		// here, we will reproduce graph 7c from the paper.
		
		VMType[] types = new VMType[] { VMType.T2_MEDIUM };
		ModelSLA[] slas = new ModelSLA[] {
				PerQuerySLA.getLatencyTimesN(3.0),
				AverageLatencyModelSLA.tenMinuteSLA(),
				MaxLatencySLA.fifteenMinuteSLA(),
				PercentSLA.nintyTenSLA()
		};

		WorkloadSpecification[] ws = Arrays.stream(slas)
				.map(sla -> new WorkloadSpecification(types, sla))
				.toArray(i -> new WorkloadSpecification[i]);

		String[] training = Arrays.stream(ws)
				.map(wsp -> {
					System.out.println("Training on " + wsp.getSLA());
					// difference from paper: train on only 500 so we can run the test
					// faster on older machines with only two threads (instead of 12)
					return WiSeDBUtils.constructTrainingData(wsp, 500, wsp.getSLA().recommendedWorkloadSizeForSpeed());
				})
				.map(f -> {
					try {
						return f.get();
					} catch (Exception e) {
						throw new RuntimeException("Could not get training data!");
					}
				})
				.toArray(i -> new String[i]);
				

		System.out.println("Done training!");
		
		// difference from paper: use 2000 instead of 5000 queries for faster scheduling
		Set<ModelQuery> workload = ModelWorkloadGenerator.randomQueries(2000, 42, ws[0].getQueryTimePredictor().QUERY_TYPES);
		
		System.out.println("Created sample workload");
		
		// get all costs
		int[] ffd = new int[slas.length];
		int[] ffi = new int[slas.length];
		int[] pack9 = new int[slas.length];
		int[] dt = new int[slas.length];
		
		for (int i = 0; i < slas.length; i++) {
			GraphSearcher ffdSearch = new FirstFitDecreasingGraphSearch(ws[i].getSLA(), ws[i].getQueryTimePredictor());
			GraphSearcher ffiSearch = new FirstFitDecreasingGraphSearch(ws[i].getSLA(), ws[i].getQueryTimePredictor(), true);
			GraphSearcher pack9search = new PackNGraphSearch(9, ws[i].getQueryTimePredictor(), ws[i].getSLA());
			
			ffd[i] = CostModelUtil.getCostForPlan(ffdSearch.schedule(workload), ws[i].getSLA());			
			ffi[i] = CostModelUtil.getCostForPlan(ffiSearch.schedule(workload), ws[i].getSLA());
			pack9[i] = CostModelUtil.getCostForPlan(pack9search.schedule(workload), ws[i].getSLA());
			
			// not exactly reproducing: WiSeDBUtils does sanity checks on the
			// schedules produced.
			ByteArrayInputStream bis = new ByteArrayInputStream(training[i].getBytes());
			dt[i] = CostUtils.getCostForPlan(ws[i], WiSeDBUtils.doPlacement(bis, ws[i], workload));
		}
		


		/*
		 * PerQuerySLA.getLatencyTimesN(3.0),
		 * AverageLatencyModelSLA.tenMinuteSLA(),
		 * MaxLatencySLA.fifteenMinuteSLA(),
		 * PercentSLA.nintyTenSLA()
		 */
		
		
		assertTrue(dt[0] <= ffi[0] && dt[0] <= ffd[0] && dt[0] <= pack9[0]);
		assertTrue(dt[1] <= ffi[1] && dt[1] <= ffd[1] && dt[1] <= pack9[1]);
		assertTrue(dt[2] <= ffi[2] && dt[2] <= ffd[2] && dt[2] <= pack9[2]);
		assertTrue(dt[3] <= ffi[3] && dt[3] <= ffd[3] && dt[3] <= pack9[3]);

		
	}
	
	@Test
	public void cancelTest() throws InterruptedException {
		WorkloadSpecification wf = new WorkloadSpecification(new VMType[] { VMType.T2_SMALL, VMType.T2_MEDIUM },
				AverageLatencyModelSLA.tenMinuteSLA());
		Future<String> toCancel = WiSeDBUtils.constructTrainingData(wf, 5000, 30);
		
		Thread.sleep(5000);
		assertTrue(toCancel.cancel(true));
		
		Thread.sleep(1000);
		
		assertTrue(toCancel.isCancelled());
	}

	


}
