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

import java.util.Timer;
import java.util.TimerTask;
import edu.wpi.first.wpilibj.CircularBuffer;

/**
 * Generic Finite Impluse Response (FIR) filter. Constructor takes an argument to indicate what sort
 * of filter to implement. Underlying logic remains the same for all filters, but coefficent value
 * and number change to implement different cutoff frequencies and styles. <br>
 * <br>
 * A FIR filter is effectively a weighted average of past signal values, but with specifically
 * chosen weights to allow or disallow specific frequencies through. Lowpass filters allow low
 * frequencies but block high frequencies, while Highpass filters allow higher frequencies but block
 * lower ones. The number indicates the aproximate frequency at which the filter transitions between
 * blocking and not blocking. <br>
 * <br>
 * FIR filters must process samples at a very regular rate to maintain accuracy. For this reason,
 * this implementation outsources the computation of filter value to a separate task, called at a
 * 20ms rate (50Hz). This limits the max. frequency that can be processed by the filter to 25Hz (per
 * the Nyquist Criteria).
 *
 *
 */
public class FIRFilter {

    /**
     * Type of filtering this filter is doing. If for some reason you forgot what type it was, check
     * it here
     */
    public final FilterType type;


    private double[] coefs;

    private CircularBuffer sampleBuffer;

    private double present_in_val;
    private double present_out_val;

    private final int length;

    private final int m_sample_period_ms = 20;

    Timer timerThread;


    /**
     * Initalize memory for the filter, and start periodic calculations in the background.
     * 
     * @param type_in Type of filter to execute
     */
    public FIRFilter(FilterType type_in) {
        type = type_in;

        switch (type) {
            case LOWPASS_2HZ:
                coefs = FilterCoefs.lowpass2HzCoef;
                break;
            case LOWPASS_5HZ:
                coefs = FilterCoefs.lowpass5HzCoef;
                break;
            case LOWPASS_15HZ:
                coefs = FilterCoefs.lowpass15HzCoef;
                break;
            case HIGHPASS_2HZ:
                coefs = FilterCoefs.highpass2HzCoef;
                break;
            case HIGHPASS_5HZ:
                coefs = FilterCoefs.highpass5HzCoef;
                break;
            default: // Definitely should never get here, cuz enum
                coefs = new double[0]; // the most boring filter of them all.
                break;
        }

        length = coefs.length;
        sampleBuffer = new CircularBuffer(length);

        sampleBuffer.reset();

        // Kick off the multi-threaded stuff.
        // Will start calling the periodic update function at an interval of m_sample_period_ms,
        // asynchronously from any other code.
        // Java magic here, don't touch!
        timerThread = new java.util.Timer("Casserole FIR Filter Update");
        timerThread.schedule(new FilterTask(this), 0L, (long) (m_sample_period_ms));

    }


    /**
     * Set a new input into the filter. Whatever is passed here will be picked up next time the
     * filter task calculates the filter's value.
     * 
     * @param val Input to the filter
     */
    public void setInput(double val) {
        present_in_val = val;
    }


    /**
     * Get the present output of the filter.
     * 
     * @return Filter's present output value
     */
    public double getFilterOutput() {
        return present_out_val;
    }


    /**
     * FIR filter accuracy is highly dependent upon inputs being processed at a regular rate. In
     * general our code kinda does that, but to absolutely ensure it, we'll fire off the
     * calculations in a background task. Also helps the scheduler be able to do some calculations
     * asynchronously.
     */
    private void periodic_update() {
        double local_result = 0;

        sampleBuffer.pushFront(present_in_val);

        for (int i = 0; i < length; i++) {
            local_result += sampleBuffer.get(i) * coefs[i];
        }

        present_out_val = local_result;
    }



    // Java multithreading magic. Do not touch.
    // Touching will incour the wrath of Cthulhu, god of java and filters.
    // May the oceans of 1's and 0's rise to praise him.
    private class FilterTask extends TimerTask {
        private FIRFilter m_filter;


        public FilterTask(FIRFilter filter) {
            if (filter == null) {
                throw new NullPointerException("Given Filter was null");
            }
            m_filter = filter;
        }


        @Override
        public void run() {
            m_filter.periodic_update();
        }
    }

}


