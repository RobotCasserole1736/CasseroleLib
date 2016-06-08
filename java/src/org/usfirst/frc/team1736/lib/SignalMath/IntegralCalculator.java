package org.usfirst.frc.team1736.lib.SignalMath;

import edu.wpi.first.wpilibj.Timer;

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
 * Class which implements a simple discrete-time integral calculation. Output is equal
 * to the accumulation of the input signal in units of <signal> * second. 
 *
 * There are many ways to take an integral in discrete time, this method in the
 * future will support many of them.
 */

public class IntegralCalculator {
	private double prev_time;
	private double accumulator;
	
	/**
	 * Constructor for integral calculator. Note that you should instantiate one of these classes per signal we wish to take the integral of.
	 */
	public IntegralCalculator(){
		prev_time = Timer.getFPGATimestamp();
		accumulator = 0;
	}
	
	/**
	 * Given a new input value, output its integral based on the last time the
	 * method was invoked. The idea is you would read a signal, then call this method
     * with this new signal value, so you have the integral right there. Then use it 
     * later on. Presently, this method only supports a simple "sample & hold" integral
     * calculation where the signal is assumed constant between the previous sample and this
     * sample. In the future, cubic-spline methods will be supported
     *
	 * @param in input to take the integral of
	 * @return the integral of the signal "in" in units of in * sec.
	 */
	public double calcIntegral(double in){
		double cur_time = Timer.getFPGATimestamp();
		/*Integration method - "Sample and hold"*/
		/*TBD - other methods...*/
		accumulator += in * (cur_time - prev_time);
		
		/*Save values for next loop and return*/
		prev_time = cur_time;
		return accumulator;
	}
	
	/**
	 * Reset the integration value back to zero immediately. Useful for
     * returning the accumulator back to an initial state.
     */
	public void resetIntegral(){
		accumulator = 0;
	}

}
