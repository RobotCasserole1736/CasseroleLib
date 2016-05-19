package org.usfirst.frc.team1736.robot;

import edu.wpi.first.wpilibj.Timer;

public class IntegralCalculator {
	private double prev_time;
	private double accumulator;
	
	IntegralCalculator(){
		prev_time = Timer.getFPGATimestamp();
		accumulator = 0;
	}
	
	public double calcIntegral(double in){
		double cur_time = Timer.getFPGATimestamp();
		/*Integration method - "Sample and hold"*/
		/*TBD - other methods...*/
		accumulator += in * (cur_time - prev_time);
		
		/*Save values for next loop and return*/
		prev_time = cur_time;
		return accumulator;
	}
	
	public void resetIntegral(){
		accumulator = 0;
	}

}
