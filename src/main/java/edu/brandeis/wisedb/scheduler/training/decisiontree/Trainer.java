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
 

package edu.brandeis.wisedb.scheduler.training.decisiontree;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.scheduler.AStarGraphSearch;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.AssignQueryAction;
import edu.brandeis.wisedb.scheduler.BudgetLimitedGraphSearch;
import edu.brandeis.wisedb.scheduler.Heuristic;
import edu.brandeis.wisedb.scheduler.StartNewVMAction;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;
import edu.brandeis.wisedb.scheduler.training.NullHeuristic;
import edu.brandeis.wisedb.scheduler.training.QueryGenerator;
import edu.brandeis.wisedb.scheduler.training.UnassignedQueryTimeHeuristic;

public class Trainer implements AutoCloseable{

	private static final Logger log = Logger.getLogger(BudgetLimitedGraphSearch.class.getName());

	private ModelSLA sla;

	public static int numThreads = 1;

	private QueryTimePredictor qtp;
	private Collection<Action> trainingData;
	private AtomicInteger count;
	private boolean haveHeaders = false;

	private ExecutorService tp;
	
	private PrintWriter out;
	private String filePath;

	private QueryGenerator qg;

	private int[] queryTypes;

	public Trainer(String filePath, ModelSLA sla) {
		this.filePath = filePath;
		this.sla = sla;
		this.queryTypes = null;
		qtp = new QueryTimePredictor();
		try {
			File f = new File(filePath);
			haveHeaders = f.exists();
			out = new PrintWriter(new FileWriter(f, true));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Trainer(QueryTimePredictor qtp, PrintWriter os, ModelSLA sla) { 
		this.filePath = "*custom outputstream*";
		this.sla = sla;
		this.qtp = qtp;
		haveHeaders = false;
		this.out = os;
		this.queryTypes = qtp.QUERY_TYPES;
	}

	public Trainer(String filePath, ModelSLA sla, int[] queryTypes) {
		this(filePath, sla);
		this.queryTypes = queryTypes;
	}

	public Trainer(String filePath, ModelSLA sla, QueryGenerator qg) {
		this(filePath, sla);
		this.qg = qg;
	}

	public String getFilePath() {
		return filePath;
	}

	public int train(int numInstances, int bruteForceSize) {
		trainingData = Collections.synchronizedList(new LinkedList<Action>());

		count = new AtomicInteger(0);

		tp = Executors.newFixedThreadPool(numThreads);



		for (int i = 0; i < numInstances; i++) {
			if (qg != null) {
				tp.submit(new TrainerThread(bruteForceSize, numInstances, qg));
				continue;
			}

			tp.submit(new TrainerThread(bruteForceSize, numInstances, queryTypes));

		}

		tp.shutdown();
		try {
			tp.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			log.warning("Training interrupted! Saving what we have now...");
			tp.shutdownNow();
		}



		printCSV(trainingData, out);

		return count.get();

	}

	
	
	public void interrupt() {
		tp.shutdownNow();
	}


	private class TrainerThread implements Runnable {

		private int bruteForceSize;
		private int[] queryTypes;

		private QueryGenerator qg;

		public TrainerThread(int bruteForceSize, int max, int[] queryTypes) {
			this.bruteForceSize = bruteForceSize;
			this.queryTypes = queryTypes;
		}

		public TrainerThread(int bruteForceSize, int max, QueryGenerator qg) {
			this.bruteForceSize = bruteForceSize;
			this.qg = qg;
			this.queryTypes = null;
		}

		@Override
		public void run() {
			try {



				Set<ModelQuery> workload;

				if (qg != null) {
					workload = qg.generateQueries(bruteForceSize);
				} else {

					if (queryTypes == null) {
						workload = ModelWorkloadGenerator.randomQueries(bruteForceSize);
					} else {
						workload = ModelWorkloadGenerator.randomQueries(bruteForceSize, queryTypes);
					}
				}


				Heuristic h;
				if (sla.isMonotonicIncreasing())
					h = new UnassignedQueryTimeHeuristic(qtp);
				else
					h = new NullHeuristic();

				AStarGraphSearch astar = new AStarGraphSearch(h, sla, qtp);



				List<Action> todo = astar.schedule(workload);
				if (todo == null) {
					// failed.
					return;
				}




				trainingData.addAll(todo);




				count.incrementAndGet();
				//				if (at % 10 == 0)
				//log.info(at + " / " + max);
			} catch (Exception e) {
				System.err.println("Error during training!");
				e.printStackTrace();
				System.exit(0);
			}
		}

	}


	private void printCSV(Collection<Action> todo, PrintWriter out) {

		if (!haveHeaders && !todo.isEmpty()) {
			for (String s : todo.iterator().next().stateAppliedTo.getFeatures().keySet()) {
				out.print(s + ",");
			}

			out.println("action");
			haveHeaders = true;

		}


		for (Action a : todo) {
			for (String f : a.stateAppliedTo.getFeatures().values()) {
				out.print(f + ",");
			}

			out.println(encodeAction(a));

		}

		out.flush();

	}




	private String encodeAction(Action a) {
		if (a instanceof StartNewVMAction) {
			return "N" + ((StartNewVMAction) a).getVM().getTypeString();
		}

		if (a instanceof AssignQueryAction) {
			return "P" + ((AssignQueryAction)a).getQuery().getType();
		}

		return "error";
	}

	@Override
	public void close() {
		out.flush();
		out.close();
	}




}
