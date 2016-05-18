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

public class Cost{
	private int queriesCost;
	private int VMBootCost;
	private int penaltyCost;
	
	public void setQueriesCost(int cost){
		this.queriesCost = cost;
	}
	public void setVMBootCost(int cost){
		this.VMBootCost = cost;
	}
	public void setPenaltyCost(int cost){
		this.penaltyCost = cost;
	}
	
	public int getQueriesCost(){
		return this.queriesCost;
	}
	public int getVMBootCost(){
		return this.VMBootCost;
	}
	public int getPenaltyCost(){
		return this.penaltyCost;
	}
	
	public int getTotalCost(){
		return this.queriesCost + this.VMBootCost + this.penaltyCost;
	}
	
	public String toString(){
		return "total Cost: "+this.getTotalCost()+", queries cost: "+this.queriesCost+
				", VM boot cost: "+this.VMBootCost+", penalty cost: "+this.penaltyCost;
	}
	
	public String toCSV() {
		// return CSV boot,queries,penalty
		return this.getVMBootCost() + ", " + this.getQueriesCost() + ", " + this.getPenaltyCost();
	}
}
