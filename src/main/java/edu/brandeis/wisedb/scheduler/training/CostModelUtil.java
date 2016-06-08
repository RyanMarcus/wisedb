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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.brandeis.wisedb.cost.Cost;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.AssignQueryAction;
import edu.brandeis.wisedb.scheduler.State;
import edu.brandeis.wisedb.scheduler.training.decisiontree.SingularMachineState;

public class CostModelUtil {
	
	public static QueryTimePredictor qtp = new QueryTimePredictor();
	
	public static int getCostForPlan(Set<ModelQuery> queries, List<Action> actions, ModelSLA sla) {
		return getDetailedCostForPlan(queries, actions, sla).getTotalCost();
	}
	
	public static int getCostForPlan(Set<ModelQuery> queries, List<Action> actions, ModelSLA sla, QueryTimePredictor qtp) {
		SingularMachineState s = new SingularMachineState(new LinkedList<ModelVM>(), queries, qtp, sla);
		
		for (Action a : actions) {
			s.applyAction(a);
		}
		
		return s.getDetailedExecutionCost().getTotalCost();
	}
	
	public static Cost getDetailedCostForPlan(Set<ModelQuery> queries, List<Action> actions, ModelSLA sla) {
		SingularMachineState s = new SingularMachineState(new LinkedList<ModelVM>(), queries, qtp, sla);
		for (Action a : actions) {
			s.applyAction(a);
		}
		return s.getDetailedExecutionCost();
	}
	
	public static State getFinalState(Set<ModelQuery> queries, List<Action> actions, ModelSLA sla) {
		SingularMachineState s = new SingularMachineState(new LinkedList<ModelVM>(), queries, qtp, sla);

		for (Action a : actions) {
			s.applyAction(a);
		}


		return s;
	}
	
	public static List<Action> calculateCostToEnd(Set<ModelQuery> queries, List<Action> actions, ModelSLA sla) {
		int remaining = getCostForPlan(queries, actions, sla);
		
		actions.get(0).computedCost = remaining;
		for (int i = 1; i < actions.size(); i++) {
			Action current = actions.get(i);
			Action previous = actions.get(i - 1);
			current.computedCost = previous.computedCost - previous.stateAppliedTo.getCostOfAction(previous);
		}
		actions.get(actions.size() - 1).computedCost = 0;
		
		return actions;
	}
	
	public static boolean validate(Set<ModelQuery> queries, List<Action> actions, ModelSLA sla) {
		SingularMachineState s = new SingularMachineState(new LinkedList<ModelVM>(), queries, qtp, sla);
		for (Action a : actions) {
			s.applyAction(a);
		}
		
		return s.isGoalState();
		
		
	}
	
	
	
	public static int getCostForPlan(List<Action> actions, ModelSLA sla) {
		return getCostForPlan(getQueriesFromActions(actions), actions, sla);
	}
	
	private static Set<ModelQuery> getQueriesFromActions(List<Action> actions) {
		return actions.stream()
				.filter(a -> a instanceof AssignQueryAction)
				.map(a -> (AssignQueryAction) a)
				.map(a -> a.getQuery())
				.collect(Collectors.toSet());
	}

	public static int getCostForPlan(List<Action> custom, ModelSLA sla, QueryTimePredictor qtp) {
		return getCostForPlan(getQueriesFromActions(custom), custom, sla, qtp);
	}
}
