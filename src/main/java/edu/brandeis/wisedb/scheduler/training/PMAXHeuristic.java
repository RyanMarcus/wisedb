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

import java.util.List;

import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.Heuristic;
import edu.brandeis.wisedb.scheduler.PMAXTPGeneralGraphSearch;
import edu.brandeis.wisedb.scheduler.State;

public class PMAXHeuristic implements Heuristic {

	private QueryTimePredictor qtp;
	private ModelSLA sla;
	
	public PMAXHeuristic(QueryTimePredictor qtp, ModelSLA sla) {
		this.qtp = qtp;
		this.sla = sla;
	}
	
	@Override
	public int predictCostToEnd(State s) {
		PMAXTPGeneralGraphSearch gs = new PMAXTPGeneralGraphSearch(qtp, sla);
		
		List<Action> pmaxPlan = gs.schedule(s.getUnassignedQueries());
		return CostModelUtil.getCostForPlan(s.getUnassignedQueries(), pmaxPlan, sla);
	}

}
