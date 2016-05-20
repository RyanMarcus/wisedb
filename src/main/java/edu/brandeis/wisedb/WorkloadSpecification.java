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

import java.util.Map;

import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;

/**
 * A class representing a workload specification, which includes
 * a set of query templates and an SLA. 
 * 
 * @author "Ryan Marcus <rcmarcus@brandeis.edu>"
 *
 */
public class WorkloadSpecification {
	private final ModelSLA sla;
	private final QueryTimePredictor qtp;
	
	/**
	 * Construct a new workload specification.
	 * 
	 * @param latencyData a map from Int -> Map<VMType, Integer>, where the int is a query type, and the second map (<VMType, Integer>) gives the latency of the query on that VM type.
	 * @param ioData a similar map with the required number of IOs
	 * @param vmTypeData an array of VM types usable for this workload
	 * @param sla the SLA 
	 */
	public WorkloadSpecification(
			Map<Integer, Map<VMType, Integer>> latencyData,
			Map<Integer, Map<VMType, Integer>> ioData, 
			VMType[] vmTypeData, 
			ModelSLA sla) {
		
		
		this.sla = sla;		
		this.qtp = new QueryTimePredictor(latencyData, ioData, vmTypeData);
	}
	
	
	
	/**
	 * Construct a workload specification based on the 10 TPC-H templates used in
	 * the paper.
	 * 
	 * @param vmTypeData the VM types to use
	 * @param sla the SLA
	 */
	public WorkloadSpecification(VMType[] vmTypeData, ModelSLA sla) {
		qtp = new QueryTimePredictor(vmTypeData);
		this.sla = sla;
	}
	
	
	public ModelSLA getSLA() {
		return sla;
	}
	
	public QueryTimePredictor getQueryTimePredictor() {
		return qtp;
	}
	
	
	
	
}
