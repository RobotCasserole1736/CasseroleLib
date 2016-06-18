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
	static volatile List<String> dial_vals = new ArrayList<String>();
	static volatile List<String> ordered_obj_name_list = new ArrayList<String>(); //Used to help preserve the order the user creates the dials in, since the hash table destroys this info
	
	static int num_dials = 0;
	
	static final String VAL_DISPLAY_FORMATTER = "%5.2f";
	
	/**
	 * Create a new dial to display on the driver view webpage.  Should be called at init, as new dials cannot be added at runtime.
	 * @param name_in Name of the value to display. Also used to reference the value when updating it.
	 * @param min_in Minimum value displayed on the dial.
	 * @param max_in Maximum value displayed on the dial.
	 * @param step_in Step value between dial tick marks.
	 */
	public static void newDial(String name_in, double min_in, double max_in, double step_in){
		//Sanitize user inputs
		if(min_in >= max_in){
			System.out.println("WARNING: new dial " + name_in + " has min value greater than maximum. Not adding dial.");
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
		new_obj.put("index", num_dials);
		new_obj.put("min", min_in);
		new_obj.put("max", max_in);
		new_obj.put("step", step_in);
		dial_vals.add(String.format(VAL_DISPLAY_FORMATTER, min_in));
		driverView_objects.put(name_in, new_obj);
		ordered_obj_name_list.add(name_in);
		num_dials += 1;
		return;
	}
	
	/**
	 * Add a new webcam to the driver view webpage. Should be called at init, as new webcams cannot be added at runtime.
	 * @param url_in Web address of the motion JPEG stream from the camera.
	 * @param name_in Name of the web stream. Internal uses only, currently...
	 */
	public static void newWebcam(String url_in, String name_in){
		//Create new object
		JSONObject new_obj = new JSONObject();
		new_obj.put("type", "webcam");
		new_obj.put("name", name_in);
		new_obj.put("url", url_in);
		driverView_objects.put(name_in, new_obj);
		ordered_obj_name_list.add(name_in);
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
			dial_vals.set(index, String.format(VAL_DISPLAY_FORMATTER, Math.min((double)obj_tmp.get("max"), Math.max((double)obj_tmp.get("min"), value_in))));
			return;
		}
		
		//If we get here, it means we didn't find the value
		System.out.println("WARNING: could not find a dial value for " + name_in + ". No value set.");
		return;
	}


}
