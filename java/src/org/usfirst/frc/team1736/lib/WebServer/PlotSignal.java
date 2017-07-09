package org.usfirst.frc.team1736.lib.WebServer;

import java.util.LinkedList;

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
import java.util.Queue;

class PlotSignal implements Comparable<PlotSignal> {

	String name;
	String display_name;
	String units;
	
	boolean acq_active;
	
	Queue<PlotSample> sample_queue;
	
	/**
	 * Class which describes one line on a plot
	 * @param name_in String of what to call the signal
	 * @param units_in units the signal is in.
	 */
	public PlotSignal(String name_in, String display_name_in, String units_in){
		display_name = display_name_in;
		name = name_in;
		units = units_in;
		
		acq_active = false;
		
		sample_queue = new LinkedList<PlotSample>();
	}
	
	/**
	 * Adds a new sample to the signal queue. It is intended that
	 * the controls code would call this once per loop to add a new
	 * datapoint to the real-time graph.
	 * @param time_in
	 * @param value_in
	 */
	public void addSample(double time_in, double value_in){
		if(acq_active){
			sample_queue.add(new PlotSample(time_in, value_in));
		}
	}
	
	/**
	 * Start acquiring data on this channel. Should be called before attempting to read info.
	 */
	public void startAcq(){
		acq_active = true;
	}
	/**
	 * Stop acquiring data on this channel. Should be called when data no longer needs to be transmitted.
	 */
	public void stopAcq(){
		acq_active = false;
	}
	
	/**
	 * Returns an array of all the samples currently in the queue, and then clears it.
	 * It is intended that the weberver would call this to transmit all available 
	 * data from previous iterations. This might return null if the control code
	 * has no new data.
	 */
	public PlotSample[] getAllSamples(){
		int size = sample_queue.size();
		PlotSample[] retval;
		if(size > 0){
			retval = new PlotSample[size];
			sample_queue.toArray(retval);
			sample_queue.clear();
		} else {
			retval = null;
		}
		return retval;
	}
	
	/**
	 * Discards all samples from the buffer
	 */
	public void clearBuffer(){
		sample_queue.clear();
	}
	
	/**
	 * @return The name of the signal
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * @return The User-friendly name of the signal
	 */
	public String getDisplayName(){
		return display_name;
	}
	
	/**
	 * @return The name of the units the signal is measured in.
	 */
	public String getUnits(){
		return units;
	}

	/**
	 *  Implement a comparison function for sorting signals by name 
	 */
	@Override
	public int compareTo(PlotSignal o) {
		return name.compareTo(o.getName());
	}
}
