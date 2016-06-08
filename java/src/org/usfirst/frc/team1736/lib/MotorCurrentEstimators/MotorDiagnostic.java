package org.usfirst.frc.team1736.lib.MotorCurrentEstimators;

import org.usfirst.frc.team1736.lib.Util.DaBouncer;

///////////////////////////////////////////////////////////////////////////////
// Copyright (c) FRC Team 1736 2016. See the License file. 
//
// Can you use this code? Sure! We're releasing this under GNUV3, which 
// basically says you can take, modify, share, publish this as much as you
// want, as long as you don't make it closed source.
//
// If you do find it useful, we'd love to hear about it! Check us out at
// http://robotcasserole.org/ and leave us a message!
///////////////////////////////////////////////////////////////////////////////

/**
* DESCRIPTION:
* <br>
* Class for detecting various motor failure modes, empowering the user to take proper action.
* <br>
* <br>
* USAGE:    
* <ol>   
* <li>Instantiate Class</li> 
* <li>Set global tune values away from defaults if desired</li> 
* <li>Call MotorDiagnostic method during periodic loop</li>
* <li>Read diagnostic status variables and take appropriate actions</li>
* </ol>
* 
 */
 
 
public class MotorDiagnostic {
	
	//available diagnostic states
	/**
     * Stalled diagnostic indicates motor should be turning, is not, and is drawing too much current.
     */
	public boolean motorStalled = false;
    /**
     * OverCurrent diagnostic indicates we are commanding the motor to turn, but there is too much current being drawn.
     */
	public boolean motorOverCurrent = false;
    /**
     * SpeedLost diagnostic indicates rotation is commanded, current draw is above minimum and below maximum, but no speed is seen at the sensor.
     */
	public boolean motorSpeedLost = false;
	
    
	/**
     * Minimum speed to declare the motor "rotating"
     */
	public double SpeedThresh_RPM = 50;
	/**
     * Minimum number of loops above the speed threshold to declare the motor "rotating"
     */
	public int SpeedDbnc_loops = 10;
	/**
     * Minimum command to ensure the motor should be rotating
     */
	public double CommandThresh = 0.5;
	/**
     * Minimum number of loops above the command threshold to ensure the motor should be rotating
     */
	public int CommandDbnc_loops = 3;
	/**
     * Maximum current the motor should draw in steady-state
     */
	public double MaxCurrentThresh_A = 50;
	/**
     * Minimum time above current threshold before declaring the motor "over-current"
     */
	public int MaxCurrentDbnc_loops = 5;
    
	/**
     * Minimum current the motor should draw in steady-state
     */
	public double MinCurrentThresh_A = 2;
	
	
	DaBouncer speedDbncr;
	DaBouncer cmdDbncr;
	DaBouncer maxCurrentDbncr;
	
	
	/**
	 * Constructor
	 */
	public MotorDiagnostic(){
		speedDbncr = new DaBouncer();
		speedDbncr.threshold = SpeedThresh_RPM;
		speedDbncr.dbnc = SpeedDbnc_loops;
		
		cmdDbncr = new DaBouncer();
		cmdDbncr.threshold = CommandThresh;
		cmdDbncr.dbnc = CommandDbnc_loops;
		
		maxCurrentDbncr = new DaBouncer();
		maxCurrentDbncr.threshold = MaxCurrentThresh_A;
		maxCurrentDbncr.dbnc = MaxCurrentDbnc_loops;
	}
	
	/**
	 * Call once per periodic loop to determine motor state
	 * @param motorSpeed_RPM The motor's speed in RPM as measured by the encoders
	 * @param currentDraw_A The motor's present current draw (estimated or measured)
	 * @param motorCommand The present motor command (from the joystick or otherise, whatever is sent to the controller)
	 */
	public void eval(double motorSpeed_RPM, double currentDraw_A, double motorCommand){
		boolean speedDbncState = speedDbncr.BelowDebounce(Math.abs(motorSpeed_RPM));
		boolean cmdDbncState = cmdDbncr.AboveDebounce(Math.abs(motorCommand));
		boolean maxCurrentDbncState = maxCurrentDbncr.AboveDebounce(Math.abs(currentDraw_A));
		
		//Stalled occurs when all three are triggered
		//aka, we're not moving and we should be, and the current went too high.
		motorStalled = speedDbncState & cmdDbncState & maxCurrentDbncState;
		
		//OverCurrent happens when current and command trigger
		//AKA, we're commanding to move AND the current is above some threshold
		motorOverCurrent = cmdDbncState & maxCurrentDbncState;
		
		//SpeedLost happens when speed and command trigger but current does not
		//AKA, we should be moving but we're not reading speed, AND current is in a normal range
		motorSpeedLost = speedDbncState & cmdDbncState & !maxCurrentDbncState & (currentDraw_A > MinCurrentThresh_A);
	}
	

}
