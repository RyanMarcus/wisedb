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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.brandeis.wisedb.aws.VMType;

public class ModelVM implements Comparable<ModelVM> {



	public static final int DB_SIZE_GB = 10;

	private VMType type;
	private Deque<ModelQuery> queries;

	public ModelVM(VMType type) {

		queries = new LinkedList<ModelQuery>();
		this.type = type;
	}





	public void addQuery(ModelQuery mq) {
		queries.add(mq);
	}

	public void addQueries(Collection<ModelQuery> toAdd) {
		queries.addAll(toAdd);
	}

	public Collection<ModelQuery> getQueries() {
		return queries;
	}

	public boolean removeQuery(ModelQuery mq) {
		return queries.remove(mq);
	}

	public List<ModelQuery> removeLastNQueries(int n) {
		if (n > this.getNumberOfQueries())
			return null;

		List<ModelQuery> toR = new ArrayList<ModelQuery>(n);
		while (toR.size() != n) {
			toR.add(queries.removeLast());
		}

		Collections.reverse(toR);
		return toR;
	}

	public VMType getType() {
		return type;
	}


	public String getTypeString() {
		return String.valueOf(type);
	}

	public int getProvisionTime() {
		// TODO: should be loaded from file / more precise based on experiments
		return 20000;
	}

	public int getBootTime() {
		// TODO: should be loaded from file / more precise based on experiments
		return 40000;
	}

	public int getCostForTime(int time) {
		// convert miliseconds to hours
		double hours = (double)time / 1000.0;
		hours /= 60;
		hours /= 60;

		// we need to add in the EBS cost
		// which is 0.05 cents / GB month
		// $0.000007 per hour
		double IOcost = 0.000007;

		// return the cost
		double cost = ((type.getCost() * hours) + (IOcost * hours));
		return (int) Math.ceil(cost);
		
		//TODO : reset
		//return time;
	}

	public int getCostForQueries(QueryTimePredictor qtp) {

		int toR = 0;
		for (ModelQuery q : queries) {
			toR += getCostForQuery(qtp, q);
		}
		
		return toR;
		

	}

	public int getCostForQuery(QueryTimePredictor qtp, ModelQuery q) {

		

		int queryTime = qtp.predict(q, this);
		int ioCost = qtp.predictIO(q, this);

		
		// TODO: no IO costs
		ioCost = (int) Math.ceil(((double)ioCost) * 0.000000000005);

		return getCostForTime(queryTime) + ioCost;
	}

	public Map<ModelQuery, Integer> getQueryLatencies(QueryTimePredictor qtp) {
		int latency = getProvisionTime() + getBootTime();
		Map<ModelQuery, Integer> toR = new HashMap<ModelQuery, Integer>();
		for (ModelQuery q : queries) {
			latency += qtp.predict(q, this);
			toR.put(q, latency);
		}

		return toR;
	}

	public int getRemainingRunningTime(QueryTimePredictor qtp) {
		Map<ModelQuery, Integer> l = this.getQueryLatencies(qtp);
		return l.values().stream().mapToInt((Integer i) -> i.intValue()).sum();
	}


	public int getWaitingTime(QueryTimePredictor qtp) {
		return getRemainingRunningTime(qtp) + this.getBootTime() + this.getProvisionTime();
	}

	public int getCostToBoot() {
		return getCostForTime(getProvisionTime() + getBootTime());
	}

	@Override
	public int compareTo(ModelVM other) {
		return this.getType().getCost() - other.getType().getCost();
	}

	public ModelVM clone() {
		ModelVM toR = new ModelVM(type);

		for (ModelQuery mq : queries) {
			toR.addQuery(mq.clone()); // TODO need clone here?
		}


		return toR;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ModelVM))
			return false;

		ModelVM vm = (ModelVM) o;
		//return vm.type == this.type && this.queries.equals(vm.queries);

		if (!vm.type.equals(type))
			return false;


		if (!vm.getQueries().equals(this.getQueries()))
			return false;
		

		return true;


	}

	@Override
	public int hashCode() {
		return queries.hashCode();
	}


	@Override
	public String toString() {
		return type.toString() + "(" + getNumberOfQueries() + ")";
	}

	public int getNumberOfQueries() {
		return queries.size();
	}

	public void removeAllQueries() {
		while (!queries.isEmpty())
			queries.remove();

	}

	public static void main(String[] args) {
		ModelVM v = new ModelVM(VMType.T2_MEDIUM);
		v.addQuery(new ModelQuery(18));

		QueryTimePredictor qtp = new QueryTimePredictor();

		System.out.println(v.getCostForQueries(qtp));

	}





	public void sort(QueryTimePredictor qtp) {
		// sort the assigned queries by latency
		List<ModelQuery> q = new ArrayList<ModelQuery>(queries);
		q.sort((a, b) -> qtp.predict(a, qtp.getOneVM()) - qtp.predict(b, qtp.getOneVM()));
		queries = new LinkedList<>(q);
	}






}
