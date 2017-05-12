package org.usfirst.frc.team1736.lib.BatteryParam;

import java.util.Arrays;
import org.usfirst.frc.team1736.lib.SignalMath.AveragingFilter;


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
 * Battery parameter estimator - calculates an estimate for a battery's open circuit voltage (Voc)
 * and equivilant series resistance (ESR) based on a windows of system current/voltage measurements.
 * Ensures enough spread in the measurement window to ensure confidence in the estimate. Based on a
 * whitepaper detailing an algorithm developed for the 2016 FRC season by FRC1736 RobotCasserole.
 * <br>
 * The constants for the initial parameter estimate were tuned assuming a freshly charged,
 * relatively new MK ES17-12 battery (from andymark). It should self-adjust to whatever battery you
 * put in. However, if you seen a giant jump at the start of each match, you may wish to update the
 * "*init" constants to more accurately represent your "nominal" initial system state. <br>
 * <br>
 * Whitepaper link: <a href="../../../../../../../../doc/current_limiting.pdf">Current
 * Estimation</a>. <br>
 * <br>
 * USAGE:
 * <ol>
 * <li>Instantiate Class</li>
 * <li>Once per periodic loop, call the updateEstimate() method to calculate a new paramater
 * estimate</li>
 * <li>After updateEstimate(), call the other getters to utilize the estimated parameters.</li>
 * </ol>
 * 
 * 
 */
public class BatteryParamEstimator {

    // Adjust these to make your initial estimation better
    final double VocEstInit = 13;
    final double EsrEstInit = 0.025;
    final double IdrawInit = 0.025;

    double VocEst = VocEstInit;
    double ESREst = EsrEstInit;
    boolean confident = false;
    double min_spread_thresh = 7.0;
    double prev_best_spread = 0;
    double prev_best_esr = EsrEstInit;

    int lms_window_length;
    double[] circ_buf_SysCurDraw_A;
    double[] circ_buf_SysVoltage_V;
    int index;

    AveragingFilter input_V_filt;
    AveragingFilter input_I_filt;
    AveragingFilter esr_output_filt;


    /**
     * Initalizes all internal variables for using the class.
     * 
     * @param length Window size to consider for estimation. 100 is usually a good start. Smaller
     *        windows make the estimator react faster to rapid changes in the electrical system, but
     *        larger windows make it more immune to noise. Engineering tradeoff, you pick!
     */
    public BatteryParamEstimator(int length) {
        lms_window_length = length;
        index = 0;
        circ_buf_SysCurDraw_A = new double[lms_window_length];
        circ_buf_SysVoltage_V = new double[lms_window_length];
        Arrays.fill(circ_buf_SysCurDraw_A, IdrawInit);
        Arrays.fill(circ_buf_SysVoltage_V, VocEstInit);
        input_V_filt = new AveragingFilter(5, VocEstInit);
        input_I_filt = new AveragingFilter(5, 0.0);
        esr_output_filt = new AveragingFilter(20, EsrEstInit);

    }


    public void setConfidenceThresh(double Thresh_A) {
        min_spread_thresh = Thresh_A;
    }


    /**
     * Update the internal estimates with a new measured system voltage and current. Should be
     * called once per control loop.
     * 
     * @param measSysVoltage_V Battery voltage as measured by PDB
     * @param measSysCurrent_A Current draw as measured by PDB
     */
    public void updateEstimate(double measSysVoltage_V, double measSysCurrent_A) {

        // Update buffers with new inputs (filtered)
        circ_buf_SysCurDraw_A[index] = input_I_filt.filter(measSysCurrent_A);
        circ_buf_SysVoltage_V[index] = input_V_filt.filter(measSysVoltage_V);
        index = (index + 1) % lms_window_length;

        // Perform Least Mean Squares estimation utilizing algorithm
        // outlined at http://faculty.cs.niu.edu/~hutchins/csci230/best-fit.htm
        double sumV = findSum(circ_buf_SysVoltage_V);
        double sumI = findSum(circ_buf_SysCurDraw_A);
        double sumIV = findDotProd(circ_buf_SysCurDraw_A, circ_buf_SysVoltage_V);
        double sumI2 = findDotProd(circ_buf_SysCurDraw_A, circ_buf_SysCurDraw_A);
        double meanV = sumV / lms_window_length;
        double meanI = sumI / lms_window_length;

        ESREst = -(sumIV - sumI * meanV) / (sumI2 - sumI * meanI);

        // Calculate the spread of the system current drawn
        // The Standard Deviation of the input windodw of points is used.
        double spread_I = findStdDev(circ_buf_SysCurDraw_A);

        // If the spread is above the preset threshold, we will be confident for this window
        if (spread_I > min_spread_thresh) {
            confident = true;
            // Additionally, if this is the best spread we've seen so far,
            // record the spread and ESR values for future use
            if (spread_I > prev_best_spread) {
                prev_best_spread = spread_I;
                prev_best_esr = ESREst;
            }
        } else { // If the spread is too small, we're not confident, and reset previous best spread.
            confident = false;
            prev_best_spread = 0;
        }

        // If we weren't confident in that ESR we just calculated, pick the
        // last known best value instead.
        if (!confident) {
            ESREst = prev_best_esr;
        }

        // Filter the output ESR to prevent drastic spikes
        ESREst = esr_output_filt.filter(ESREst);

        // From the ESR, calculate the open-circuit voltage
        VocEst = meanV + ESREst * meanI;

        return; // nothing to return, params are gotten with other "getter" functions

    }


    /**
     * Getter for ESR. Must be called after estimator is run.
     * 
     * @return the most recent calculated equivalent series resistance
     */
    public double getEstESR() {
        return ESREst;
    }


    /**
     * Getter for Voc. Must be called after estimator is run.
     * 
     * @return the most recent calculated open circuit voltage
     */
    public double getEstVoc() {
        return VocEst;
    }


    /**
     * Getter for Confidence condition met boolean. Must be called after estimator is run.
     * 
     * @return True if the most recent window had enough spread in I_draw for a reasonable estimate,
     *         false if not.
     */
    public boolean getConfidence() {
        return confident;
    }


    /**
     * Given a system current draw, estimate the resulting system voltage given the latest estimated
     * battery parameters.
     * 
     * @param Idraw_A estimated system current draw in Amps
     * @return Estimated Vsys in Volts
     */
    public double getEstVsys(double Idraw_A) {
        return VocEst - Idraw_A * ESREst;
    }


    /**
     * Given a minimum system voltage allowable, estimate the maximum current which may be drawn
     * from the battery at the present time.
     * 
     * @param VsysMin_V minimum desirable system voltage
     * @return The maximum current which the robot may pull in amps.
     */
    public double getMaxIdraw(double VsysMin_V) {
        return (VocEst - VsysMin_V) / ESREst;
    }


    /**
     * Calculates the sum of all elements in an array of doubles
     * 
     * @param input - array to add up.
     * @return sum of all values in the array.
     */
    private double findSum(double[] input) {
        double sum = 0;
        for (double i : input) {
            sum += i;
        }
        return sum;
    }


    /**
     * Calculates the dot product of two equally-sized arrays of doubles A and B. Dot product is
     * found by multiplying each element of A with the corresponding element of B, and adding
     * together the result of each multiplication.
     * 
     * @param A First Array
     * @param B Second Array
     * @return dot product of A and B.
     */
    private double findDotProd(double[] A, double[] B) {
        double sum = 0;
        if (A.length != B.length) {
            System.out.println("Error - A and B vectors are not the same length! Cannot compute dot product.");
            return Double.NaN;
        }

        for (int i = 0; i < A.length; i++) {
            sum += A[i] * B[i];
        }

        return sum;
    }


    /**
     * Calculates the standard deviation of a set of doubles. Standard deviation is defined as the
     * square root of the sum of the squares of the distances of each element from the average of
     * the dataset.
     * 
     * @param input - doubles to take the Standard Deviation of.
     * @return standard deviation of the input set.
     */
    private double findStdDev(double[] input) {
        double avg_input = findSum(input) / input.length;
        double sum = 0;

        for (int i = 0; i < input.length; i++) {
            sum += Math.pow((input[i] - avg_input), 2);
        }
        return Math.sqrt(sum / input.length);
    }
}
