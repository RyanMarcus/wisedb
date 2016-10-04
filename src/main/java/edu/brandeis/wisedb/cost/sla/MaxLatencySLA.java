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

import java.util.Map;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.TightenableSLA;

public class MaxLatencySLA implements TightenableSLA {

	private int latency;
	private int penalty;
	
	public MaxLatencySLA(int minLat, int penalty) {
		latency = minLat;
		this.penalty = penalty;
	}
	
	public int getLatency() {
		return latency;
	}
	
	@Override
	public int calculatePenalty(Map<ModelQuery, Integer> latencies) {
		return latencies.values().stream().mapToInt((Integer i) -> {
			if (i > latency) {
				return (i - latency) * penalty;
			}
			return 0;
		}).sum();
	}

	
	public static ModelSLA fiveMinuteSLA() {
		return new MaxLatencySLA(5 * 60 * 1000, 1);
	}

	public static ModelSLA tenMinuteSLA() {
		return new MaxLatencySLA(10 * 60 * 1000, 1);

	}
	
	public static ModelSLA fifteenMinuteSLA() {
		return new MaxLatencySLA(15 * 60 * 1000, 1);

	}

	@Override
	public boolean queryOrderMatters() {
		return false;
	}

	@Override
	public TightenableSLA tighten(int amt) {
		return new MaxLatencySLA(latency - amt, penalty);
	}
	

	@Override
	public boolean isMonotonicIncreasing() {
		return true;
	}
	
	@Override
	public String toString() {
		return "[Max latency: " + (latency - 60000) + "]";
	}
	
	
}
