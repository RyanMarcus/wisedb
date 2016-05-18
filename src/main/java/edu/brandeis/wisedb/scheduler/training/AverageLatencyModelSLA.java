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
 

package edu.brandeis.wisedb.scheduler.training;

import java.util.Map;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.TightenableSLA;

public class AverageLatencyModelSLA implements TightenableSLA {

	private int latency;
	private int penalty;
	
	public AverageLatencyModelSLA(int avgLatency, int penalty) {
		latency = avgLatency;
		this.penalty = penalty;
	}
	
	@Override
	public int calculatePenalty(Map<ModelQuery, Integer> latencies) {
		if (latencies.values().size() == 0)
			return 0;
		
		
		double avg = latencies.values().stream().mapToInt((Integer i) -> {
			return i.intValue();
		}).average().getAsDouble();
		
		if (avg < latency)
			return 0;
		
		return (int)((avg - latency) * penalty);
	}
	
	public static TightenableSLA fiveMinuteSLA() {
		return new AverageLatencyModelSLA(5 * 60 * 1000, 1);
	}

	public static TightenableSLA tenMinuteSLA() {
		return new AverageLatencyModelSLA(10 * 60 * 1000, 1);

	}

	@Override
	public boolean queryOrderMatters() {
		return true;
	}

	@Override
	public TightenableSLA tighten(int amt) {
		return new AverageLatencyModelSLA(latency - amt, penalty);
	}
	

}
