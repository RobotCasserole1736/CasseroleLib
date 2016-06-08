package org.usfirst.frc.team1736.lib.SignalMath;

import java.util.Arrays;

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
 * Class which defines a simple median filter. After initialization, the "filter()" method 
 * can simply be called with the newest input, and the most recent filtered output will be returned.
 * Median filters are useful for rejecting noise which induces large errors for single loops ("shot noise")
 *
 */
public class MedianFilter {
	
	int N; //length of filter 
	double[] circ_buffer; //circular buffer to hold all values
	int index; //"pointer" to the starting index in the buffer
	
	
	/**
	 * Initialize all things needed for the median filter.
	 * @param length Number of previous values to consider for the median calculation. 
     *               5 is probably a good starting point. Bigger length means more noise rejection,
     *               but also induces more delay from input to output. Also, median calculations are
     *               non-linear in big-O time complexity, so cranking this number up will <i>really</i>
     *               start to slow things down.
	 * @param init_val Initial output of the filter. Usually zero, unless you know something about your system that we don't.
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
	 * Add a new input to the filter and get the current output from the filter
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
