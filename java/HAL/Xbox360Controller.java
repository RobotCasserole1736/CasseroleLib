package org.usfirst.frc.team1736.robot;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Joystick.RumbleType;

public class Xbox360Controller {
	
	/*This class was created purely in an effort to make controller calls
	 * less clunky overall. Not terribly useful, but hopefully way easier than
	 * typing out a super long string every time you want to call a controller
	 * button.
	 * 
	 * I don't want to put "get" in front of my methods because that'll make the calls
	 * ever so slightly longer, so I won't. As Chris says, "If later we don't like it,
	 * we'll change it."
	 */
	
	//Xbox Controller 2015 (Zero Based (I think)) Constants
	//-Controller Buttons
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
			
	//-Controller Axes
	final static int XBOX_LSTICK_XAXIS = 0;
	final static int XBOX_LSTICK_YAXIS = 1;
	final static int XBOX_LTRIGGER_AXIS = 2;
	final static int XBOX_RTRIGGER_AXIS = 3;
	final static int XBOX_RSTICK_XAXIS = 4;
	final static int XBOX_RSTICK_YAXIS = 5;
	
	//-Controller D-Pad POV Hat
	final static int XBOX_DPAD_POV = 0;
	
	private Joystick joystick;
	
	public Xbox360Controller(int joystick_id)
	{
		joystick = new Joystick(joystick_id);
	}
	
	//ARE YOU READY TO RRRRRRUUUUUUUUUUMMMMMMMBBBBBBLLLLLLLLEEEEEEEEE
	public void setLeftRumble(float value)
	{
		joystick.setRumble(RumbleType.kLeftRumble, value);
	}
	
	public void setRightRumble(float value)
	{
		joystick.setRumble(RumbleType.kRightRumble, value);
	}
	
	public void rumbleOff()
	{
		setLeftRumble(0f);
		setRightRumble(0f);
	}
	
	//JOYSTICK BUTTON METHODS
	public boolean A()
	{
		return joystick.getRawButton(XBOX_A_BUTTON);
	}
	
	public boolean B()
	{
		return joystick.getRawButton(XBOX_B_BUTTON);
	}
	
	public boolean X()
	{
		return joystick.getRawButton(XBOX_X_BUTTON);
	}
	
	public boolean Y()
	{
		return joystick.getRawButton(XBOX_Y_BUTTON);
	}
	
	public boolean LB()
	{
		return joystick.getRawButton(XBOX_LEFT_BUTTON);
	}
	
	public boolean RB()
	{
		return joystick.getRawButton(XBOX_RIGHT_BUTTON);
	}
	
	public boolean BackButton()
	{
		return joystick.getRawButton(XBOX_BACK_BUTTON);
	}
	
	public boolean StartButton()
	{
		return joystick.getRawButton(XBOX_START_BUTTON);
	}
	
	public boolean LStickButton()
	{
		return joystick.getRawButton(XBOX_LSTICK_BUTTON);
	}
	
	public boolean RStickButton()
	{
		return joystick.getRawButton(XBOX_RSTICK_BUTTON);
	}
	
	//JOYSTICK AXIS METHODS
	public double LStick_X()
	{
		return joystick.getRawAxis(XBOX_LSTICK_XAXIS);
	}
	
	public double LStick_Y()
	{
		return joystick.getRawAxis(XBOX_LSTICK_YAXIS);
	}
	
	public double LTrigger()
	{
		return joystick.getRawAxis(XBOX_LTRIGGER_AXIS);
	}
	
	public double RTrigger()
	{
		return joystick.getRawAxis(XBOX_RTRIGGER_AXIS);
	}
	
	public double RStick_X()
	{
		return joystick.getRawAxis(XBOX_RSTICK_XAXIS);
	}
	
	public double RStick_Y()
	{
		return joystick.getRawAxis(XBOX_RSTICK_YAXIS);
	}
	
	//JOYSTICK DPAD HAT METHOD
	public int DPad()
	{
		return joystick.getPOV(XBOX_DPAD_POV);
	}
}
