package edu.brandeis.wisedb;

/**
 * This action represents assigning a query of a particular template
 * to the most recenetly provisioned VM.
 * 
 * @author "Ryan Marcus <ryan@cs.brandeis.edu>"
 *
 */
public class AdvisorActionAssign extends AdvisorAction {
	private int queryTypeToAssign;
	
	AdvisorActionAssign(int qta) {
		this.queryTypeToAssign = qta;
	}
	

	/**
	 * Gets the type of query to be assigned to the most recently provisioned VM
	 * @return the template ID of the query to assign
	 */
	public int getQueryTypeToAssign() {
		return queryTypeToAssign;
	}
	
	@Override
	public String toString() {
		return "[ASSIGN " + getQueryTypeToAssign() + "]";		
	}
}
