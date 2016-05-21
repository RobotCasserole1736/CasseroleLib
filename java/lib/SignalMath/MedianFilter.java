package org.usfirst.frc.team1736.robot;

import java.util.Arrays;

/**
 * Median Filter - Class which defines a simple median filter. After initilization, the "filter()" method 
 * can simply be called with the newest input, and the most recent filtered output will be returned.
 * @author Chris Gerth
 *
 */
public class MedianFilter {
	
	int N; //length of filter 
	double[] circ_buffer; //circular buffer to hold all values
	int index; //"pointer" to the starting index in the buffer
	
	
	/**
	 * init - initialize all things needed for the averaging filter.
	 * @param length
	 * @param init_val
	 */
	public MedianFilter(int length, double init_val){
		index = 0;
		N = length;
		//Allocate buffer
		circ_buffer = new double[N];
		//Fill the buffer with the initial value
		Arrays.fill(circ_buffer, init_val);
	}
	
	/**
	 * Filter - add a new input to the filter and get the current output from the filter
	 * @param input - new value to add to the filter
	 * @return the present output filtered value
	 */
	public double filter(double input){
		double[] temp_buffer = new double[N];
		
	    //put new value into the buffer	
		circ_buffer[index] = input;
		//Update index (circularly since circular buffer)
		index = (index + 1)%N;
		
		//Copy the buffer to a temporary spot
		System.arraycopy(circ_buffer, 0, temp_buffer, 0, circ_buffer.length);
		
		//sort the temporary array
		Arrays.sort(temp_buffer);
		
		//calculate median based on the middle of the sorted array
		int middle = N/2;
		if(N%2==1){ //case - length is odd so a middle point actually exists
			return temp_buffer[middle];
		}
		else{ //case - length is even so the middle is the average of the two middle samples.
			return (temp_buffer[middle-1] + temp_buffer[middle])/2;
		}
		
	}

}
