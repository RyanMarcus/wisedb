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
 

package edu.brandeis.wisedb.cost.sla;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.TightenableSLA;

public class PerQuerySLA implements TightenableSLA {

	
	private Map<Integer, Integer> latencies;
	private int penalty;
	
	public PerQuerySLA(Map<Integer, Integer> latencies, int penalty) {
		this.latencies = latencies;
		this.penalty = penalty;
	}
	
	@Override
	public int calculatePenalty(Map<ModelQuery, Integer> l) {
		
		int total = 0;
		
		for (Entry<ModelQuery, Integer> e : l.entrySet()) {
			Integer dueAt = latencies.get(e.getKey().getType());
			
			if (dueAt == null)
				continue;
			
			if (e.getValue() > dueAt) {
				total += (e.getValue() - dueAt) * penalty;
			}
			
		}
		
		return total;
	}
	
	
	
	/**
	 * A per query SLA with b = 1 that is used in the paper (query types = 18, 19)
	 * @return the SLA
	 */
	public static PerQuerySLA getPQSPaper() {
		Map<Integer, Integer> presets = new HashMap<Integer, Integer>();
		presets.put(19, 62000 + 10 + 157000);
		presets.put(18, 62000 + 10 + 157000 + 410000 + 10);
		
		return new PerQuerySLA(presets, 1);
	}
	
	/** 
	 * A per query SLA with b = 1 for the query types 3, 4, and 14
	 * @return the SLA
	 */
	public static PerQuerySLA getPQS1() {
		Map<Integer, Integer> presets = new HashMap<Integer, Integer>();
		presets.put(3, 200000 + 156000 + 62000 + 10);
		presets.put(4, 204000 + 156000 + 62000 + 10);
		presets.put(14, 156000 + 62000);
		
		return new PerQuerySLA(presets, 1);
	}
	
	/**
	 * A per query SLA with b = 2 for the query types 3, 4, and 14
	 * @return the SLA
	 */
	public static PerQuerySLA getPQS2() {
		Map<Integer, Integer> presets = new HashMap<Integer, Integer>();
		presets.put(3, 200000 + 62000);
		presets.put(4, 204000 + 62000);
		presets.put(14, 204000 + 156000 + 62000);
		
		return new PerQuerySLA(presets, 1);
	}
	
	/**
	 * A per query SLA with b = 3 for the query types 3, 4, and 14
	 * @return the SLA
	 */
	public static PerQuerySLA getPQS3() {
		Map<Integer, Integer> presets = new HashMap<Integer, Integer>();
		presets.put(3, 200000 + 62000);
		presets.put(4, 204000 + 200000 + 62000 + 10 );
		presets.put(14, 156000 + 204000 + 200000 + 62000 + 10 + 10);
		
		return new PerQuerySLA(presets, 1);
	}
	
	/**
	 * A per query SLA with b = 3 for the query types 3, 4, and 14
	 * @return the SLA
	 */
	public static PerQuerySLA getPQS4() {
		Map<Integer, Integer> presets = new HashMap<Integer, Integer>();
		presets.put(3, 200000 + 200000 + 62000 + 10);
		presets.put(4, 204000 + 200000 + 200000 + 62000 + 10 );
		presets.put(14, 156000 + 200000 + 200000 + 62000 + 10);
		
		return new PerQuerySLA(presets, 1);
	}
	
	
	/**
	 * A per query SLA with b = 3 for the query types 3, 4, 14, 18, 19
	 * @return the SLA
	 */
	public static PerQuerySLA getPQS5() {
		Map<Integer, Integer> presets = new HashMap<Integer, Integer>();
		presets.put(3, 200000 + 200000 + 62000 + 10);
		presets.put(4, 204000 + 200000 + 200000 + 62000 + 10 );
		presets.put(14, 156000 + 200000 + 200000 + 62000 + 10);
		
		presets.put(18, 410000 + 62000 + 10);
		presets.put(19, 410000 + 157000 + 10 + 62000 + 10);
		
		return new PerQuerySLA(presets, 1);
	}
	
	/**
	 * A per query SLA with b = 4 for the query types 3, 4, 5, 6, 14, 18, 19
	 * @return the SLA
	 */
	public static PerQuerySLA getPQS6() {
		Map<Integer, Integer> presets = new HashMap<Integer, Integer>();
		presets.put(3, 200000 + 200000 + 62000 + 10);
		presets.put(4, 204000 + 200000 + 200000 + 62000 + 10 );
		presets.put(14, 156000 + 200000 + 200000 + 62000 + 10);
		
		presets.put(18, 410000 + 62000 + 10);
		presets.put(19, 410000 + 157000 + 10 + 62000 + 10);
		presets.put(5, 236000 + 410000 + 157000 + 10 + 62000 + 10);
		presets.put(6, 148000 + 410000 + 157000 + 10 + 62000 + 10);

		
		return new PerQuerySLA(presets, 1);
	}
	
	/**
	 * A per query SLA with b = 3 for the query types {19, 18, 3, 4, 5, 6, 7, 14}
	 * @return the SLA
	 */
	public static PerQuerySLA getPQS7() {
		Map<Integer, Integer> presets = new HashMap<Integer, Integer>();
		presets.put(3, 200000 + 200000 + 62000 + 10);
		presets.put(4, 204000 + 200000 + 200000 + 62000 + 10 );
		presets.put(14, 156000 + 200000 + 200000 + 62000 + 10);
		
		presets.put(18, 410000 + 62000 + 10);
		presets.put(19, 410000 + 157000 + 10 + 62000 + 10);
		presets.put(5, 236000 + 410000 + 157000 + 10 + 62000 + 10);
		presets.put(6, 148000 + 410000 + 157000 + 10 + 62000 + 10);

		presets.put(7, 62000 + 205000 + 10);
		
		return new PerQuerySLA(presets, 1);
	}

	
	public static PerQuerySLA getLatencyTimesN(double n) {
		Map<Integer, Integer> presets = new HashMap<Integer, Integer>();
		QueryTimePredictor qtp = new QueryTimePredictor();
		
		for (Integer i : qtp.QUERY_TYPES) {
			presets.put(i, (int) (32000 + (n * qtp.predict(new ModelQuery(i), VMType.T2_MEDIUM))));
		}
		
		return new PerQuerySLA(presets, 1);
	}

	@Override
	public boolean queryOrderMatters() {
		return true;
	}

	@Override
	public TightenableSLA tighten(int amt) {
		Map<Integer, Integer> newL = new HashMap<Integer, Integer>();
		
		for (Map.Entry<Integer, Integer> e : latencies.entrySet()) {
			newL.put(e.getKey(), e.getValue() - amt);
		}
		
		PerQuerySLA toR = new PerQuerySLA(newL, penalty);
		return toR;
	}

	@Override
	public boolean isMonotonicIncreasing() {
		return true;
	}
	
	
}
