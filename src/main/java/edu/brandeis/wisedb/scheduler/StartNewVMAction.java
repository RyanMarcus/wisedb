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

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelVM;

public class StartNewVMAction extends Action {

	private ModelVM toStart;
	
	public StartNewVMAction(ModelVM t) {
		toStart = t;
	}
	
	public StartNewVMAction(VMType t) {
		this(new ModelVM(t));
	}
	
	public StartNewVMAction(ModelVM t, State appliedTo) {
		this(t);
		this.stateAppliedTo = appliedTo;
	}
	
	public VMType getType() {
		return toStart.getType();
	}
	
	public ModelVM getVM() {
		return toStart;
	}

	@Override
	public String toString() {
		return "[START " + toStart.toString() + " (" + computedCost + ")]";
	}
}
