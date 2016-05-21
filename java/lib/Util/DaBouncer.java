package org.usfirst.frc.team1736.robot;

/**
 * DaBouncer - Threshold and Debounce library
 * @author Alex Stevenson (2016)
 *
 */
public class DaBouncer {

	public double threshold; //The point at which the the value of a variable should be less than
	public int dbnc; //The amount of time the value of a variable is over the threshold
	
	double DebounceCounter;
	
	/**
	 * Constsructor. Set the threshold and debounce by setting 
	 * the "threshold" and "dbnc" class variables
	 */
	public DaBouncer(){
		DebounceCounter = 0;
	}
	
	/**
	 * Call once per periodic loop. Counts samples which are above the threshold
	 * Outputs true if the signal has been above the threshold for the debounce duration, false otherwise
	 * Output goes false as soon as the signal drops below the threshold.
	 * @param input
	 * @return
	 */
	public boolean AboveDebounce(double input){
		if (input > threshold){
			DebounceCounter++;
		}
		else { 
			DebounceCounter = 0;
		}
		
		if (DebounceCounter > dbnc){
			return true;
		}
		else {
			return false;
		}
	}
		
	/**
	 * Call once per periodic loop. Counts samples which are below the threshold
	 * Outputs true if the signal has been below the threshold for the debounce duration, false otherwise
	 * Output goes false as soon as the signal rises above the threshold.
	 * @param input
	 * @return
	 */
	public boolean BelowDebounce(double input){
		if (input < threshold){
			DebounceCounter++;
		}
		else { 
			DebounceCounter = 0;
		}
		
		if (DebounceCounter > dbnc){
			return true;
		}
		else {
			return false;
		}
	}
}