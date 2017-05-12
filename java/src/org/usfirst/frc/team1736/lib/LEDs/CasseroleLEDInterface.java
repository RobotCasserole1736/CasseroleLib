package org.usfirst.frc.team1736.lib.LEDs;

import java.awt.Color;

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

public interface CasseroleLEDInterface {

    // period between periodic function calls
    // in milliseconds
    double m_update_period_ms = 50;
	
	public default void clearColorBuffer(){
		return;
	}
	public default void setLEDColor(int index, double r, double g, double b) {
		return;
	}
	
	public default void setLEDColor(int index, Color c){
		setLEDColor(index, ((double)c.getRed())/255.0, ((double)c.getGreen())/255.0, ((double)c.getBlue())/255.0);
	}

	public default void setLEDColorHSL(int idx, double h, double s, double l){
	    double r, g, b;

	    if (s == 0f) {
	        r = g = b = l; // achromatic
	    } else {
	        double q = l < 0.5f ? l * (1 + s) : l + s - l * s;
	        double p = 2 * l - q;
	        r = hueToRgb(p, q, h + 1f/3f);
	        g = hueToRgb(p, q, h);
	        b = hueToRgb(p, q, h - 1f/3f);
	    }
	    
	    setLEDColor(idx, r, g, b);
	}

	/** Helper method that converts hue to rgb */
	public static double hueToRgb(double p, double q, double t) {
	    if (t < 0f)
	        t += 1f;
	    if (t > 1f)
	        t -= 1f;
	    if (t < 1f/6f)
	        return p + (q - p) * 6f * t;
	    if (t < 1f/2f)
	        return q;
	    if (t < 2f/3f)
	        return p + (q - p) * (2f/3f - t) * 6f;
	    return p;
	}
	
	/**
	 * Get an intermediate color between two colors
	 * Super simple cross-fade in RGB space with no regard for
	 * HSV or any actual science on how colors work.
	 * @param ratio 0 is full c0, 1 is full c1
	 * @param c0 one color
	 * @param c1 the other color
	 * @return an intermediate color
	 */
	public static Color getIntermedeateColor(double ratio, Color c0, Color c1){
		double red   = (double)(c0.getRed())   * (1.0-ratio) + (double)(c1.getRed())   * (ratio);
		double green = (double)(c0.getGreen()) * (1.0-ratio) + (double)(c1.getGreen()) * (ratio);
		double blue  = (double)(c0.getBlue())  * (1.0-ratio) + (double)(c1.getBlue())  * (ratio);
		return new Color((int)red, (int)green, (int)blue);
	}
	
}
