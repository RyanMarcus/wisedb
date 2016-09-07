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

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.TightenableSLA;

public class PercentSLA implements TightenableSLA {

	private float proportion;
	private int underLimit;


	public PercentSLA(float prop, int under) {
		proportion = prop;
		underLimit = under;
	}

	@Override
	public int calculatePenalty(Map<ModelQuery, Integer> latencies) {
		if (latencies.size() == 0)
			return 0;
		
		long numOver = latencies.values().stream().filter(i -> i > underLimit).count();

//		System.out.println("Num over: " + numOver);
		
		if (numOver == 0)
			return 0;
		
		float prop = ((float)latencies.size() - (float)numOver) / (float)latencies.size();

//		System.out.println("Prop: " + prop);
		
		if (prop >= proportion)
			return 0;

		int maxOver = Math.round(latencies.size() * (1.f - proportion));

//		System.out.println("Max over: " + maxOver);
						
		return (int) (latencies.values().stream()
				.filter(i -> i > underLimit)
				.sorted((a, b) -> b - a)
				.limit(numOver - maxOver)
				.count() * 100000);				



	}
	
	public static TightenableSLA nintyTenSLA() {
		return new PercentSLA(0.90f, 10 * 60 * 1000);
	}
	
	public static TightenableSLA nintyTenSLA(int deadline) {
		return new PercentSLA(0.90f, deadline);
	}

	
	
	public static void main(String[] args) {
		ModelSLA sla = PercentSLA.nintyTenSLA();
		
		Map<ModelQuery, Integer> latencies = new HashMap<>();
		latencies.put(new ModelQuery(1), 9 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);
		latencies.put(new ModelQuery(1), 8 * 60 * 1000);


		latencies.put(new ModelQuery(1), 11 * 60 * 1000);
		latencies.put(new ModelQuery(1), 11 * 60 * 1000);


		
		System.out.println(sla.calculatePenalty(latencies));

	}

	@Override
	public boolean queryOrderMatters() {
		return true;
	}

	@Override
	public TightenableSLA tighten(int amt) {
		return new PercentSLA(proportion, underLimit - amt);
	}
	
	
	public int recommendedWorkloadSizeForSpeed() {
		return 7;
	}
}
