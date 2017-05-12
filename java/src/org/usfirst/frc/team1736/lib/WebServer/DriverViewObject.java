package org.usfirst.frc.team1736.lib.WebServer;

import org.json.simple.JSONObject;

public abstract class DriverViewObject {
	
	/**
	 * Inteface to get the sent JSON object when the webpage asks for the initial defintion of what will be on the driverview
	 * @return
	 */
	public abstract JSONObject getInitJsonObj();
	
	/**
	 * Interface to get the sent JSON object when it is time to update the values displayed on the webpage
	 * @return
	 */
	public abstract JSONObject getUpdJsonObj();

}
