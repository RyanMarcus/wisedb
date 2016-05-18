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

public class SetLatencyModelQuery extends ModelQuery {

	private int latency;
	
	public SetLatencyModelQuery(int type, int latency) {
		super(type);
		this.latency = latency;
	}
	
	public int getLatency() {
		return latency;
	}
	
	@Override
	public ModelQuery clone() {
		ModelQuery toR = new SetLatencyModelQuery(getType(), latency);
		toR.id = id;
		return toR;
	}

}
