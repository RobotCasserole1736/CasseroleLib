package org.usfirst.frc.team1736.lib.WebServer;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.usfirst.frc.team1736.lib.Calibration.CalWrangler;

public class CassesroleWebStates {
	/** The list of objects which are broadcast. Must be volatile to ensure atomic accesses */
	static volatile List<JSONObject> data_array_elements = new ArrayList<JSONObject>();
	static CalWrangler wrangler_obj;
	
	/**
	 * Sets the calibration wrangler which will be used for web operations
	 */
	public static void setCalWrangler(CalWrangler wrangler_in){
		wrangler_obj = wrangler_in;
	}
	
	/**
	 * Get the present calWrangler being used for web operations
	 * @return wrangler in use
	 */
	public static CalWrangler getCalWrangler(){
		return wrangler_obj;
	}
	
	/** 
	 * Put a new state to the web interface, or update an existing one with the same name 
	 * @param name Name for the state to display.
	 * @param value Double Floating-point value to display
	 */
	public static void putDouble(String name, double value){
		putGeneric(name, Double.toString(value));
	}
	
	/** 
	 * Put a new state to the web interface, or update an existing one with the same name 
	 * @param name Name for the state to display.
	 * @param value Double Floating-point value to display
	 */
	public static void putBoolean(String name, boolean value){
		putGeneric(name, Boolean.toString(value));
	}
	
	public static void putInteger(String name, int value){
		putGeneric(name, Integer.toString(value));
	}
	
	public static void putString(String name, String value){
		putGeneric(name, value);	
	}
	
	private static void putGeneric(String name, String value){
		boolean is_new = true;
		for(JSONObject obj : data_array_elements){
			if(obj.get("name").equals(name)){
				obj.put("value", value);
				is_new = false;
			}
		}
		
		if(is_new){
			JSONObject new_obj = new JSONObject();
			new_obj.put("name", name);
			new_obj.put("value", value);
			data_array_elements.add(new_obj);
		}
	}

}
