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
 
 

package edu.brandeis.wisedb.scheduler.training;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.brandeis.wisedb.cost.ModelQuery;
import edu.brandeis.wisedb.cost.QueryTimePredictor;
import edu.brandeis.wisedb.cost.SetLatencyModelQuery;

public class ModelWorkloadGenerator {
	public static Set<ModelQuery> randomQueries(int num) {
		return ModelWorkloadGenerator.randomQueries(num, (int)System.currentTimeMillis());
	}
	
	public static Set<ModelQuery> randomQueries(int num, int seed) {
		return ModelWorkloadGenerator.randomQueries(num, seed, (new QueryTimePredictor()).QUERY_TYPES);
		
	}
	
	public static Set<ModelQuery> randomQueries(int num, int seed, int[] classes) {		
		Set<ModelQuery> toR = new HashSet<ModelQuery>();
		Random r = new Random(seed);
		for (int i = 0; i < num; i++) {
			toR.add(new ModelQuery(classes[r.nextInt(classes.length)]));
		}
		
		
		return toR;
	}

	public static Set<ModelQuery> randomQueries(int bruteForceSize, int[] queryTypes) {
		return randomQueries(bruteForceSize, (int)System.currentTimeMillis(), queryTypes);
	}
	
	public static Set<ModelQuery> randomQueries(int min, int max, int types, int width, int queries, int seed) {
		Random r = new Random(seed);
		return IntStream.range(0, queries).mapToObj(i -> {
			return generateSetQuery(r, min, max, types, width);
		}).collect(Collectors.toSet());
	}
	
	public static Set<ModelQuery> randomQueries(int min, int max, int types, int width, int queries) {
		Random r = new Random();
		return IntStream.range(0, queries).mapToObj(i -> {
			return generateSetQuery(r, min, max, types, width);
		}).collect(Collectors.toSet());
	}
	
	private static SetLatencyModelQuery generateSetQuery(Random r, int min, int max, int types, int width) {
		int range = (max - min) / types;
		int type = r.nextInt(types);
		int deviate = (int) (r.nextGaussian() * width);
		int center = (type * range) + min;
		
		deviate = (deviate > 2*width ? 2*width : deviate);
		
		
		return new SetLatencyModelQuery(type, Math.max(0, center + deviate));
		
	}
}
