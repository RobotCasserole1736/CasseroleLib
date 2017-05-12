package org.usfirst.frc.team1736.lib.MotorCurrentEstimators;

import edu.wpi.first.wpilibj.PowerDistributionPanel;

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
 * Current draw estimator for full-sized (2.5") CIM motor plus nominal wiring. Tweaked around
 * FRC1736's 2016 robot. Given a applied voltage and speed, the current draw is estimated. This is
 * useful for determining if a driver command will draw too much current. Treats a group of motors
 * as a single current-drawing device (useful for single side of drivetrain where all motors run the
 * same speed). Estimation is based off of a whitepaper by FRC1736 from the 2016 competition season.
 * <br>
 * <br>
 * Whitepaper link: <a href="../../../../../../../../doc/current_limiting.pdf">Current
 * Estimation</a>. <br>
 * <br>
 * USAGE:
 * <ol>
 * <li>Instantiate Class with proper values</li>
 * <li>Call estimator function during periodic loop to estimate motor current draw</li>
 * </ol>
 * 
 * 
 */


public class CIMCurrentEstimator {

    PowerDistributionPanel pdp;

    // Known motor constants - from VEX
    private static final double stallCurrent_A = 131.0;
    private static final double operatingVoltage = 12.0;
    private static final double freewheelSpeed_RadperSec = 5330 * 0.1049;
    private static final double freewheelCurrent_A = 2.7;

    // Guessed Constants
    private static final double motorWiringResistance = 0.051; // tweaked around empirical estimates
                                                               // from 2016 robot

    // Derived motor constants
    private static final double ESR = operatingVoltage / stallCurrent_A + motorWiringResistance;
    private static final double Ki = (operatingVoltage - freewheelCurrent_A * ESR) / freewheelSpeed_RadperSec;

    // configurable constants
    int numMotorsInSystem; // Number of motors driving this system
    double contVDrop;


    /**
     * Sets up the current estimator with the system parameters.
     * 
     * @param numMotors Integer number of motors in the gearbox system. Usually 2 or 3 for a side of
     *        a drivetrain, or 1 for a single motor.
     * @param controllerVDrop_V voltage drop induced by the motor controller, in V.
     * @param pdp Instance of the PDP class from the top level (needed for voltage measurements)
     */
    public CIMCurrentEstimator(int numMotors, double controllerVDrop_V, PowerDistributionPanel pdp) {
        this.numMotorsInSystem = numMotors;
        this.contVDrop = controllerVDrop_V;
        this.pdp = pdp;
    }


    /**
     * Determines a unique file name, and opens a file in the data captures directory and writes the
     * initial lines to it.
     * 
     * @param motorSpeed_radpersec The speed of the motor input shaft in radians per second.
     *        Measured from an encoder attached to the same mechanism as the motor(s).
     * @param motorCommand The command sent to the motor (-1 to 1 range).
     * @return The current (in amps) drawn from the motor under the speed/voltage conditions given
     *         at the input.
     */
    public double getCurrentEstimate(double motorSpeed_radpersec, double motorCommand) {
        if (motorCommand > 0.05)
            return Math.max(((double) numMotorsInSystem)
                    * (((pdp.getVoltage() - contVDrop) * motorCommand) - Ki * motorSpeed_radpersec) / ESR, 0);
        else if (motorCommand < -0.05)
            return -Math.min(((double) numMotorsInSystem)
                    * (((pdp.getVoltage() - contVDrop) * motorCommand) - Ki * motorSpeed_radpersec) / ESR, 0);
        else
            return 0.0;
    }

}
