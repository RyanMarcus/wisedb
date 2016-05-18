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

import java.nio.file.FileSystems;


public class OneOfEachTypeStarter {


	public static void main(String[] args) throws InterruptedException {
		// TODO: don't include this in the source file
		/*private static final String key = "AKIAJLLEH6G7C635346A";
		private static final String secret = "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";

		private static final String snapshot = "snap-16dc3f9b";
		private static final String ami = "ami-5c541034";
		
		private static final Path keypath = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".ssh", "xcloud.pem");
		
		endpoint = "ec2.us-east-1.amazonaws.com"
		private key pair = "xcloud"
		subnet id = "subnet-1ed37969"
	*/
		
		
		AWSConfiguration aws = AWSConfigurationFactory.createConfiguration(
				"AKIAJLLEH6G7C635346A",
				"lb5h/YojZSEo8aVLbMoz5ZyVLMYIM21qfgCOyr8Y", 
				"snap-16dc3f9b", 
				"ami-5c541034", 
				FileSystems.getDefault().getPath(System.getProperty("user.home"), ".ssh", "xcloud.pem"), 
				"ec2.us-east-1.amazonaws.com", 
				"xcloud", 
				"subnet-143eca3f");
		
		VMCreator vmc = new VMCreator(aws);
		
		try {
			vmc.createInstance(VMType.C4_LARGE, VMDiskConfiguration.SSD30, false);
			vmc.createInstance(VMType.C4_XLARGE, VMDiskConfiguration.SSD30, false);
			vmc.createInstance(VMType.M3_LARGE, VMDiskConfiguration.SSD30, false);
			vmc.createInstance(VMType.M3_MEDIUM, VMDiskConfiguration.SSD30, false);
			vmc.createInstance(VMType.T2_MEDIUM, VMDiskConfiguration.SSD30, false);
			vmc.createInstance(VMType.T2_SMALL, VMDiskConfiguration.SSD30, false);
		} catch (VirtualMachineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

}
