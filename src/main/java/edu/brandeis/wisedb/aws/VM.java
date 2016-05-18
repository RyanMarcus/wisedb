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
 

package edu.brandeis.wisedb.aws;

public class VM {
	private VMCreator vmc;
	
	public String id;
	public String ip;
	
	public VM(String id, String ip, VMCreator vmc) {
		this.ip = ip;
		this.id = id;
		this.vmc = vmc;
	}
	
	public void terminateVM() throws VirtualMachineException {
		vmc.terminateVM(this);
	}
	
	public void foriciblyTerminateVM() throws VirtualMachineException {
		int trys = 5;
		while (trys != 0) {
			try {
				vmc.terminateVM(this);
			} catch (VirtualMachineException e) {
				trys++;
				
				if (trys == 0)
					throw e;
				
				continue;
			}
			break;
		}
	}
}
