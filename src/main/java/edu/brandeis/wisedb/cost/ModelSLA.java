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
 
 

package edu.brandeis.wisedb.cost;

import java.util.Map;

public interface ModelSLA {
	public int calculatePenalty(Map<ModelQuery, Integer> latencies);
	public boolean queryOrderMatters();
	public default boolean isMonotonicIncreasing() {
		return false;
	}
	
	public default int recommendedWorkloadSizeForSpeed() {
		return 9;
	}
}
