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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.MaxLatencySLA;

public class WiSeDBUtilsTest {

	@Test
	public void ffdBustingTest() {

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
		
		String training = WiSeDBUtils.constructTrainingData(wf, 500, 8);
		
		Map<Integer, Integer> queryFreqs = new HashMap<>();
		queryFreqs.put(1, 2);
		queryFreqs.put(2, 2);
		queryFreqs.put(3, 2);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(training.getBytes());
		
		List<Action> a = WiSeDBUtils.doPlacement(bis, wf, queryFreqs);
	
		System.out.println(a);
		
		int cost = CostModelUtil.getCostForPlan(a, wf.getSLA(), wf.getQueryTimePredictor());
		
		assertEquals(14, cost);
	}

}
