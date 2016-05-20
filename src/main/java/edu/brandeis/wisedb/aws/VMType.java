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
 

package edu.brandeis.wisedb.aws;

public enum VMType {
	T2_SMALL, T2_MEDIUM, M3_MEDIUM, M3_LARGE, C4_LARGE, C4_XLARGE, UNIT;

	/**
	 * Gets the cost per hour in 1/10 of a cent
	 * 2.6 cents per hour will be returned as 26 
	 * 
	 * @return the cost per hour in 1/10 of a cent
	 */
	public int getCost() {
		switch (this) {
		case C4_LARGE:
			return 1160;
		case C4_XLARGE:
			return 2320;
		case M3_LARGE:
			return 1400;
		case M3_MEDIUM:
			return 700;
		case T2_MEDIUM:
			return 520;
		case T2_SMALL:
			return 260;
		case UNIT:
			return 1000*60*60; // costs 1 per milisecond
		default:
			return Integer.MAX_VALUE;

		}
	}

	public String toString() {
		switch (this) {
		case C4_LARGE:
			return "c4.large";
		case C4_XLARGE:
			return "c4.xlarge";
		case M3_LARGE:
			return "m3.large";
		case M3_MEDIUM:
			return "m3.medium";
		case T2_MEDIUM:
			return "t2.medium";
		case T2_SMALL:
			return "t2.small";
		case UNIT:
			return "unit VM";
		default:
			return "unknown";

		}
	}

	public static VMType fromString(String s) {
		switch (s) {
		case "c4.large":
			return C4_LARGE;
		case "c4.xlarge":
			return C4_XLARGE;
		case "m3.large":
			return M3_LARGE;
		case "m3.medium":
			return M3_MEDIUM;
		case "t2.medium":
			return T2_MEDIUM;
		case "t2.small":
			return T2_SMALL;
		case "unit":
			return UNIT;
		default:
			return null;
		}
	}


}
