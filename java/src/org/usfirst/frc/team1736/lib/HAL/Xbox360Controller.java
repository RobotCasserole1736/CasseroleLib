package org.usfirst.frc.team1736.lib.HAL;

import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.Joystick;


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
 * This class was created purely in an effort to make controller calls less clunky overall. Not
 * terribly useful, but hopefully way easier than typing out a super long string every time you want
 * to call a controller button. <br>
 * I don't want to put "get" in front of my methods because that'll make the calls ever so slightly
 * longer, so I won't. As Chris says, "If later we don't like it, we'll change it." <br>
 * <br>
 * USAGE:
 * <ol>
 * <li>Instantiate Class</li>
 * <li>Call methods to read from controller.</li>
 * </ol>
 * 
 * 
 */

public class Xbox360Controller {


    // Xbox Controller 2015 (Zero Based (I think)) Constants
    // -Controller Buttons
    final static int XBOX_A_BUTTON = 1;
    final static int XBOX_B_BUTTON = 2;
    final static int XBOX_X_BUTTON = 3;
    final static int XBOX_Y_BUTTON = 4;
    final static int XBOX_LEFT_BUTTON = 5;
    final static int XBOX_RIGHT_BUTTON = 6;
    final static int XBOX_BACK_BUTTON = 7;
    final static int XBOX_START_BUTTON = 8;
    final static int XBOX_LSTICK_BUTTON = 9;
    final static int XBOX_RSTICK_BUTTON = 10;

    // -Controller Axes
    final static int XBOX_LSTICK_XAXIS = 0;
    final static int XBOX_LSTICK_YAXIS = 1;
    final static int XBOX_LTRIGGER_AXIS = 2;
    final static int XBOX_RTRIGGER_AXIS = 3;
    final static int XBOX_RSTICK_XAXIS = 4;
    final static int XBOX_RSTICK_YAXIS = 5;

    // -Controller D-Pad POV Hat
    final static int XBOX_DPAD_POV = 0;

    private double deadzone = 0;
    
    private Joystick joystick; 


    /**
     * Set up a new joystick
     * 
     * @param joystick_id The index to pass to WPIlib for the joystick. Usually 0 is driver1, 1 is
     *        driver2, and so on
     */
    public Xbox360Controller(int joystick_id) {
        joystick = new Joystick(joystick_id);
    }
    
    /**
     * Update the deadzone for all joystick axes
     * 
     * @param _deadzone minimum non-zero output to allow from the joystick
     */
    public void setDeadzone(double _deadzone){
    	deadzone = _deadzone;
    }


    /**
     * Adjust the rumbliness of the left rumble motor.
     * 
     * @param value Strength of Rumble, 0 is min and 1 is max.
     */
    public void setLeftRumble(float value) {
        joystick.setRumble(RumbleType.kLeftRumble, value);
    }


    /**
     * Adjust the rumbliness of the right rumble motor.
     * 
     * @param value Strength of Rumble, 0 is min and 1 is max.
     */
    public void setRightRumble(float value) {
        joystick.setRumble(RumbleType.kRightRumble, value);
    }


    /**
     * Turn off all rumble motors
     */
    public void rumbleOff() {
        setLeftRumble(0f);
        setRightRumble(0f);
    }


    /**
     * @return True if A is pressed, False if A is not pressed
     */
    public boolean A() {
        return joystick.getRawButton(XBOX_A_BUTTON);
    }


    /**
     * @return True if B is pressed, False if B is not pressed
     */
    public boolean B() {
        return joystick.getRawButton(XBOX_B_BUTTON);
    }


    /**
     * @return True if X is pressed, False if X is not pressed
     */
    public boolean X() {
        return joystick.getRawButton(XBOX_X_BUTTON);
    }


    /**
     * @return True if Y is pressed, False if Y is not pressed
     */
    public boolean Y() {
        return joystick.getRawButton(XBOX_Y_BUTTON);
    }


    /**
     * @return True if the left bumper button is pressed, False if it is not pressed
     */
    public boolean LB() {
        return joystick.getRawButton(XBOX_LEFT_BUTTON);
    }


    /**
     * @return True if the right bumper button is pressed, False if it is not pressed
     */
    public boolean RB() {
        return joystick.getRawButton(XBOX_RIGHT_BUTTON);
    }


    /**
     * @return True if the "Back" button (on top of the controller) is pressed, False if it is not
     *         pressed.
     */
    public boolean BackButton() {
        return joystick.getRawButton(XBOX_BACK_BUTTON);
    }


    /**
     * @return True if the "Start" button (on top of the controller) is pressed, False if it is not
     *         pressed.
     */
    public boolean StartButton() {
        return joystick.getRawButton(XBOX_START_BUTTON);
    }


    /**
     * @return True if the left stick is depressed, False if it is not pressed.
     */
    public boolean LStickButton() {
        return joystick.getRawButton(XBOX_LSTICK_BUTTON);
    }


    /**
     * @return True if the right stick is depressed, False if it is not pressed.
     */
    public boolean RStickButton() {
        return joystick.getRawButton(XBOX_RSTICK_BUTTON);
    }


    // JOYSTICK AXIS METHODS
    /**
     * @return Left stick X axis position. 0 is centered, 1 is all the way to the right, -1 is all
     *         the way to the left.
     */
    public double LStick_X() {
    	return limitDeadzone(joystick.getRawAxis(XBOX_LSTICK_XAXIS));
    }


    /**
     * @return Left stick Y axis position. 0 is centered, 1 is all the way to the top, -1 is all the
     *         way to the bottom.
     */
    public double LStick_Y() {
    	return limitDeadzone(-joystick.getRawAxis(XBOX_LSTICK_YAXIS));
    }


    /**
     * @return Left trigger depression. 0 is not depressed, 1 is fully pulled in, 0.5 is partially
     *         pulled. Note the Xbox underlying hardware ties the left and right triggers together,
     *         so pulling both triggers at the same time results in interesting things happening.
     */
    public double LTrigger() {
        return limitDeadzone(joystick.getRawAxis(XBOX_LTRIGGER_AXIS));
    }


    /**
     * @return Right trigger depression. 0 is not depressed, 1 is fully pulled in, 0.5 is partially
     *         pulled. Note the Xbox underlying hardware ties the left and right triggers together,
     *         so pulling both triggers at the same time results in interesting things happening.
     */
    public double RTrigger() {
        return limitDeadzone(joystick.getRawAxis(XBOX_RTRIGGER_AXIS));
    }


    /**
     * @return Right stick X axis position. 0 is centered, 1 is all the way to the right, -1 is all
     *         the way to the left.
     */
    public double RStick_X() {
        return limitDeadzone(joystick.getRawAxis(XBOX_RSTICK_XAXIS));
    }


    /**
     * @return Left stick Y axis position. 0 is centered, 1 is all the way to the top, -1 is all the
     *         way to the bottom.
     */
    public double RStick_Y() {
        return limitDeadzone(joystick.getRawAxis(XBOX_RSTICK_YAXIS));
    }


    // JOYSTICK DPAD HAT METHODS
    /**
     * @return The current "angle" from the DPad (POV switch)
     */
    public int DPad() {
        return joystick.getPOV(XBOX_DPAD_POV);
    }


    /**
     * @return True if the DPad is pushed up, False if it is not pressed
     */
    public boolean DPadUp() {
        if ((joystick.getPOV(XBOX_DPAD_POV) >= 315 || joystick.getPOV(XBOX_DPAD_POV) <= 45) && joystick.getPOV(XBOX_DPAD_POV) != -1)
            return true;
        else
            return false;
    }


    /**
     * @return True if the DPad is pushed right, False if it is not pressed
     */
    public boolean DPadRight() {
        if (joystick.getPOV(XBOX_DPAD_POV) >= 45 && joystick.getPOV(XBOX_DPAD_POV) <= 135)
            return true;
        else
            return false;
    }


    /**
     * @return True if the DPad is pushed down, False if it is not pressed
     */
    public boolean DPadDown() {
        if (joystick.getPOV(XBOX_DPAD_POV) >= 135 && joystick.getPOV(XBOX_DPAD_POV) <= 225)
            return true;
        else
            return false;
    }


    /**
     * @return True if the DPad is pushed left, False if it is not pressed
     */
    public boolean DPadLeft() {
        if (joystick.getPOV(XBOX_DPAD_POV) >= 225 && joystick.getPOV(XBOX_DPAD_POV) <= 315)
            return true;
        else
            return false;
    }
    
    
    private double limitDeadzone(double input){
    	if(Math.abs(input) >= deadzone){
    		return input;
    	} else {
    		return 0.0;
    	}
    }

}
