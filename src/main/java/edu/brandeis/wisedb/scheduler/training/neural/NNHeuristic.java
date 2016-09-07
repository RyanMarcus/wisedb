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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.train.MLTrain;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.persist.EncogDirectoryPersistence;

import edu.brandeis.wisedb.scheduler.Action;
import edu.brandeis.wisedb.scheduler.Heuristic;
import edu.brandeis.wisedb.scheduler.State;

public class NNHeuristic implements Heuristic {

	private TwoDimensionalArrayNormalizer inputNorm;
	private TwoDimensionalArrayNormalizer outputNorm;
	private BasicNetwork nn;
	private String[] features;
	
	public NNHeuristic(Set<Action> trainingSet, Map<String, Bound> bounds, double minOut, double maxOut) {
		// impose an arbitrary ordering for ease of use
		List<Action> results = new ArrayList<Action>(trainingSet);
		
		
		// extract the ordering of the features
		features = results.get(0).stateAppliedTo.getFeatures().keySet().toArray(new String[] {});


		double[][] inputs = new double[results.size()][features.length];
		double[][] outputs = new double[results.size()][1];

		for (int i = 0; i < results.size(); i++) {
			for (int j = 0; j < features.length; j++) {
				inputs[i][j] = Double.valueOf(results.get(i).stateAppliedTo.getFeatures().get(features[j]));
			}
			outputs[i][0] = results.get(i).computedCost;
		}

		double[][] inputBounds = new double[2][features.length];
		for (int i = 0; i < features.length; i++) {
			inputBounds[0][i] = bounds.get(features[i]).min;
			inputBounds[1][i] = bounds.get(features[i]).max;
		}
		
		double[][] outputBounds = new double[2][1];
		outputBounds[0][0] = minOut;
		outputBounds[1][0] = maxOut;
		
		inputNorm = new TwoDimensionalArrayNormalizer(0, 1, inputBounds);
		outputNorm = new TwoDimensionalArrayNormalizer(0, 1, outputBounds);

		inputs = inputNorm.normalize(inputs);
		outputs = outputNorm.normalize(outputs);
		
		nn = new BasicNetwork();
		nn.addLayer(new BasicLayer(null, true, features.length));
		nn.addLayer(new BasicLayer(new ActivationSigmoid(), true, (features.length + 1) / 2));
		nn.addLayer(new BasicLayer(new ActivationSigmoid(), false, 1));
		nn.getStructure().finalizeStructure();
		nn.reset();

		BasicMLDataSet d = new BasicMLDataSet(inputs, outputs);
		MLTrain train = new ResilientPropagation(nn, d);

		do {
			train.iteration();
		} while (train.getError() > 0.02 && train.getIteration() <= 10000);
		train.finishTraining();		
		
		
		
		// TODO: log accuracy
		System.out.println("Error: " + train.getError());
	}

	public NNHeuristic(File savedNN, File savedInput, File savedOutput) throws FileNotFoundException {
		nn = (BasicNetwork) EncogDirectoryPersistence.loadObject(savedNN);
		inputNorm = new TwoDimensionalArrayNormalizer(savedInput);
		outputNorm = new TwoDimensionalArrayNormalizer(savedOutput);
	}
	
	@Override
	public int predictCostToEnd(State s) {
		if (features == null) {
			features = s.getFeatures().keySet().toArray(new String[] {});
		}
		
		double[][] inputs = new double[1][features.length];
		
		int i = 0;
		for (Entry<String, String> e : s.getFeatures().entrySet()) {
			inputs[0][i] = Double.valueOf(e.getValue());
			i++;
		}
		
		inputs = inputNorm.normalize(inputs);
		MLData result = nn.compute(new BasicMLData(inputs[0]));
		double[] nnResult = result.getData();
		//System.out.println(Arrays.toString(nnResult));
		Double denormed = outputNorm.denormalize(new double[][] {nnResult})[0][0];
		return denormed.intValue();
	}
	
	public void saveNN(File nnLoc, File inpNorm, File outNorm) throws FileNotFoundException {
		EncogDirectoryPersistence.saveObject(nnLoc, nn);
		inputNorm.saveToFile(inpNorm);
		outputNorm.saveToFile(outNorm);

	}

}
