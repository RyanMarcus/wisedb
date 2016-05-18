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
 

package edu.brandeis.wisedb.scheduler;

import java.util.List;
import java.util.Set;

import edu.brandeis.wisedb.cost.Cost;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;

public interface GraphSearcher {
	
	public List<Action> schedule(Set<ModelQuery> toSched);
	
	public default Cost getCostForRandom(int seed, int count, ModelSLA sla, int[] classes) {
		Set<ModelQuery> toSched = ModelWorkloadGenerator.randomQueries(count, seed, classes);
		List<Action> actions = schedule(toSched);
		return CostModelUtil.getDetailedCostForPlan(toSched, actions, sla);
	}

	public default Cost getCostForRandom(QueryTimePredictor qtp, int seed, int count, ModelSLA sla) {
		return getCostForRandom(seed, count, sla, qtp.QUERY_TYPES);
	}
	
	public default Cost getCostForQueries(Set<ModelQuery> q, ModelSLA sla) {
		return CostModelUtil.getDetailedCostForPlan(q, schedule(q), sla);
	}
}
