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

import java.util.Calendar;

public class VMCreationTimeProfile {
	public long startedAt;
	public long readyReportedAt;
	public long sshConnectedAt;
	public VMType type;
	public VMDiskConfiguration disk;
	
	public String toCSV() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append(",");
		sb.append(disk);
		sb.append(",");
		sb.append(startedAt);
		sb.append(",");
		sb.append(readyReportedAt);
		sb.append(",");
		sb.append(sshConnectedAt);
		return sb.toString();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		long provisionTime = readyReportedAt - startedAt;
		long bootTime = sshConnectedAt - readyReportedAt;
		long totalTime = sshConnectedAt - startedAt;
		
		provisionTime /= 1000;
		bootTime /= 1000;
		totalTime /= 1000;
		
		sb.append("Total time: ");
		sb.append(totalTime);
		sb.append("s (");
		sb.append(provisionTime);
		sb.append("s provision time, ");
		sb.append(bootTime);
		sb.append("s boot time) ");
		sb.append(" at ");

		
		Calendar calendar = Calendar.getInstance();
	    calendar.setTimeInMillis(startedAt);
	    
	    sb.append(calendar.getTime().toString());
	    
	    sb.append( " for type ");
	    sb.append(type);
	    sb.append(" (");
	    sb.append(disk);
	    sb.append(")");
		
		return sb.toString();
	}
}
