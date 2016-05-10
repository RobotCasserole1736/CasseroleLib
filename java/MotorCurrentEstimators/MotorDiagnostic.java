package org.usfirst.frc.team1736.robot;

/**
 * Motor Diagnostics - Detects various conditions with speed controllers
 * and takes mitigating actions
 * @author gerthcm
 *
 */
public class MotorDiagnostic {
	
	//available diagnostic states
	public boolean motorStalled = false;
	public boolean motorOverCurrent = false;
	public boolean motorSpeedLost = false;
	
	//Tune Constants - set outside the class before calling constructor
	public double SpeedThresh_RPM = 50;
	public int SpeedDbnc_loops = 10;
	public double CommandThresh = 0.5;
	public int CommandDbnc_loops = 3;
	public double CurrentThresh_A = 50;
	public int CurrentDbnc_loops = 5;
	
	DaBouncer speedDbncr;
	DaBouncer cmdDbncr;
	DaBouncer currentDbncr;
	
	
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
		
		currentDbncr = new DaBouncer();
		currentDbncr.threshold = CurrentThresh_A;
		currentDbncr.dbnc = CurrentDbnc_loops;
	}
	
	/**
	 * Call once per periodic loop to determine motor state
	 * @param motorSpeed_RPM - the motor's speed in RPM as measured by the encoders
	 * @param currentDraw_A - The motor's present current draw (estimated or measured)
	 * @param motorCommand - The present motor command (from the joystick or otherise, whatever is sent to the controller)
	 */
	public void eval(double motorSpeed_RPM, double currentDraw_A, double motorCommand){
		boolean speedDbncState = speedDbncr.BelowDebounce(Math.abs(motorSpeed_RPM));
		boolean cmdDbncState = cmdDbncr.AboveDebounce(Math.abs(motorCommand));
		boolean currentDbncState = currentDbncr.AboveDebounce(Math.abs(currentDraw_A));
		
		//Stalled occurs when all three are triggered
		//aka, we're not moving and we should be, and the current went too high.
		motorStalled = speedDbncState & cmdDbncState & currentDbncState;
		
		//OverCurrent happens when current and command trigger
		//AKA, we're commanding to move AND the current is above some threshold
		motorOverCurrent = cmdDbncState & currentDbncState;
		
		//SpeedLost happens when speed and command trigger but current does not
		//AKA, we should be moving but we're not reading speed, AND current is in a normal range
		motorSpeedLost = speedDbncState & cmdDbncState & !currentDbncState;
	}
	

}
