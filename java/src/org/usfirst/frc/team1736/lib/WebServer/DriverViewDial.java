package org.usfirst.frc.team1736.lib.WebServer;

import org.json.simple.JSONObject;

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

class DriverViewDial extends DriverViewObject {
	
    static final String VAL_DISPLAY_FORMATTER = "%5.2f";
	
    /** JSON object for initializing the Dial */
    private JSONObject dialJsonInitObj;
    
    /** JSON object for Updating the Dial */
    private JSONObject dialJsonUpdateObj;
    
    /** Properties of this dial */
    private String name;
    private double min;
    private double max;
    private double step;
    private double min_acceptable;
    private double max_acceptable;
    
    private double cur_val;
	
	public DriverViewDial(String name_in, double min_in, double max_in, double step_in, double min_acceptable_in,
            double max_acceptable_in){
		
        // Sanitize user inputs
        if (min_in >= max_in) {
            System.out
                    .println("WARNING: new dial " + name_in + " has min value greater than maximum. Not adding dial.");
            return;
        }
        if (min_acceptable_in >= max_acceptable_in) {
            System.out.println("WARNING: new dial " + name_in
                    + " has an acceptable min value greater than maximum. Not adding dial.");
            return;
        }
        if (max_in - min_in <= step_in) {
            System.out.println("WARNING: new dial " + name_in
                    + " has too small a step size. Make sure the step size is smaller than the range of the dial. Not adding dial.");
            return;
        }
        
        //save data
        name = Utils.nameTransform(name_in);
        min = min_in;
        max = max_in;
        step = step_in;
        min_acceptable = min_acceptable_in;
        max_acceptable = max_acceptable_in;
        cur_val = min_in;
        
        //Create the JSON object for defining the init data for the dial
        dialJsonInitObj = new JSONObject();
        dialJsonInitObj.put("type", "dial");
        dialJsonInitObj.put("name", name);
        dialJsonInitObj.put("displayName", name_in);
        dialJsonInitObj.put("min", min);
        dialJsonInitObj.put("max", max);
        dialJsonInitObj.put("min_acceptable", min_acceptable);
        dialJsonInitObj.put("max_acceptable", max_acceptable);
        dialJsonInitObj.put("step", step);
        
        
        //Create the JSON object for defining the update data for the dial
        dialJsonUpdateObj = new JSONObject();
        dialJsonUpdateObj.put("type", "dial");
        dialJsonUpdateObj.put("name", name);
        dialJsonUpdateObj.put("value", String.format(VAL_DISPLAY_FORMATTER, min_in));
        return;
	}
	
	/**
	 * Comparison function to evaluate if a given name of a dial matches this dial
	 * @return true if the names match, false otherwise
	 */
	public boolean nameEquals(String name_in){
		return Utils.nameTransform(name_in).equals(name);
	}
	
	//Public get/set functions to fulfill the object interface
	@Override
	public JSONObject getInitJsonObj(){
		return dialJsonInitObj;
	}

	@Override
	public JSONObject getUpdJsonObj(){
		return dialJsonUpdateObj;
	}

	public void setVal(double value) {
		cur_val = value;
		dialJsonUpdateObj.put("value", String.format(VAL_DISPLAY_FORMATTER, value));
	}
	
	

}
