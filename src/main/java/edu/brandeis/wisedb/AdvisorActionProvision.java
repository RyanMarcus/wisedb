package edu.brandeis.wisedb;

import edu.brandeis.wisedb.aws.VMType;

/**
 * This action represents provisioning a new VM of a certain type.
 * 
 *
 */
public class AdvisorActionProvision extends AdvisorAction {

	private VMType toProv;
	
	AdvisorActionProvision(VMType toProv) {
		this.toProv = toProv;
	}
	
	/**
	 * Returns the type of VM to provision
	 * @return the VM type
	 */
	public VMType getVMTypeToProvision() {
		return toProv; 
	}
	
	@Override
	public String toString() {
		return "[START " + toProv.toString() + "]";
	}
	
}
