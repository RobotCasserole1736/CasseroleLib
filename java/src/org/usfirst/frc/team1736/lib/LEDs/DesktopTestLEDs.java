package org.usfirst.frc.team1736.lib.LEDs;

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

import java.awt.Color;
import java.awt.Graphics;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;


public class DesktopTestLEDs extends JPanel implements CasseroleLEDInterface  {

	private static final long serialVersionUID = 5649843216559961231L;
	
	// Color Buffer - all bytes to send out from the
    double[] ledBuffer;
    int num_leds;

    // byte restrictions
    double ledMaxVal = 1.0;

    // State variables
    boolean newBuffer; // true when the ledBuffer has been updated since the last time it was
                       // written to the LEDs
    
    static int blueOffset = 0;
    static int greenOffset = 1;
    static int redOffset = 2;
    
    //where our LED's get drawn
    JFrame window;
    
    
    //GUI Parameters (two strips)
    public final int LED_RADIUS_PX = 10;
    public final int LED_SPACING_PX = 30;
    
    public final int UPPER_LED_COUNT = 26;
    public final int LOWER_LED_COUNT = 25;
    
    Timer timerThread;
    JTextArea timereadout;
    
    int loop_counter;

    public DesktopTestLEDs(int numLEDs){
    	
    	//Set up internal buffers for LED colors
    	num_leds = numLEDs;
    	ledBuffer = new double[num_leds*3];
    	
    	timereadout = new JTextArea("Time:");
    	timereadout.setEditable(false);
    	this.add(timereadout,-1);
    	
        loop_counter = 0;
        
        //set up GUI update thread
        timerThread = new java.util.Timer("LED Test GUI Update");
        timerThread.schedule(new DesktopTestLEDSUpdateTask(this), (long) (m_update_period_ms/2), (long) (m_update_period_ms/2));

        
    }
	
	@Override
	public void clearColorBuffer() {
        for (int i = 0; i < num_leds; i++) {
            setLEDColor(i, 0, 0, 0);
        }
        // Mark the buffer as updated
        newBuffer = true;
        // we're done!
        return;
		
	}

	@Override
	public void setLEDColor(int index, double r, double g, double b) {
        ledBuffer[index*3 + blueOffset] = limitIntensityRange(b);
        ledBuffer[index*3 + greenOffset] = limitIntensityRange(g);
        ledBuffer[index*3 + redOffset] = limitIntensityRange(r);
        // Mark the buffer as updated
        newBuffer = true;
        // we're done!
        return;
		
	}
	
	@Override
	public void paintComponent(Graphics g){
		//Assume a set of strips (two, upper and lower)
		
		//visual offset to center led in strip
		int led_offset = (LED_SPACING_PX - LED_RADIUS_PX*2)/2;
		
		//Upper
		g.setColor(Color.BLACK);
		g.fillRect(LED_SPACING_PX, LED_SPACING_PX, UPPER_LED_COUNT*LED_SPACING_PX, LED_SPACING_PX);  
		for(int i = 0; i < UPPER_LED_COUNT; i++){
			g.setColor(new Color(getRVal(i), getGVal(i), getBVal(i)));
			g.fillOval(LED_SPACING_PX*(i+1)+led_offset, LED_SPACING_PX+led_offset, LED_RADIUS_PX*2, LED_RADIUS_PX*2);
		}
		
		//lower
		g.setColor(Color.BLACK);
		g.fillRect(LED_SPACING_PX, LED_SPACING_PX*3, LOWER_LED_COUNT*LED_SPACING_PX, LED_SPACING_PX);  
		for(int i = 0; i < LOWER_LED_COUNT; i++){
			g.setColor(new Color(getRVal(i+UPPER_LED_COUNT), getGVal(i+UPPER_LED_COUNT), getBVal(i+UPPER_LED_COUNT)));
			g.fillOval(LED_SPACING_PX*(i+1)+led_offset, LED_SPACING_PX*3+led_offset, LED_RADIUS_PX*2, LED_RADIUS_PX*2);
		}
		
		//time marker
		timereadout.setText("Time: " + String.format("%5.3f", loop_counter*(m_update_period_ms/2)/1000) + "s");
	}
	
	private int getRVal(int index){
		return (int)Math.round(ledBuffer[index*3 + redOffset] * 255);
	}
	private int getGVal(int index){
		return (int)Math.round(ledBuffer[index*3 + greenOffset] * 255);
	}
	private int getBVal(int index){
		return (int)Math.round(ledBuffer[index*3 + blueOffset] * 255);
	}
	
	
	private double limitIntensityRange(double in){
		double out;
		if(in > 1.0){
			out = 1.0;
		} else if(in < 0.0){
			out = 0.0;
		} else {
			out = in;
		}
		
		return out;
	}
	

	public void updateColors() {
		loop_counter++;
		if(newBuffer){
			repaint();
		}
		newBuffer = false;
		
	}
	
	
    // Java multithreading magic. Do not touch.
    // Touching will incour the wrath of Cthulhu, god of java and LED Strips.
    // May the oceans of 1's and 0's rise to praise him.
    private class DesktopTestLEDSUpdateTask extends TimerTask {
        private DesktopTestLEDs m_leds;


        public DesktopTestLEDSUpdateTask(DesktopTestLEDs leds) {
            if (leds == null) {
                throw new NullPointerException("Given Desktop LEDs Controller Class was null");
            }
            m_leds = leds;
        }


        @Override
        public void run() {
            m_leds.updateColors();
        }
    }

}
