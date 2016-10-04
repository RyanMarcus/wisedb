package edu.brandeis.wisedb.scheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.util.IOUtils;
import com.google.common.io.Files;

import edu.brandeis.wisedb.WorkloadSpecification;
import edu.brandeis.wisedb.aws.VMType;
import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelVM;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.MaxLatencySLA;

public class MLPGraphSearcher implements GraphSearcher {

	private MaxLatencySLA sla;
	private QueryTimePredictor qtp;
	private String glpsolPath;

	public MLPGraphSearcher(MaxLatencySLA deadline, QueryTimePredictor qtp, String glpsolPath) {
		this.sla = deadline;
		this.qtp = qtp;
		this.glpsolPath = glpsolPath;
	}

	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		List<ModelQuery> queryList = new ArrayList<>(toSched);
		List<Integer> queryWeights = queryList.stream()
				.map(q -> qtp.predict(q, VMType.T2_SMALL))
				.collect(Collectors.toList());

		int maxBins = (int)(new FirstFitDecreasingGraphSearch(sla, qtp)).schedule(toSched).stream()
				.filter(a -> a instanceof StartNewVMAction)
				.count();

		int deadline = sla.getLatency() - 60000;
		
		StringBuilder sb = new StringBuilder();

		// make variables for each query
		for (int qIdx = 0; qIdx < queryList.size(); qIdx++) {
			for (int binIdx = 0; binIdx < maxBins; binIdx++) {
				sb.append("var x" + qIdx + "d" + binIdx + ", >= 0, <= 1, integer;\n");
			}
		}

		// make variables for each bin
		for (int binIdx = 0; binIdx < maxBins; binIdx++) {
			sb.append("var y" + binIdx + ", >= 0, <= 1, integer;\n");
		}

		// constraints so no bin is too full
		for (int binIdx = 0; binIdx < maxBins; binIdx++) {
			sb.append("s.t. binSize" + binIdx + ": ");
			List<String> constraints = new ArrayList<>(queryList.size());
			for (int qIdx = 0; qIdx < queryList.size(); qIdx++) {
				constraints.add(queryWeights.get(qIdx) + "*x" + qIdx + "d" + binIdx);
			}
			sb.append(constraints.stream().collect(Collectors.joining(" + ")));
			sb.append(" <= " + deadline + "*y" + binIdx + ";\n");
		}

		// constraints so every item is in one bin
		for (int qIdx = 0; qIdx < queryList.size(); qIdx++) {
			sb.append("s.t. placed" + qIdx +": ");
			List<String> constraints = new ArrayList<>(maxBins);
			for (int binIdx = 0; binIdx < maxBins; binIdx++) {
				constraints.add("x" + qIdx + "d" + binIdx);
			}
			sb.append(constraints.stream().collect(Collectors.joining(" + ")));
			sb.append(" = 1;\n");
		}

		// minimize the number of bins
		{
			sb.append("minimize numBins: ");
			List<String> constraints = new ArrayList<>(maxBins);
			for (int binIdx = 0; binIdx < maxBins; binIdx++) {
				constraints.add("y" + binIdx);
			}
			sb.append(constraints.stream().collect(Collectors.joining(" + ")));
			sb.append(";\n");
		}


		sb.append("solve;\n");

		// display all the assignment variables
		List<String> constraints = new ArrayList<>(queryList.size());
		for (int binIdx = 0; binIdx < maxBins; binIdx++) {
			for (int qIdx = 0; qIdx < queryList.size(); qIdx++) {
				constraints.add("x" + qIdx + "d" + binIdx);
			}
		}
		
		sb.append("printf \"!!!\";\ndisplay ");
		sb.append(constraints.stream().collect(Collectors.joining(", ")));
		sb.append(";\n");
		
		
		// TODO use os.path.join equiv or at least get sys sep instead of /...
		try {
			String path = System.getProperty("user.home") + "/.prog.mod";
			File f = new File(path);
			Files.write(sb.toString().getBytes(), f);
			Thread.sleep(500);
			Process p = Runtime.getRuntime().exec(glpsolPath + " --math --tmlim 5 " + path);
			p.waitFor();
			String output = IOUtils.toString(p.getInputStream());
			return processGLPSOLOutput(output, maxBins, queryList);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	private List<Action> processGLPSOLOutput(String output, int maxBins, List<ModelQuery> qs) {
		List<List<ModelQuery>> vms = new ArrayList<>(maxBins);
		
		for (int i = 0; i < maxBins; i++)
			vms.add(new LinkedList<ModelQuery>());
		
		
		String[] lines = output.split("\n");
		Set<ModelQuery> remaining = new HashSet<>(qs);
		for (String line : lines) {
			if (!line.contains(".val ="))
				continue;
			
			int value = Integer.valueOf(line.split("=")[1].trim());
			int query = Integer.valueOf(line.split("d")[0].substring(1).trim());
			int vm = Integer.valueOf(line.split("d")[1].split("\\.")[0].trim());
			if (value == 1) {
				if (!remaining.contains(qs.get(query))) {
					System.err.println("solution double-assigned query " + query);
					return null;
				}
				vms.get(vm).add(qs.get(query));
				remaining.remove(qs.get(query));
			}
		}
		
		if (remaining.size() != 0) {
			System.err.println("solution did not assign these queries: " + remaining);
			return null;
		}
		
		
		List<Action> toR = new LinkedList<Action>();
		for (List<ModelQuery> vm : vms) {
			if (vm.size() == 0)
				continue;
			
			ModelVM toAdd = new ModelVM(VMType.T2_SMALL);
			toR.add(new StartNewVMAction(toAdd));
			for (ModelQuery q : vm) {
				toR.add(new AssignQueryAction(q, toAdd));
			}
		}
		return toR;
	}
	
	
	public static void main(String[] args) {
		Map<Integer, Map<VMType, Integer>> latency = new HashMap<>();
		Map<VMType, Integer> forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 20000);
		latency.put(1, forMachine);
		
		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 30000);
		latency.put(2, forMachine);
		
		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 80000);
		latency.put(3, forMachine);
		
		
		Map<Integer, Map<VMType, Integer>> ios = new HashMap<>();
		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 10);
		ios.put(1, forMachine);
		
		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 10);
		ios.put(2, forMachine);
		
		forMachine = new HashMap<>();
		forMachine.put(VMType.T2_SMALL, 10);
		ios.put(3, forMachine);
		
		WorkloadSpecification wf = new WorkloadSpecification(
				latency, 
				ios, 
				new VMType[] { VMType.T2_SMALL },
				new MaxLatencySLA(60000 + 91000, 1));
		
		MLPGraphSearcher gs = new MLPGraphSearcher((MaxLatencySLA) wf.getSLA(), wf.getQueryTimePredictor(), "/usr/local/bin/glpsol");
		
		Set<ModelQuery> mqs = new HashSet<>();
		mqs.add(new ModelQuery(1));
		mqs.add(new ModelQuery(1));
		mqs.add(new ModelQuery(1));
		mqs.add(new ModelQuery(2));
		mqs.add(new ModelQuery(2));
		mqs.add(new ModelQuery(2));
		mqs.add(new ModelQuery(3));
		mqs.add(new ModelQuery(3));
		mqs.add(new ModelQuery(3));
		mqs.add(new ModelQuery(1));
		mqs.add(new ModelQuery(1));
		mqs.add(new ModelQuery(1));
		mqs.add(new ModelQuery(2));
		mqs.add(new ModelQuery(2));
		mqs.add(new ModelQuery(2));
		mqs.add(new ModelQuery(3));
		mqs.add(new ModelQuery(3));
		mqs.add(new ModelQuery(3));
		mqs.add(new ModelQuery(1));
		mqs.add(new ModelQuery(1));
		mqs.add(new ModelQuery(1));
		mqs.add(new ModelQuery(2));
		mqs.add(new ModelQuery(2));
		mqs.add(new ModelQuery(2));
		mqs.add(new ModelQuery(3));
		mqs.add(new ModelQuery(3));
		mqs.add(new ModelQuery(3));

		System.out.println(gs.schedule(mqs));
	}

}
