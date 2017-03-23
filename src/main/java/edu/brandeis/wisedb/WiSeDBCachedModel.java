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

import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;

/**
 * A cached WiSeDB model (black box)
 *
 */
public class WiSeDBCachedModel {
	private DTSearcher dt;
	private String trainingData;
	private WorkloadSpecification wf;
		
	WiSeDBCachedModel(DTSearcher dt, String trainingData, WorkloadSpecification wf) {
		this.dt = dt;
		this.trainingData = trainingData;
		this.wf = wf;
	}
	
	DTSearcher getDT() {
		return dt;
	}
	
	public String getDecisionTree() {
		return dt.getTree();
	}
	
	public String getTrainingData() {
		return trainingData;
	}
	
	public WorkloadSpecification getWorkloadSpecification() {
		return wf;
	}
}
