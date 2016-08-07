package org.usfirst.frc.team1736.lib.WebServer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.json.simple.JSONObject;
import org.usfirst.frc.team1736.lib.Calibration.CalWrangler;

/**
 * DESCRIPTION:
 * <br>
 * Driver View webpage definition class. Adds new things to the driver view webpage 
 * (webcam mjpeg videos or dials). Has methods to update the dial displayed values.
 * <br>
 * ASSUMPTIONS:
 * <br>
 * Be sure the casserole webserver is started at some point, otherwise the webpage won't be displayed.  
 * <br>
 * USAGE:    
 * <ol>   
 * <li>Instantiate class</li> 
 * <li>On init, call newDial and newWebcam once per object to be displayed on the driver view webpage.</li> 
 * <li>During runtime, call setDialValue to update the dial value display</li>    
 * </ol>
 * 
 *
 */

public class CasseroleDriverView {
	/** The list of objects which are broadcast. Must be volatile to ensure atomic accesses */
	static volatile Hashtable<String, JSONObject> driverView_objects = new Hashtable<String, JSONObject>();
	static volatile List<String> obj_vals = new ArrayList<String>();
	static volatile List<String> ordered_obj_name_list = new ArrayList<String>(); //Used to help preserve the order the user creates the dials in, since the hash table destroys this info
	
	static int num_sendable_objs = 0;

	static final String VAL_DISPLAY_FORMATTER = "%5.2f";
	
	/**
	 * Create a new dial to display on the driver view webpage.  Should be called at init, as new dials cannot be added at runtime.
	 * Dials are designed for two purposes: providing detailed value information at runtime, as well as at-a-glance indication of acceptable/unacceptable.
	 * The numbers and units presented on the dial provide the first, while the red/green outline around the arc of travel shows the operator whether
	 * the value is acceptable without reading any numbers. Use the min/max acceptable limit arguments to define this range.
	 * @param name_in Name of the value to display. Also used to reference the value when updating it.
	 * @param min_in Minimum value displayed on the dial.
	 * @param max_in Maximum value displayed on the dial.
	 * @param step_in Step value between dial tick marks.
	 * @param min_acceptable_in Lower limit of green display area on drawn dial.
	 * @param max_acceptable_in Upper limit of green display area on drawn dial.
	 */
	public static void newDial(String name_in, double min_in, double max_in, double step_in, double min_acceptable_in, double max_acceptable_in){
		//Sanitize user inputs
		if(min_in >= max_in){
			System.out.println("WARNING: new dial " + name_in + " has min value greater than maximum. Not adding dial.");
			return;
		}
		if(min_acceptable_in >= max_acceptable_in){
			System.out.println("WARNING: new dial " + name_in + " has an acceptable min value greater than maximum. Not adding dial.");
			return;
		}
		if(max_in-min_in <= step_in){
			System.out.println("WARNING: new dial " + name_in + " has too small a step size. Make sure the step size is smaller than the range of the dial. Not adding dial.");
			return;
		}
		//Create new object
		JSONObject new_obj = new JSONObject();
		new_obj.put("type", "dial");
		new_obj.put("name", name_in);
		new_obj.put("index", num_sendable_objs);
		new_obj.put("min", min_in);
		new_obj.put("max", max_in);
		new_obj.put("min_acceptable", min_acceptable_in);
		new_obj.put("max_acceptable", max_acceptable_in);
		new_obj.put("step", step_in);
		obj_vals.add(String.format(VAL_DISPLAY_FORMATTER, min_in));
		driverView_objects.put(name_in, new_obj);
		ordered_obj_name_list.add(name_in);
		num_sendable_objs += 1;
		return;
	}
	
	/**
	 * Add a new webcam to the driver view webpage. Should be called at init, as new webcams cannot be added at runtime.
	 * @param url_in Web address of the motion JPEG stream from the camera.
	 * @param marker_x X draw position of the crosshairs (in percent - 0% is full left, 100% is full right).
	 * @param marker_y Y draw position of the crosshairs (in percent - 0% is full top,  100% is full bottom).
	 * @param img_rotate_deg Degrees clockwise to rotate the image to be displayed. Crosshairs are drawn on top of the already-rotated image.
	 * @param name_in Name of the web stream. Internal uses only, currently...
	 */
	public static void newWebcam(String name_in, String url_in, double marker_x, double marker_y, double img_rotate_deg){
		//Create new object
		JSONObject new_obj = new JSONObject();
		new_obj.put("type", "webcam");
		new_obj.put("name", name_in);
		new_obj.put("url", url_in);
		new_obj.put("targ_x_pct", marker_x);
		new_obj.put("targ_y_pct", marker_y);
		new_obj.put("rotation_deg", img_rotate_deg);
		driverView_objects.put(name_in, new_obj);
		ordered_obj_name_list.add(name_in);
		return;
	}
	
	/**
	 * Create a new String to display on the driver view webpage.  Should be called at init, as new string values cannot be added at runtime.
	 * Strings are hard to read and should be used sparingly. If at all possible, use a boolean or dial.
	 * @param name_in Name of the value to display. Also used to reference the value when updating it.
	 */
	public static void newStringBox(String name_in){
		//Create new object
		JSONObject new_obj = new JSONObject();
		new_obj.put("type", "stringbox");
		new_obj.put("name", name_in);
		new_obj.put("index", num_sendable_objs);
		obj_vals.add("N/A");
		driverView_objects.put(name_in, new_obj);
		ordered_obj_name_list.add(name_in);
		num_sendable_objs += 1;
		return;
	}
	
	/**
	 * Create a new Boolean indicator to display on the driver view webpage.  Should be called at init, as new indicators values cannot be added at runtime.
	 * Designing with these indicators should follow a "dark console" philosophy: or normal operations, all indicators should be dark. Red indicators should
	 * illuminate for very bad abmormailites (pressure low, batt voltage low, etc). Yellow is for less severe conditions which degrade performance. Green
	 * is for conditions which are not usually present, but are good. (ex: ball in intake).
	 * @param name_in Name of the value to display. Also used to reference the value when updating it.
	 * @param color_in Color to display. Currently, only supported values are "red", "yellow", and "green".
	 */
	public static void newBoolean(String name_in, String color_in){
		//Create new object
		JSONObject new_obj = new JSONObject();
		new_obj.put("type", "boolean");
		new_obj.put("name", name_in);
		new_obj.put("color", color_in);
		new_obj.put("index", num_sendable_objs);
		obj_vals.add("False");
		driverView_objects.put(name_in, new_obj);
		ordered_obj_name_list.add(name_in);
		num_sendable_objs += 1;
		return;
	}
	
	/**
	 * Display a new value on an existing dial at runtime
	 * @param name_in Name of the dial to update
	 * @param value_in Value to display on the dial. Should be in the min/max range assigned for the dial, or the displayed value will be truncated.
	 */
	//might be called from different threads, but all calls go to the web server thread.
	public static synchronized void setDialValue(String name_in, double value_in){
		int index = -1;
		if(driverView_objects.containsKey(name_in)){
			JSONObject obj_tmp = driverView_objects.get(name_in);
			index = (int) obj_tmp.get("index");
			obj_vals.set(index, String.format(VAL_DISPLAY_FORMATTER, Math.min((double)obj_tmp.get("max"), Math.max((double)obj_tmp.get("min"), value_in))));
			return;
		}
		//If we get here, it means we didn't find the value
		System.out.println("WARNING: could not find a dial value for " + name_in + ". No value set.");
		return;
	}
	
	/**
	 * Display a new value on an existing string box at runtime
	 * @param name_in Name of the dial to update
	 * @param value_in Value to display on the dial. Should be in the min/max range assigned for the dial, or the displayed value will be truncated.
	 */
	//might be called from different threads, but all calls go to the web server thread.
	public static synchronized void setStringBox(String name_in, String value_in){
		int index = -1;
		if(driverView_objects.containsKey(name_in)){
			JSONObject obj_tmp = driverView_objects.get(name_in);
			index = (int) obj_tmp.get("index");
			obj_vals.set(index, value_in);
			return;
		}
		//If we get here, it means we didn't find the value
		System.out.println("WARNING: could not find a string box for " + name_in + ". No value set.");
		return;
	}
	
	/**
	 * Display a new value on an existing boolean display
	 * @param name_in Name of the dial to update
	 * @param value_in Value to display on the indicator.
	 */
	//might be called from different threads, but all calls go to the web server thread.
	public static synchronized void setBoolean(String name_in, boolean value_in){
		int index = -1;
		if(driverView_objects.containsKey(name_in)){
			JSONObject obj_tmp = driverView_objects.get(name_in);
			index = (int) obj_tmp.get("index");
			obj_vals.set(index, value_in?"1":"0");
			return;
		}
		//If we get here, it means we didn't find the value
		System.out.println("WARNING: could not find a boolean for for " + name_in + ". No value set.");
		return;
	}


}
