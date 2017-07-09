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

class DriverViewBoolean extends DriverViewObject {

    /** JSON object for initializing the Boolean indicator */
    private JSONObject boolJsonInitObj;
    
    /** JSON object for Updating the Boolean indicator */
    private JSONObject boolJsonUpdateObj;
    
	String name;
	String color;
	boolean value;
	
	public DriverViewBoolean(String name_in, String color_in){
		
		name = Utils.nameTransform(name_in);
		color = color_in;
		value = false;
		
        // Create new objects
        boolJsonInitObj = new JSONObject();
        boolJsonInitObj.put("type", "boolean");
        boolJsonInitObj.put("name", name);
        boolJsonInitObj.put("displayName", name_in);
        boolJsonInitObj.put("color", color_in);
        
        boolJsonUpdateObj = new JSONObject();
        boolJsonUpdateObj.put("type", "boolean");
        boolJsonUpdateObj.put("name", name);
        boolJsonUpdateObj.put("value", "False");

	}

	@Override
	public JSONObject getInitJsonObj() {
		// TODO Auto-generated method stub
		return boolJsonInitObj;
	}

	@Override
	public JSONObject getUpdJsonObj() {
		// TODO Auto-generated method stub
		return boolJsonUpdateObj;
	}
	
	public void setVal(boolean value_in) {
		value = value_in;
		if(value){
			boolJsonUpdateObj.put("value", "True");
		} else {
			boolJsonUpdateObj.put("value", "False");
		}
	}

}
