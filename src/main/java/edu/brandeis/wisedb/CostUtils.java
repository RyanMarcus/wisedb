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
