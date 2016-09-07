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

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.AssignQueryAction;
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
	 * Gets the monetary cost (in 1/10 of cent) for a given action sequence
	 * @param wf the workload specification
	 * @param actions the list of actions
	 * @return the cost
	 */
	public static int getCostForPlan(WorkloadSpecification wf, List<AdvisorAction> actions) {
		return CostModelUtil.getCostForPlan(convertFromAdvisorActions(actions), wf.getSLA(), wf.getQueryTimePredictor());
	}
}
