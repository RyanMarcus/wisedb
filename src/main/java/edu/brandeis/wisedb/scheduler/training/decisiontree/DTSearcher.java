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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Logger;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.ModelSLA;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.sla.AverageLatencyModelSLA;
import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.AssignQueryAction;
import edu.brandeis.wisedb.scheduler.GraphSearcher;
import edu.brandeis.wisedb.scheduler.StartNewVMAction;
import edu.brandeis.wisedb.scheduler.State;
import edu.brandeis.wisedb.scheduler.training.ModelWorkloadGenerator;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;

public class DTSearcher implements GraphSearcher {

	private static final Logger log = Logger.getLogger(DTSearcher.class.getName());


	private J48 tree;
	private QueryTimePredictor qtp;
	private ModelSLA sla;
	private Attribute[] attributes;
	private Instances wekaDataSet;

	private void init(Instances is, QueryTimePredictor qtp, ModelSLA sla) throws Exception {
		wekaDataSet = is;


		this.qtp = qtp;
		this.sla = sla;

		// the last attribute is the action / class label
		wekaDataSet.setClassIndex(wekaDataSet.numAttributes() - 1);

		// build the tree with a pruning level of 0.25
		// and a minimum of 2 instances per leaf node
		String[] options = new String[] {
				"-C",
				"0.05",
				"-M",
				"2"
		};

		tree = new J48();
		tree.setOptions(options);

		// do 10 fold cross validation

		// do NOT train the classifier before evaluation!
		/*
		Evaluation eval = new Evaluation(wekaDataSet);
		eval.crossValidateModel(tree, wekaDataSet, 10, new Random());
		log.info("Model CV: correct/incorrect: " + eval.correct() + "/" + eval.incorrect());
		 */

		// build the tree
		tree.buildClassifier(wekaDataSet);


		attributes = new Attribute[wekaDataSet.numAttributes()];
		for (int i = 0; i < wekaDataSet.numAttributes(); i++) {
			attributes[i] = wekaDataSet.attribute(i);
		}


		log.finer("Got attributes: " + Arrays.toString(attributes));
	}

	public DTSearcher(InputStream trainingData, QueryTimePredictor qtp, ModelSLA sla) throws Exception {
		
		CSVLoader csv = new CSVLoader();
		csv.setSource(trainingData);
		csv.setMissingValue("?");
		
		Instances i = csv.getDataSet();
		
		init(i, qtp, sla);
	}

	public DTSearcher(String trainingData, QueryTimePredictor qtp, ModelSLA sla) throws Exception {
		init(new DataSource(trainingData).getDataSet(), qtp, sla);
	}

	@Override
	public List<Action> schedule(Set<ModelQuery> toSched) {
		SingularMachineState start = new SingularMachineState(toSched, qtp, sla);
		List<Action> toR = new LinkedList<Action>();


		applyLoop: while (!start.isGoalState()) {
			log.fine("Current state: " + start);


			SortedMap<String, String> features = start.getFeatures();
			Instance toClassify = new Instance(attributes.length);
			toClassify.setDataset(wekaDataSet);


			for (Attribute a : attributes) {
				if (a.name().equals("action")) {
					//toClassify.setValue(a, "N");
					continue;
				}

				try {

					if (features.get(a.name()).equals("?")) {
						toClassify.setMissing(a);
						continue;
					}
					try {
						double d = Double.valueOf(features.get(a.name()));
						toClassify.setValue(a, d);
					} catch (NumberFormatException e) {
						toClassify.setValue(a, features.get(a.name()));
					}
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					log.warning("Encountered previously unseen attribute value! Might need better training data... making random selection.");
					log.warning("Value for attribute " + a.name() + " was " + features.get(a.name()));
					Action rand = getPUAction(start);
					log.warning("Random action selected: " + rand);
					toR.add(rand);
					start.applyAction(rand);
					continue applyLoop;
				}
			}


			toClassify.setClassMissing();
			log.finer("Going to classify: " + toClassify);

			try {
				double d = tree.classifyInstance(toClassify);
				toClassify.setClassValue(d);
				String action = toClassify.stringValue(toClassify.classIndex());
				log.finer("Got action string: " + action);

				Action selected = null;
				for (Action a : start.getPossibleActions()) {
					if (actionMatches(a, action)) {
						selected = a;
						break;
					}
				}

				if (selected == null) {
					//log.warning("Could not find applicable action for string: " + action + " ... picking random action");
					Action a = getPUAction(start);
					start.applyAction(a);
					toR.add(a);
					continue;
				}

				log.fine("Selected action: " + selected);



				start.applyAction(selected);

				toR.add(selected);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}

		return toR;
	}

	private boolean actionMatches(Action a, String fromWeka) {
		if (fromWeka.startsWith("P")) {
			if (!(a instanceof AssignQueryAction))
				return false;

			int queryType = Integer.parseInt(fromWeka.substring(1));
			return ((AssignQueryAction) a).getQuery().getType() == queryType;
		}

		if (fromWeka.startsWith("N") && (a instanceof StartNewVMAction)) {
			if (fromWeka.substring(1).equals(((StartNewVMAction) a).getVM().getTypeString()))
				return true;
		}

		return false;
	}

	public static void main(String[] args) throws FileNotFoundException, Exception {
		QueryTimePredictor qtp = new QueryTimePredictor();
		ModelSLA sla = new AverageLatencyModelSLA(5 * 60 * 1000, 1);
		DTSearcher dt = new DTSearcher("/Users/ryan/tree_test.csv", qtp, sla);

		Set<ModelQuery> test = ModelWorkloadGenerator.randomQueries(20);
		dt.schedule(test);
	}

	private Action getPUAction(State last) {
		int base = last.getDetailedExecutionCost().getPenaltyCost();

		for (Action a : last.getPossibleActions()) {
			if (a instanceof StartNewVMAction)
				continue; // try to pick one that isn't starting a new VM

			if (last.getNewStateForAction(a).getDetailedExecutionCost().getPenaltyCost() <= base) {
				return a;
			}
		}

		for (Action a : last.getPossibleActions()) {
			if (a instanceof StartNewVMAction) {
				return a;
			}
		}

		log.warning("Could not find non-violating action: " + last);
		log.warning("Possible actions (" + last.getPossibleActions().size() + ") were: ");
		for (Action a : last.getPossibleActions()) {
			log.warning(a + " (with penalty: " + last.getNewStateForAction(a).getDetailedExecutionCost().getPenaltyCost() + " )");
		}
		return last.getPossibleActions().iterator().next();
	}

}
