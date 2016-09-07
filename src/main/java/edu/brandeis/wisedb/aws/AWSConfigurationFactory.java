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

import java.nio.file.Path;

public class AWSConfigurationFactory {
	public static AWSConfiguration createConfiguration(String awsKey, 
			String awsKeySecret, 
			String awsSnapshot, 
			String awsAMI, 
			Path privateKeyPath, 
			String endpoint, 
			String keyPairName, 
			String subnetID) {
		
		
		return new PreconfiguredAWSConfiguration(awsKey, awsKeySecret, awsSnapshot,
				awsAMI, privateKeyPath, endpoint, keyPairName, subnetID);
		
	}
	
	
	private static class PreconfiguredAWSConfiguration implements AWSConfiguration {

		
		private String awsKey;
		private String awsKeySecret; 
		private String awsSnapshot;
		private String awsAMI;
		private Path privateKeyPath; 
		private String endpoint;
		private String keyPairName; 
		private String subnetID;
		
		public PreconfiguredAWSConfiguration(String awsKey, 
				String awsKeySecret, 
				String awsSnapshot, 
				String awsAMI, 
				Path privateKeyPath, 
				String endpoint, 
				String keyPairName, 
				String subnetID) {
			
			this.awsKey = awsKey;
			this.awsKeySecret = awsKeySecret;
			this.awsSnapshot = awsSnapshot;
			this.awsAMI = awsAMI;
			this.privateKeyPath = privateKeyPath;
			this.endpoint = endpoint;
			this.keyPairName = keyPairName;
			this.subnetID = subnetID;
			
		}
		
		public String getAWSKey() {
			return awsKey;
		}

		public String getAWSKeySecret() {
			return awsKeySecret;
		}

		public String getSnapshotID() {
			return awsSnapshot;
		}

		public String getAMIID() {
			return awsAMI;
		}

		public Path getPrivateKeyPath() {
			return privateKeyPath;
		}

		public String getEndpoint() {
			return endpoint;
		}

		public String getKeyPairName() {
			return keyPairName;
		}

		public String getSubnet() {
			return subnetID;
		}
		
	}
}
