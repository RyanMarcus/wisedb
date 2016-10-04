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
package edu.brandeis.wisedb;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.AssignQueryAction;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.StartNewVMAction;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;

/**
 * A utility package for calculating the cost of a workload strategy
 * 
 *
 */
public class CostUtils {
	
	// hide default constructor
	private CostUtils() {}
	
	private static List<Action> convertFromAdvisorActions(List<AdvisorAction> actions) {
		List<Action> toR = new LinkedList<Action>();
		ModelVM last = null;
		
		for (AdvisorAction a : actions) {
			if (a instanceof AdvisorActionProvision) {
				AdvisorActionProvision prov = (AdvisorActionProvision) a;
				StartNewVMAction toAdd = new StartNewVMAction(prov.getVMTypeToProvision());
				last = toAdd.getVM();
				toR.add(toAdd);
				continue;
			}
			
			if (a instanceof AdvisorActionAssign) {
				AdvisorActionAssign assign = (AdvisorActionAssign) a;
				Action toAdd = new AssignQueryAction(new ModelQuery(assign.getQueryTypeToAssign()), last);
				toR.add(toAdd);
				continue;
			}
			
			throw new RuntimeException("Unknown advisor action encountered: " + a);
		}
		
		return toR;
	}
	
	/**
	 * Gets the cost of using a particular heuristic method
	 * @param gs the graph searcher / method
	 * @param wf workload spec
	 * @param mqs model queries
	 * @return the cost of scheduling the queries with this method
	 */
	public static int getCostForSearcher(GraphSearcher gs, WorkloadSpecification wf, Map<Integer, Integer> queryFreqs) {
		Set<ModelQuery> workload = queryFreqs.entrySet().stream()
				.flatMap(e -> IntStream.range(0, e.getValue()).mapToObj(i -> new ModelQuery(e.getKey())))
				.collect(Collectors.toSet());
		
		List<Action> a = gs.schedule(workload);
		return CostModelUtil.getCostForPlan(workload, a, wf.getSLA(), wf.getQueryTimePredictor());
	}
	
	/**
	 * Gets the monetary cost (in 1/10 of cent) for a given action sequence
	 * @param wf the workload specification
	 * @param actions the list of actions
	 * @return the cost
	 */
	public static int getCostForPlan(WorkloadSpecification wf, List<AdvisorAction> actions) {
		return CostModelUtil.getCostForPlan(convertFromAdvisorActions(actions), wf.getSLA(), wf.getQueryTimePredictor());
	}
}
