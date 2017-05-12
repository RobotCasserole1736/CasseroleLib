package org.usfirst.frc.team1736.lib.SignalMath;

/*
 *******************************************************************************************
 * Copyright (C) 2017 FRC Team 1736 Robot Casserole - www.robotcasserole.org
 *******************************************************************************************
 *
 * This software is released under the MIT Licence - see the license.txt
 *  file in the root of this repo.
 *
 * Non-legally-binding statement from Team 1736:
 *  Thank you for taking the time to read through our software! We hope you
 *   find it educational and informative! 
 *  Please feel free to snag our software for your own use in whatever project
 *   you have going on right now! We'd love to be able to help out! Shoot us 
 *   any questions you may have, all our contact info should be on our website
 *   (listed above).
 *  If you happen to end up using our software to make money, that is wonderful!
 *   Robot Casserole is always looking for more sponsors, so we'd be very appreciative
 *   if you would consider donating to our club to help further STEM education.
 */

import java.util.Arrays;

/**
 * Class which defines a simple median filter. After initialization, the "filter()" method can
 * simply be called with the newest input, and the most recent filtered output will be returned.
 * Median filters are useful for rejecting noise which induces large errors for single loops (
 * "shot noise")
 *
 */
public class MedianFilter {

    int N; // length of filter
    double[] circ_buffer; // circular buffer to hold all values
    int index; // "pointer" to the starting index in the buffer


    /**
     * Initialize all things needed for the median filter.
     * 
     * @param length Number of previous values to consider for the median calculation. 5 is probably
     *        a good starting point. Bigger length means more noise rejection, but also induces more
     *        delay from input to output. Also, median calculations are non-linear in big-O time
     *        complexity, so cranking this number up will <i>really</i> start to slow things down.
     * @param init_val Initial output of the filter. Usually zero, unless you know something about
     *        your system that we don't.
     */
    public MedianFilter(int length, double init_val) {
        index = 0;
        N = length;
        // Allocate buffer
        circ_buffer = new double[N];
        // Fill the buffer with the initial value
        Arrays.fill(circ_buffer, init_val);
    }


    /**
     * Add a new input to the filter and get the current output from the filter
     * 
     * @param input - new value to add to the filter
     * @return the present output filtered value
     */
    public double filter(double input) {
        double[] temp_buffer = new double[N];

        // put new value into the buffer
        circ_buffer[index] = input;
        // Update index (circularly since circular buffer)
        index = (index + 1) % N;

        // Copy the buffer to a temporary spot
        System.arraycopy(circ_buffer, 0, temp_buffer, 0, circ_buffer.length);

        // sort the temporary array
        Arrays.sort(temp_buffer);

        // calculate median based on the middle of the sorted array
        int middle = N / 2;
        if (N % 2 == 1) { // case - length is odd so a middle point actually exists
            return temp_buffer[middle];
        } else { // case - length is even so the middle is the average of the two middle samples.
            return (temp_buffer[middle - 1] + temp_buffer[middle]) / 2;
        }

    }

}
