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

class DriverViewStringBox extends DriverViewObject {
	
	
    /** JSON object for initializing the Stringbox */
    private JSONObject sbJsonInitObj;
    
    /** JSON object for Updating the Stringbox */
    private JSONObject sbJsonUpdateObj;
    
    String name;
    String value;
	
	public DriverViewStringBox(String name_in){
		
		name = Utils.nameTransform(name_in);
		value = "N/A";
		
        // Create new object
        sbJsonInitObj = new JSONObject();
        sbJsonInitObj.put("type", "stringbox");
        sbJsonInitObj.put("name", name);
        sbJsonInitObj.put("displayName", name_in);
        sbJsonInitObj.put("value", value);
        
        // Create new object
        sbJsonUpdateObj = new JSONObject();
        sbJsonUpdateObj.put("type", "stringbox");
        sbJsonUpdateObj.put("name", name);
        sbJsonUpdateObj.put("value", value);

	}

	@Override
	public JSONObject getInitJsonObj() {
		return sbJsonInitObj;
	}

	@Override
	public JSONObject getUpdJsonObj() {
		return sbJsonUpdateObj;
	}

	public void setVal(String value_in) {
		value = value_in;
		sbJsonUpdateObj.put("value", value);
	}

}
