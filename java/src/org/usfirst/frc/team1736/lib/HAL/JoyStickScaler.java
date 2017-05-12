package org.usfirst.frc.team1736.lib.HAL;

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

import org.usfirst.frc.team1736.lib.Calibration.Calibration;

public class JoyStickScaler {
	static double input ;
	static Calibration exponent1 = new Calibration("Joystick Exponential Scale", 2.0, 1.0, 10.0);
	
	
	public static double joyscale(double input){
		double output;
	
	
	
	if (input>=0) {
	    output = Math.pow(input, exponent1.get());
	}
	else{
	    output = -Math.pow(-input, exponent1.get());
	}
	return output;
	}
	
	
	
	
	
	
}
//-((-input)^(exponent))