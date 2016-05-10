package org.usfirst.frc.team1736.robot;

import edu.wpi.first.wpilibj.PowerDistributionPanel;

public class CIMCurrentEstimator {
	
	PowerDistributionPanel pdp;
	
	//Known motor constants - from VEX
	private static final double stallCurrent_A = 131.0;
	private static final double operatingVoltage = 12.0;
	private static final double freewheelSpeed_RadperSec = 5330 * 0.1049;
	private static final double freewheelCurrent_A = 2.7;
	
	//Guessed Constants
	private static final double motorWiringResistance = 0.051; //total guess to make the numbers work? assumed 0.006 for wires and 0.005 for controller
	
	//Derived motor constants
	private static final double ESR = operatingVoltage/stallCurrent_A + motorWiringResistance;
	private static final double Ki = (operatingVoltage - freewheelCurrent_A*ESR)/freewheelSpeed_RadperSec;
	
	//configurable constants
	int numMotorsInSystem; //Number of motors driving this system
	double contVDrop;
	
	
	/**
	 * init - Sets up the current estimator with the system parameters.
	 * Input - 
     *      numMotors = Integer number of motors in the gearbox system. Usually 2 or 3, depending on your setup.
     *      motorEncRatio = ratio of motor gear teeth divided by encoder gear teeth. A number smaller than one means the motor spins slower than the encoder.
     *      controllerVDrop_V = voltage drop induced by the motor controller, in V. 
	 */
	public CIMCurrentEstimator(int numMotors, double controllerVDrop_V, PowerDistributionPanel pdp) {
		this.numMotorsInSystem = numMotors;
		this.contVDrop = controllerVDrop_V;
		this.pdp = pdp;
	}
	
	/**
	 * getCurrentEstimate - Determines a unique file name, and opens a file in the data captures directory
	 *        and writes the initial lines to it. 
	 * Input - 
	 */
	public double getCurrentEstimate(double motorSpeed_radpersec, double motorCommand) {
		if(motorCommand > 0.05)
			return Math.max(((double)numMotorsInSystem)*(((pdp.getVoltage()-contVDrop)*motorCommand)-Ki*motorSpeed_radpersec)/ESR, 0);
		else if(motorCommand < -0.05)
			return -Math.min(((double)numMotorsInSystem)*(((pdp.getVoltage()-contVDrop)*motorCommand)-Ki*motorSpeed_radpersec)/ESR, 0);
		else
			return 0.0;
	}

}
