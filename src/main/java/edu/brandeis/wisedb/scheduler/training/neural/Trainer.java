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
 

package edu.brandeis.wisedb.scheduler.training.neural;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;
import edu.brandeis.wisedb.scheduler.AStarGraphSearch;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.BestFirstGraphSearch;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.Heuristic;
import edu.brandeis.wisedb.scheduler.PackUntilViolationGraphSearch;
import edu.brandeis.wisedb.scheduler.RandomWalkPrefixGraphSearch;
import edu.brandeis.wisedb.scheduler.training.CostModelUtil;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;
import edu.brandeis.wisedb.scheduler.training.NullHeuristic;

public class Trainer {

	public static void main(String[] args) throws FileNotFoundException {
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.FINE);
		Logger.getLogger(AStarGraphSearch.class.getName()).addHandler(ch);
		Logger.getLogger(RandomWalkPrefixGraphSearch.class.getName()).addHandler(ch);

		Logger.getLogger(AStarGraphSearch.class.getName()).setLevel(Level.INFO);
		Logger.getLogger(RandomWalkPrefixGraphSearch.class.getName()).setLevel(Level.INFO);

		
		QueryTimePredictor qtp = new QueryTimePredictor();
		ModelSLA sla = new MaxLatencySLA(1000 * 60 * 20, 1);
		Heuristic h = new NullHeuristic();
		
		Set<Action> trainingData = new HashSet<Action>();
		
		
		
		int baseSolve = 4;
		int problemSize = 50;
		
		System.out.println("Solve size = " + baseSolve);
		

		Map<String, Bound> featureBounds = new HashMap<String, Bound>();
		featureBounds.put("remaining-queries", new Bound(0, problemSize));
		featureBounds.put("latency-diffs", new Bound(0, 2*426349));
		
		// see how much pack until costs
		PackUntilViolationGraphSearch puntil = new PackUntilViolationGraphSearch(qtp, sla);
		Set<ModelQuery> sample = ModelWorkloadGenerator.randomQueries(problemSize, 1, new int[] { 18 });
		List<Action> plan = puntil.schedule(sample);
		int puntilCost = (int) (1.5 * CostModelUtil.getCostForPlan(sample, plan, sla));
		
		featureBounds.put("pack-until", new Bound(0, puntilCost));
		
		
		
		Trainer t = new Trainer(h, sla, qtp, baseSolve);

		final int numberOfBaseProblems = 100;
		trainingData.addAll(t.getSamples(problemSize, numberOfBaseProblems, 3));
		
		System.out.println("Training heuristic on " + trainingData.size() + " data points");
		h = new NNHeuristic(trainingData, featureBounds, 0, puntilCost);
		for (int solveSize = baseSolve + 1; solveSize <= problemSize; solveSize++) {
			System.out.println("Solve size = " + solveSize);
			t = new Trainer(h, sla, qtp, solveSize);

			trainingData.addAll(t.getSamples(problemSize, 50, 3));

			System.out.println("Training heuristic on " + trainingData.size() + " data points");
			h = new NNHeuristic(trainingData, featureBounds, 0, puntilCost);
			// save the NN
			((NNHeuristic) h).saveNN(new File("recent.eg"), new File("inp_recent.norm"), new File("out_recent.norm"));
		}
		
		
		
		
		// use a greedy approach to solve the 10 problem with the trained heuristic. 
		GraphSearcher gs = new BestFirstGraphSearch(h, qtp, sla);
		Set<ModelQuery> toSchedule = ModelWorkloadGenerator.randomQueries(10, 42);
		
		long start = System.currentTimeMillis();
		System.out.println("Attempting large problem with greedy algorithm...");
		List<Action> results = gs.schedule(toSchedule);
		start = System.currentTimeMillis() - start;
		System.out.println("Solved in " + start + " (ms)");
		System.out.println(results);
		System.out.println("Plan costs: " + CostModelUtil.getCostForPlan(toSchedule, results, sla));
		System.exit(0);
		
		
	}
	
	private Heuristic h;
	private ModelSLA sla;
	private QueryTimePredictor qtp;
	private final int maxSolveSize;
	private AtomicInteger currentCount;
	
	public Trainer(Heuristic h, ModelSLA sla, QueryTimePredictor qtp, int maxSolveSize) {
		this.h = h;
		this.sla = sla;
		this.qtp = qtp;
		this.maxSolveSize = maxSolveSize;
	}
	
	public List<Action> getSample(int size) {
		GraphSearcher gs = new RandomWalkPrefixGraphSearch(h, sla, qtp, maxSolveSize);
		
		Set<ModelQuery> toSchedule = ModelWorkloadGenerator.randomQueries(size);

		long start = System.currentTimeMillis();
		List<Action> results = gs.schedule(toSchedule);
		start = System.currentTimeMillis() - start;
		
		if (results == null)
			return null;
		
		CostModelUtil.calculateCostToEnd(toSchedule, results, sla);
		while (results.get(0).stateAppliedTo.getUnassignedQueries().size() != maxSolveSize) {
			results.remove(0);
		}
		
		return results;
	}
	
	public List<Action> getSamples(int size, int numSamples, int numThreads) {
		currentCount = new AtomicInteger(0);
		ExecutorService tp = Executors.newFixedThreadPool(numThreads);
		
		List<Action> toR = new LinkedList<Action>();
		
		for (int i = 0; i < numSamples; i++)
			tp.submit(new TrainerThread(size, toR));
		
		
		
		tp.shutdown();
		try {
			tp.awaitTermination(10, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// we need to interrupt the tasks that are already running
			tp.shutdownNow();
			
			
			// just return what we've got
			return toR;
		}
		
		return toR;
		
	}

	
	private class TrainerThread implements Runnable {
		
		private int size;
		private List<Action> toAppendTo;
		private TrainerThread(int size, List<Action> toAppendTo) {
			this.size = size;
			this.toAppendTo = toAppendTo;
		}
		
		@Override
		public void run() {
			List<Action> t = getSample(size);
			
			if (t == null)
				return;
			
			if (Thread.currentThread().isInterrupted()) {
				return;
			}
			
			synchronized (toAppendTo) {
				toAppendTo.addAll(t);
			}
			
			int cur = currentCount.incrementAndGet();
			System.out.println(cur);
			
		}
		
	}
}
