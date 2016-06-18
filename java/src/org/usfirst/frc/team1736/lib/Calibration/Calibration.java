package org.usfirst.frc.team1736.lib.Calibration;


///////////////////////////////////////////////////////////////////////////////
//Copyright (c) FRC Team 1736 2016. See the License file. 
//
//Can you use this code? Sure! We're releasing this under GNUV3, which 
//basically says you can take, modify, share, publish this as much as you
//want, as long as you don't make it closed source.
//
//If you do find it useful, we'd love to hear about it! Check us out at
//http://robotcasserole.org/ and leave us a message!
///////////////////////////////////////////////////////////////////////////////

/**
* DESCRIPTION:
* <br>
* Single Calibration. Describes a piece of data which is usually constant, but can
* be overridden by a cal wrangler from a csv file on the RIO's filesystem. This enables
* a software team to control what a pit crew has control over (Shooter speed is a good
* candidate. Port number for left drivetrain motor A is a bad candidate). 
* <br>
* <br>
* USAGE:    
* <ol>   
* <li>Instantiate a CalManager first (if not done yet)</li> 
* <li>Instantiate the calibration with a default value, and reference to the wrangler</li> 
* <li>At runtime, use the get() method to read the calibrated value. The returned value may change depending on what the wrangler has overwritten.</li>    
* </ol>
* 
* 
*/


public class Calibration {
	public final double default_val;
	public final String name;
	public volatile double cur_val;
	public volatile boolean overridden;
	public double max_cal;
	public double min_cal;
	
	/**
	 * Constructor for a new calibratable value.
	 * @param name_in String for the name of the calibration. Best to make it the same of the variable name. 
	 * @param default_val_in Default value for the calibration. Will keep this value unless the wrangler overwrites it.
	 * @param wrangler_in Reference to the wrangler which will control this calibration. 
	 */
	public Calibration(String name_in, double default_val_in){
		
		/*default stuff and stuff*/
		default_val = default_val_in;
		cur_val = default_val;
		name = name_in.trim();
		overridden = false;
		min_cal = Double.NEGATIVE_INFINITY;
		max_cal = Double.POSITIVE_INFINITY;
		
		commonConstructor();
	}
	
	/**
	 * Constructor for a new calibratable value with range limiting
	 * @param name_in String for the name of the calibration. Best to make it the same of the variable name. 
	 * @param default_val_in Default value for the calibration. Will keep this value unless the wrangler overwrites it.
	 * @param wrangler_in Reference to the wrangler which will control this calibration. 
	 * @param min_in Minimum allowable calibration value. If a user attempts to override the value outside this range, a WARNING: will be thrown and the calibrated value will be capped at the minimum.
	 * @param max_in Maximum allowable calibration value. If a user attempts to override the value outside this range, a WARNING: will be thrown and the calibrated value will be capped at the maximum.
	 */
	public Calibration(String name_in, double default_val_in, double min_in, double max_in){
		
		/*default stuff and stuff*/
		name = name_in.trim();
		min_cal = min_in;
		max_cal = max_in;
		
		default_val = limitRange(default_val_in);
		cur_val = default_val;
				
		commonConstructor();		
	}
	
	private void commonConstructor(){
		overridden = false;
		CalWrangler.register(this);
	}
	
	private double limitRange(double in){
		double temp;
		//Cross-check that default value is in-range
		if(in < min_cal){
			System.out.println("WARNING: Calibration: Requested value for " + name + " is too small. Setting value to minimum value of " + Double.toString(min_cal));
			temp = min_cal;
		} else if(in > max_cal){
			System.out.println("WARNING: Calibration: Requested value for " + name + " is too large. Setting value to maximum value of " + Double.toString(max_cal));
			temp = max_cal;
		} else {
			temp = in;
		}
		return temp;
	}
	
	
	/**
	 * Retrieve the present value of this calibration. This is the method to use whenever the calibratable value is to be read.
	 * @return Present value of the calibration
	 */
	public double get(){
		if(overridden)
			return cur_val;
		else
			return default_val;
	}
	
	/**
	 * Get the default value of the calibration. Will not be overridden by wrangler. Do not use unless you're certain you don't wan't 
	 * any updates the wrangler may apply to this calibration. 
	 * @return Default value for the calibration
	 */
	public double getDefault(){
		return default_val;
	}
	
	/**
	 * Set a new value to override the present calibration. Value will be limited to allowable min/max range for this calibration.
	 * @param val_in Value to set.
	 */
	public void setOverride(double val_in){
		double temp = limitRange(val_in);
		cur_val = temp;
		overridden = true;
	}
	
	/**
	 * Returns the calibration back to the default value.
	 */
	public void reset(){
		overridden = false;
		cur_val = default_val;
	}
	
	

}
