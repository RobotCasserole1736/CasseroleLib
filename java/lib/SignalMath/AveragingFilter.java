package org.usfirst.frc.team1736.robot;

import java.util.Arrays;

/**
 * Averaging Filter - Class which defines a simple average filter. After initilization, the "filter()" method 
 * can simply be called with the newest input, and the most recent filtered output will be returned.
 * @author Chris Gerth
 *
 */
public class AveragingFilter {
	
	int N; //length of filter 
	double[] circ_buffer; //circular buffer to hold all values
	double sum; //hold sum of all numbers in the buffer at all times.
	int index; //"pointer" to the starting index in the buffer
	
	
	/**
	 * init - initialize all things needed for the averaging filter.
	 * @param length
	 * @param init_val
	 */
	public AveragingFilter(int length, double init_val){
		index = 0;
		N = length;
		//Allocate buffer
		circ_buffer = new double[N];
		//Fill the buffer with the initial value
		Arrays.fill(circ_buffer, init_val);
		//Calculate initial sum
		sum = N*init_val;
	}
	
	/**
	 * Filter - add a new input to the filter and get the current output from the filter
	 * @param input - new value to add to the filter
	 * @return the present output filtered value
	 */
	public double filter(double input){
		//use the running-sum method to compute the average. Better cuz it's O(1) time and O(n) memory.
		//Computing the sum from scratch is O(n) for both.
		sum -= circ_buffer[index];
		
		circ_buffer[index] = input;
		sum += circ_buffer[index];
		//Update index (circularly due to buffer)
		index = (index + 1)%N;
		//Return average = sum/length
		return sum/N;
	}

}
