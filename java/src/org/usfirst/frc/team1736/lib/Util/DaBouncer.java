package org.usfirst.frc.team1736.lib.Util;

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

/**
 * DESCRIPTION: <br>
 * Threshold and Debounce library for signal conditioning USAGE:
 * <ol>
 * <li>Instantiate class</li>
 * <li>Set Threshold and dbnc (time) globals</li>
 * <li>Call *Debounce() method once per loop with each new signal value to debounce the signal</li>
 * </ol>
 * 
 * @author Alex Stevenson (2016)
 *
 */
public class DaBouncer {

    /** Debouncer threshold (for "above" or "below" calculation) */
    public double threshold = 0; // The point at which the the value of a variable should be less
                                 // than
    /** Debounce time (in loops) */
    public int dbnc = 5; // The amount of time the value of a variable is over the threshold

    double DebounceCounter;


    /**
     * Constsructor. Set the threshold and debounce by setting the "threshold" and "dbnc" class
     * variables
     */
    public DaBouncer() {
        DebounceCounter = 0;
    }


    /**
     * Call once per periodic loop. Counts samples which are above the threshold Outputs true if the
     * signal has been above the threshold for the debounce duration, false otherwise Output goes
     * false as soon as the signal drops below the threshold.
     * 
     * @param input Signal to debounce
     * @return True if timer expired & still above threshold, false if not.
     */
    public boolean AboveDebounce(double input) {
        if (input > threshold) {
            DebounceCounter++;
        } else {
            DebounceCounter = 0;
        }

        if (DebounceCounter > dbnc) {
            return true;
        } else {
            return false;
        }
    }
    
    public boolean AboveDebounceBoo(boolean b) {
    	if (b = true){
    		DebounceCounter++;	
    	}
    	else {
    		DebounceCounter = 0;
    	}
    	
    	if (DebounceCounter > dbnc) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Call once per periodic loop. Counts samples which are below the threshold Outputs true if the
     * signal has been below the threshold for the debounce duration, false otherwise Output goes
     * false as soon as the signal rises above the threshold.
     * 
     * @param input Signal to debounce
     * @return True if timer expired & still below threshold, false if not.
     */
    public boolean BelowDebounce(double input) {
        if (input < threshold) {
            DebounceCounter++;
        } else {
            DebounceCounter = 0;
        }

        if (DebounceCounter > dbnc) {
            return true;
        } else {
            return false;
        }
    }
}
