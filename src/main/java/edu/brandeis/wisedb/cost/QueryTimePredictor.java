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
 
 

package edu.brandeis.wisedb.cost;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.brandeis.wisedb.aws.VMType;

public class QueryTimePredictor {

	private Map<Integer, Map<VMType, Integer>> latencyData;
	private Map<Integer, Map<VMType, Integer>> ioData;

	//public final static int[] QUERY_TYPES = new int[] {19, 18, 3, 4, 5, 6, 7, 8, 10, 14};
	//public static int[] QUERY_TYPES = new int[] {19, 18, 3, 7, 14};
	//public static int[] QUERY_TYPES = new int[] {19, 18, 3};
	//public static int[] QUERY_TYPES = new int[] {101, 102, 100};
	
	public int[] QUERY_TYPES;

	
	private VMType[] types = new VMType[] { VMType.T2_MEDIUM };
	
	//public static int[] QUERY_TYPES = new int[] {0, 1, 2, 3, 4};

	
	public QueryTimePredictor(Map<Integer, Map<VMType, Integer>> latency, Map<Integer, Map<VMType, Integer>> ioData, VMType[] types) {
		this(latency, ioData);
		this.types = types;
	}
	
	public QueryTimePredictor(Map<Integer, Map<VMType, Integer>> latency, Map<Integer, Map<VMType, Integer>> ioData) {
		this.latencyData = latency;
		this.ioData = ioData;
		
		QUERY_TYPES = this.latencyData.keySet()
				.stream()
				.mapToInt(i -> i)
				.toArray();
	}
	
	public QueryTimePredictor(VMType[] types) {
		this.types = types;
		loadValuesFromJSON();
	}
	
	public QueryTimePredictor() {
		loadValuesFromJSON();
	}
	
	public void loadValuesFromJSON() {
		latencyData = new HashMap<Integer, Map<VMType, Integer>>();
		ioData = new HashMap<Integer, Map<VMType, Integer>>();


		// read in the seed file
		try {
			ObjectMapper om = new ObjectMapper();
			JsonNode rootNode = om.readValue(new File("seed.json"), JsonNode.class);
			rootNode.fields().forEachRemaining((Entry<String, JsonNode> e) -> {
				Integer queryType = Integer.valueOf(e.getKey());
				HashMap<VMType, Integer> latency = new HashMap<VMType, Integer>();
				HashMap<VMType, Integer> io = new HashMap<VMType, Integer>();

				latencyData.put(queryType, latency);
				ioData.put(queryType, io);

				e.getValue().fields().forEachRemaining((Entry<String, JsonNode> ee) -> {
					VMType t = VMType.fromString(ee.getKey());
					latency.put(t, (int) (ee.getValue().path("latency").asDouble() * 1000.0));
					io.put(t, ee.getValue().path("ios").asInt());
				});
			});

		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		QUERY_TYPES = new int[] {6, 3, 10, 5, 18};//{18, 5, 14, 4, 6};

	}

	public int predict(ModelQuery q, ModelVM vm) {
		return predict(q, vm.getType());
	}

	public int predictIO(ModelQuery q, ModelVM vm) {
	

		if (!ioData.containsKey(q.getType()))
			return Integer.MAX_VALUE;
		return ioData.get(q.getType()).get(vm.getType());
	}

	/**
	 * Predicts the cost of a query ran on a certain type of VM
	 * 
	 * @param q the query to cost
	 * @param vm the VM to calculate the query cost for
	 * @return the cost of the query
	 */
	public int predict(ModelQuery q, VMType vm) {

		if (q instanceof SetLatencyModelQuery)
			return ((SetLatencyModelQuery) q).getLatency();
		
	

		if ((!latencyData.containsKey(q.getType()))
				|| (!latencyData.get(q.getType()).containsKey(vm)))
			throw new UnsupportedOperationException("Could not find latency data for query " + q + " on " + vm);
		
		return latencyData.get(q.getType()).get(vm);
	}

	public Set<ModelVM> getNewVMs() {
		Set<ModelVM> toR = new HashSet<ModelVM>();

		for (VMType t : types) {
			toR.add(new ModelVM(t));

		}

		return toR;
	}
	
	public VMType getOneVM() {
		return types[0];
	}
	
	public static void main(String[] args) {
		QueryTimePredictor qtp = new QueryTimePredictor();
		for (Integer i : qtp.QUERY_TYPES) {
			ModelVM vm = new ModelVM(VMType.T2_MEDIUM);
			ModelQuery q = new ModelQuery(i);
			int time = qtp.predict(q, vm);
			int ios = qtp.predictIO(q, vm);
			System.out.println(i + ", " + time + " (" + ios + " IOs)");

		}

		System.out.println("Type\tt2.med\tt2.small");
		ModelVM small = new ModelVM(VMType.T2_SMALL);
		ModelVM medium = new ModelVM(VMType.T2_MEDIUM);
		for (Integer i : qtp.QUERY_TYPES) {
			ModelQuery q = new ModelQuery(i);
			System.out.println(i + "\t" + medium.getCostForQuery(qtp, q) + "\t" + small.getCostForQuery(qtp, q));

		}

	}


}
