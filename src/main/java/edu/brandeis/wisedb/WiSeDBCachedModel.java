package edu.brandeis.wisedb;

import edu.brandeis.wisedb.scheduler.training.decisiontree.DTSearcher;

public class WiSeDBCachedModel {
	private DTSearcher dt;
	
	WiSeDBCachedModel(DTSearcher dt) {
		this.dt = dt;
	}
	
	DTSearcher getDT() {
		return dt;
	}
}
