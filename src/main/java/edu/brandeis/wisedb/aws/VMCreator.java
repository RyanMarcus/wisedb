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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.model.VolumeType;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;


public class VMCreator {



	private AWSConfiguration config;
	
	public VMCreator(AWSConfiguration config) {
		this.config = config;
	}
	
	private AmazonEC2Client getEC2() {
		AWSCredentials cred = new BasicAWSCredentials(config.getAWSKey(), config.getAWSKeySecret());
		AmazonEC2Client ec2 = new AmazonEC2Client(cred);
		ec2.setEndpoint(config.getEndpoint());	
		return ec2;
	}

	public VM createInstance(VMType type, VMDiskConfiguration disk, boolean waitForRunning) throws VirtualMachineException {
		AmazonEC2Client ec2 = getEC2();

		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

		// TODO: figure out how to change storage type

		String instanceType = "";
		switch (type) {
		case C4_LARGE:
			instanceType = "c4.large";
			break;
		case C4_XLARGE:
			instanceType = "c4.xlarge";
			break;
		case M3_LARGE:
			instanceType = "m3.large";
			break;
		case M3_MEDIUM:
			instanceType = "m3.medium";
			break;
		case T2_MEDIUM:
			instanceType = "t2.medium";
			break;
		case T2_SMALL:
			instanceType = "t2.small";
			break;
		default:
			break;
		
		}
		
		
		BlockDeviceMapping bdm = null;
		switch (disk) {
		case HD100:
			bdm = new BlockDeviceMapping()
				.withDeviceName("/dev/sda1")
				.withEbs(new EbsBlockDevice().withVolumeSize(100)
											 .withVolumeType(VolumeType.Standard)
											 .withDeleteOnTermination(true)
											 .withSnapshotId(config.getSnapshotID()));
											 
		case SSD10:
			bdm = new BlockDeviceMapping()
			.withDeviceName("/dev/sda1")
			.withEbs(new EbsBlockDevice().withVolumeSize(10)
										 .withVolumeType(VolumeType.Gp2)
										 .withDeleteOnTermination(true)
										 .withSnapshotId(config.getSnapshotID()));
		case SSD30:
			bdm = new BlockDeviceMapping()
			.withDeviceName("/dev/sda1")
			.withEbs(new EbsBlockDevice().withVolumeSize(30)
										 .withVolumeType(VolumeType.Gp2)
										 .withDeleteOnTermination(true)
										 .withSnapshotId(config.getSnapshotID()));
		default:
			break;
		
		}

		System.out.println(instanceType);
		runInstancesRequest = runInstancesRequest.withImageId(config.getAMIID())
				.withInstanceType(instanceType)
				.withMinCount(1)
				.withMaxCount(1)
				.withKeyName(config.getKeyPairName())
				.withSubnetId(config.getSubnet())
				.withBlockDeviceMappings(bdm);

		
			



		RunInstancesResult rir = ec2.runInstances(runInstancesRequest);

		String instanceID = rir.getReservation().getInstances().get(0).getInstanceId();
		String ip;

		if (waitForRunning) {
			int maxTry = 60;
			while (true) {
				try {
					DescribeInstancesResult dir = ec2.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceID));
					InstanceState is = dir.getReservations().get(0).getInstances().get(0).getState();
					//System.out.println("Got state: " + is);

					// apparently this constant isn't stored anywhere... *sigh*
					if (is.getCode() == 16) {
						ip = dir.getReservations().get(0).getInstances().get(0).getPublicIpAddress();
						break;
					}
				} catch (AmazonServiceException e) {
					//System.err.println("Trouble with AWS: " + e.getMessage());
				}
				maxTry--;

				if (maxTry == 0) {
					throw new VirtualMachineException("machine never entered running state");
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {

				}
			}
			VM toR = new VM(instanceID, ip, this);
			return toR;
		}
		
		VM toR = new VM(instanceID, null, this);
		return toR;
	}


	public void confirmSSH(VM vm) throws VirtualMachineException {
		// once the machine is running, check to make sure that SSH is available 
		JSch jsch = new JSch();
		Session ssh = null;

		while (true) {
			try {
				jsch.addIdentity(config.getPrivateKeyPath().toString());
				ssh = jsch.getSession("ec2-user", vm.ip, 22);

				ssh.setUserInfo(new UserInfo() {
					public String getPassphrase() {	return null; }
					public String getPassword() { return null; }
					public boolean promptPassphrase(String arg0) { return false; }
					public boolean promptPassword(String arg0) { return false; }
					public boolean promptYesNo(String arg0) { return true; }
					public void showMessage(String arg0) { }
				});

				ssh.connect(120000);
				ssh.disconnect();
				break;
			} catch (JSchException e) {

				if (ssh != null) {
					ssh.disconnect();
				}

				throw new VirtualMachineException("Machine did not reply to SSH");
			}
		}
	}

	public void terminateVM(VM vm) throws VirtualMachineException {
		TerminateInstancesResult tir = getEC2().terminateInstances(new TerminateInstancesRequest().withInstanceIds(vm.id));
		if (!tir.getTerminatingInstances().get(0).getInstanceId().equals(vm.id)) {
			throw new VirtualMachineException("termination failed. the VM id was not included in the response");
		}
		
		int code = tir.getTerminatingInstances().get(0).getCurrentState().getCode();
		
		// these constants aren't given anywhere... the first is
		// shutting-down, the second is terminated.
		if (code != 32 && code != 48) {
			throw new VirtualMachineException("termination failed.");
		}
		
		
	}

	
	public VMCreationTimeProfile testTime(VMType type, VMDiskConfiguration disk) throws VirtualMachineException {
		VMCreationTimeProfile vmctp = new VMCreationTimeProfile();
		vmctp.type = type;
		vmctp.disk = disk;
		VM vm = null;
		
		vmctp.startedAt = System.currentTimeMillis();
		try {
			vm = createInstance(type, disk, true);
			vmctp.readyReportedAt = System.currentTimeMillis();
			confirmSSH(vm);
			vmctp.sshConnectedAt = System.currentTimeMillis();
		} catch (VirtualMachineException e) {
			if (vm != null)
				vm.foriciblyTerminateVM();
			throw e;
		}
		
		
		if (vm != null)
			terminateVM(vm);
		
		return vmctp;
	}


}
