package org.usfirst.frc.team1736.lib.WebServer;

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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import org.json.simple.JSONObject;

/**
 * DESCRIPTION: <br>
 * Realtime Plot View webpage definition class. Allows the user to send a set of variables to 
 * an interface where they can be plotted in real-time. <br>
 * ASSUMPTIONS: <br>
 * Be sure the casserole webserver is started at some point, otherwise the webpage won't be
 * displayed. Note that states can only be added (even at runtime), but never removed. At least not
 * yet... <br>
 * USAGE:
 * <ol>
 * <li>On init, call addNewSignal with all the signals you want available.</li>
 * <li>During runtime, call the addSample() method to record a new value to be plotted.</li>
 * </ol>
 * 
 *
 */
public class CasseroleWebPlots {
    /** The set of objects which are broadcast. Must be volatile to ensure atomic accesses */
    public static volatile Hashtable<String, PlotSignal> RTPlotSignals = new Hashtable<String, PlotSignal>();
    
    public static ArrayList<PlotSignal> activeSignalList = null;
    
    public static boolean acqActive = false;

    /**
     * Put a new signal to the web interface, or update an existing one with the same name
     * 
     * @param name Name for the state to display.
     * @param value Double Floating-point value to display
     */
	public static void addSample(String name, double samp_time, double value) {

		String fixed_name = Utils.nameTransform(name);
        if (RTPlotSignals.containsKey(fixed_name)) {
        	RTPlotSignals.get(fixed_name).addSample(samp_time, value);

        } else {
        	System.out.println("Error: RT Plot signal name " + name + " does not exist!");
        }
    }
    
    public static void addNewSignal(String name, String units){
    	String fixed_name = Utils.nameTransform(name);
    	if(!RTPlotSignals.containsKey(fixed_name)){
    		PlotSignal new_obj = new PlotSignal(fixed_name, name, units);
    		RTPlotSignals.put(fixed_name, new_obj);
    	}
    }
    
    /**
     * Given an array of signal names from a client, starts acquisition of those signals 
     * @param signal_names
     */
    public static void startAcq(List<String> signal_names){
    	activeSignalList = new ArrayList<PlotSignal>();
    	
    	for(String signal_name : signal_names){
    		if(CasseroleWebPlots.RTPlotSignals.containsKey(signal_name)){
    			activeSignalList.add(CasseroleWebPlots.RTPlotSignals.get(signal_name));
    			CasseroleWebPlots.RTPlotSignals.get(signal_name).startAcq();
    		} else {
    			System.out.println("ERROR: RT Plot client asked for non-existant signal " + signal_name);
    		}
    	}
    	acqActive = true;
    	
    }
    
    /**
     * Stops all current acquisition
     */
    public static void stopAcq(){
    	acqActive = false;
    	
    	for(PlotSignal signal : activeSignalList){
    		signal.stopAcq();
    		signal.clearBuffer();
    	}
    	
    	activeSignalList = null;
    	
    }

}
