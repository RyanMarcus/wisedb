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
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Logger;

public class TwoDimensionalArrayNormalizer {
	
	private double dmin;
	private double dmax;
	
	private double[] a;
	private double[] b;
	
	private double[] colMin;
	private double[] colMax;

	
	private static final Logger log = Logger.getLogger(TwoDimensionalArrayNormalizer.class.getName());

	
	public TwoDimensionalArrayNormalizer(double desiredMin, double desiredMax, double[][] data) {
		dmin = desiredMin;
		dmax = desiredMax;
		calculateRegressions(data);
	}
	
	public TwoDimensionalArrayNormalizer(File savedData) throws FileNotFoundException {
		Scanner s = new Scanner(savedData);
		dmin = s.nextDouble();
		dmax = s.nextDouble();
		
		int l = s.nextInt();
		a = new double[l];
		b = new double[l];
		colMin = new double[l];
		colMax = new double[l];
		
		
		for (int i = 0; i < l; i++) {
			colMin[i] = s.nextDouble();
		}
		
		for (int i = 0; i < l; i++) {
			colMax[i] = s.nextDouble();
		}
		
		for (int i = 0; i < l; i++) {
			a[i] = s.nextDouble();
		}
		
		for (int i = 0; i < l; i++) {
			b[i] = s.nextDouble();
		}
		s.close();
		
		
	}
	
	private void calculateRegressions(double[][] input) {
		colMin = new double[input[0].length];
		colMax = new double[input[0].length];
		
		for (int i = 0; i < input.length; i++) {
			for (int j = 0; j < input[i].length; j++) {
				colMin[j] = (colMin[j] > input[i][j] ? input[i][j] : colMin[j]);
				colMax[j] = (colMax[j] < input[i][j] ? input[i][j] : colMax[j]);
			}
		}
		
		// now, for each column we need some
		// a and some b such that
		// colMin*a + b = dmin => colMin*a + b + -dmin = 0 => colMin*a + -dmin = -b
		// colMax*a + b = dmax => colMax*a + b + -dmax = 0 => colMax*a + -dmax = -b
		// colMin*a + -dmin = colMax*a + -dmax
		// colMin*a + -colMax*a = -dmax + dmin
		// (colMin - colMax)*a = -dmax + dmin
		// a = (dmin - dmax) / (colMin - colMax)
		// b = dmin - colMin*((dmin - dmax) / (colMin - colMax))
		
		a = new double[input[0].length];
		b = new double[input[0].length];
		
		for (int i = 0; i < a.length; i++) {
			a[i] = (dmin - dmax) / (colMin[i] - colMax[i]);
			b[i] = dmin - colMin[i]*a[i];
		}
		
		
	}
	
	public double[][] normalize(double[][] input) {
		double[][] normData = new double[input.length][input[0].length];
		for (int i = 0; i < input.length; i++) {
			for (int j = 0; j < input[i].length; j++) {
				normData[i][j] = a[j] * input[i][j] + b[j];
				if (normData[i][j] < colMin[j] || normData[i][j] > colMax[j]) {
					log.warning("Value " + j + " out of bounds: " + normData[i][j] + " (bounds were: " + colMin[j] + ", " + colMax[j] + ")");
					normData[i][j] = Math.max(colMin[j], normData[i][j]);
					normData[i][j] = Math.min(colMax[j], normData[i][j]);
				}
					
			}
		}
		
		return normData;
	}
	
	public double[][] denormalize(double[][] input) {
		double[][] denorm = new double[input.length][input[0].length];
		for (int i = 0; i < input.length; i++) {
			for (int j = 0; j < input[i].length; j++) {
				denorm[i][j] = (input[i][j] - b[j])/a[j];
			}
		}
		
		return denorm;
	}
	
	public void saveToFile(File f) throws FileNotFoundException {
		PrintStream out = new PrintStream(f);
		out.println(dmin);
		out.println(dmax);
		out.println(a.length);
		
		for (double d : colMin) {
			out.println(d);
		}
		
		for (double d : colMax) {
			out.println(d);
		}
		
		for (double d : a) {
			out.println(d);
		}
		
		for (double d : b) {
			out.println(d);
		}
		
		out.close();
	}
	
	public static void main(String[] args) {
		double[][] d = { 
				{50, 60, 70},
				{80, 90, 100},
				{-10, -20, -30}
		};
		
		TwoDimensionalArrayNormalizer tdan = new TwoDimensionalArrayNormalizer(0.0, 1.0, d);
		
		System.out.println(Arrays.deepToString(tdan.normalize(d)));
		System.out.println(Arrays.deepToString(tdan.denormalize(tdan.normalize(d))));

		
	}
}
