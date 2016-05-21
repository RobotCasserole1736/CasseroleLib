/**
 * 
 */
package org.usfirst.frc.team1736.robot;

import edu.wpi.first.wpilibj.Timer;

/**
 * @author gerthcm
 *
 */
public class DerivativeCalculator {
	private double prev_time;
	private double prev_signal_val;
	
	/**
	 * Construction - simple class to calculate the derivative of a signal
	 * Accounts for the fact sample time isn't really constant...
	 * Note we should have one of these classes per signal we wish to take the derivative of.
	 */
	DerivativeCalculator(){
		prev_time = Timer.getFPGATimestamp();
		prev_signal_val = 0;
	}
	
	/**
	 * calcDeriv - given an input value, output its derivative based on the last time the
	 * method was invoked
	 * @param in - input to take the derivative of
	 * @return the derivative of the signal "in" in units of in/sec.
	 */
	public double calcDeriv(double in){
		double cur_time = Timer.getFPGATimestamp();
		double output = (in - prev_signal_val)/(cur_time - prev_time);
		prev_time = cur_time;
		prev_signal_val = in;
		return output;
		
	}

}
