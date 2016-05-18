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

import java.util.Arrays;

import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.Heuristic;
import edu.brandeis.wisedb.scheduler.State;

public class UnassignedQueryTimeHeuristic implements Heuristic {

	private QueryTimePredictor qtp;
	private ModelVM[] vms;
	
	public UnassignedQueryTimeHeuristic(QueryTimePredictor qtp) {
		this.qtp = qtp;

		vms = qtp.getNewVMs().toArray(new ModelVM[] {});
	}
	
	
	@Override
	public int predictCostToEnd(State s) {
		int toR = s.getUnassignedQueries().stream().mapToInt(q ->
			 Arrays.stream(vms)
					.mapToInt(vm -> vm.getCostForQuery(qtp, q))
					.min()
					.getAsInt()
		).sum(); 

		//System.out.println("Minimum cost for " + s + " is: " + toR);
		
		return toR;
		
	}

}
