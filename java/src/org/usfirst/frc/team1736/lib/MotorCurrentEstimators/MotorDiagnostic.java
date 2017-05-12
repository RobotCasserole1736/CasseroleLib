package org.usfirst.frc.team1736.lib.MotorCurrentEstimators;

import org.usfirst.frc.team1736.lib.Util.DaBouncer;

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
 * Class for detecting various motor failure modes, empowering the user to take proper action. <br>
 * <br>
 * USAGE:
 * <ol>
 * <li>Instantiate Class</li>
 * <li>Set global tune values away from defaults if desired</li>
 * <li>Call MotorDiagnostic method during periodic loop</li>
 * <li>Read diagnostic status variables and take appropriate actions</li>
 * </ol>
 * 
 */


public class MotorDiagnostic {

    // available diagnostic states
    /**
     * Stalled diagnostic indicates motor should be turning, is not, and is drawing too much
     * current.
     */
    public boolean motorStalled = false;
    /**
     * OverCurrent diagnostic indicates we are commanding the motor to turn, but there is too much
     * current being drawn.
     */
    public boolean motorOverCurrent = false;
    /**
     * SpeedLost diagnostic indicates rotation is commanded, current draw is above minimum and below
     * maximum, but no speed is seen at the sensor.
     */
    public boolean motorSpeedLost = false;


    /**
     * Minimum speed to declare the motor "rotating"
     */
    public double SpeedThresh_RPM = 50;
    /**
     * Minimum number of loops above the speed threshold to declare the motor "rotating"
     */
    public int SpeedDbnc_loops = 10;
    /**
     * Minimum command to ensure the motor should be rotating
     */
    public double CommandThresh = 0.5;
    /**
     * Minimum number of loops above the command threshold to ensure the motor should be rotating
     */
    public int CommandDbnc_loops = 3;
    /**
     * Maximum current the motor should draw in steady-state
     */
    public double MaxCurrentThresh_A = 50;
    /**
     * Minimum time above current threshold before declaring the motor "over-current"
     */
    public int MaxCurrentDbnc_loops = 5;

    /**
     * Minimum current the motor should draw in steady-state
     */
    public double MinCurrentThresh_A = 2;


    DaBouncer speedDbncr;
    DaBouncer cmdDbncr;
    DaBouncer maxCurrentDbncr;


    /**
     * Constructor
     */
    public MotorDiagnostic() {
        speedDbncr = new DaBouncer();
        speedDbncr.threshold = SpeedThresh_RPM;
        speedDbncr.dbnc = SpeedDbnc_loops;

        cmdDbncr = new DaBouncer();
        cmdDbncr.threshold = CommandThresh;
        cmdDbncr.dbnc = CommandDbnc_loops;

        maxCurrentDbncr = new DaBouncer();
        maxCurrentDbncr.threshold = MaxCurrentThresh_A;
        maxCurrentDbncr.dbnc = MaxCurrentDbnc_loops;
    }


    /**
     * Call once per periodic loop to determine motor state
     * 
     * @param motorSpeed_RPM The motor's speed in RPM as measured by the encoders
     * @param currentDraw_A The motor's present current draw (estimated or measured)
     * @param motorCommand The present motor command (from the joystick or otherise, whatever is
     *        sent to the controller)
     */
    public void eval(double motorSpeed_RPM, double currentDraw_A, double motorCommand) {
        boolean speedDbncState = speedDbncr.BelowDebounce(Math.abs(motorSpeed_RPM));
        boolean cmdDbncState = cmdDbncr.AboveDebounce(Math.abs(motorCommand));
        boolean maxCurrentDbncState = maxCurrentDbncr.AboveDebounce(Math.abs(currentDraw_A));

        // Stalled occurs when all three are triggered
        // aka, we're not moving and we should be, and the current went too high.
        motorStalled = speedDbncState & cmdDbncState & maxCurrentDbncState;

        // OverCurrent happens when current and command trigger
        // AKA, we're commanding to move AND the current is above some threshold
        motorOverCurrent = cmdDbncState & maxCurrentDbncState;

        // SpeedLost happens when speed and command trigger but current does not
        // AKA, we should be moving but we're not reading speed, AND current is in a normal range
        motorSpeedLost = speedDbncState & cmdDbncState & !maxCurrentDbncState & (currentDraw_A > MinCurrentThresh_A);
    }


}
