package org.usfirst.frc.team1736.lib.WebServer;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.usfirst.frc.team1736.lib.Calibration.CalWrangler;

public class CassesroleDriverView {
	/** The list of objects which are broadcast. Must be volatile to ensure atomic accesses */
	static volatile List<JSONObject> driverView_objects = new ArrayList<JSONObject>();
	static volatile List<String> dial_vals = new ArrayList<String>();
	
	static int num_dials = 0;
	
	static final String VAL_DISPLAY_FORMATTER = "%5.2f";
	
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
		driverView_objects.add(new_obj);
		num_dials += 1;
		return;
	}
	
	public static void newWebcam(String url_in){
		//Create new object
		JSONObject new_obj = new JSONObject();
		new_obj.put("type", "webcam");
		new_obj.put("url", url_in);
		driverView_objects.add(new_obj);
		return;
	}
	
	//might be called from different threads, but all calls go to the web server thread.
	public static synchronized void setDialValue(String name_in, double value_in){
		int index = -1;
		
		//look for index of dial to update
		for(JSONObject obj : driverView_objects){
			if(obj.get("type").equals("dial") & obj.get("name").equals(name_in) ){
				index = (int) obj.get("index");
				dial_vals.set(index, String.format(VAL_DISPLAY_FORMATTER, Math.min((double)obj.get("max"), Math.max((double)obj.get("min"), value_in))));
				return;
			}
		}
		
		//If we get here, it means we didn't find the value
		System.out.println("WARNING: could not find a dial value for " + name_in + ". No value set.");
		return;
	}


}
