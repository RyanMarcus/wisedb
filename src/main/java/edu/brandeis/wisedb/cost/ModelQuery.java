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

import java.util.concurrent.atomic.AtomicInteger;

public class ModelQuery implements Comparable<ModelQuery> {
	private int type;
	private static AtomicInteger count = new AtomicInteger(0);

	
	protected int id;
	
	public ModelQuery(int type) {
		this.type = type;
		this.id = count.incrementAndGet();
	}
	
	public int getType() {
		return type;
	}
	
	public ModelQuery clone() {
		ModelQuery toR = new ModelQuery(type);
		toR.id = id;
		return toR;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ModelQuery))
			return false;
		
		ModelQuery q = (ModelQuery) o;
		return q.id == this.id;
	}
	
	@Override
	public int hashCode() {
		return Integer.valueOf(id).hashCode();
	}
	
	@Override 
	public int compareTo(ModelQuery other) {
		return this.getType() - other.getType();
	}
	
	@Override
	public String toString() {
		return "[" + type +  "]";
	}
}
