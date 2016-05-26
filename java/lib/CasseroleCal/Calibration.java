package lib.CasseroleCal;


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
* USAGE:    
* <ol>   
* <li>Instantiate a CalWrangler first (if not done yet)</li> 
* <li>Instantiate the calibration with a default value, and reference to the wrangler</li> 
* <li>At runtime, use the get() method to read the calibrated value. The returned value may change depending on what the wrangler has overwritten.</li>    
* </ol>
* 
* 
*/


public class Calibration {
	public final double default_val;
	private final CalWrangler wrangler;
	public final String name;
	public double cur_val;
	public boolean overridden;
	
	/**
	 * Constructor for a new calibratable value.
	 * @param name_in String for the name of the calibration. Best to make it the same of the variable name. 
	 * @param default_val_in Default value for the calibration. Will keep this value unless the wrangler overwrites it.
	 * @param wrangler_in Reference to the wrangler which will control this calibration. 
	 */
	Calibration(String name_in, double default_val_in, CalWrangler wrangler_in){
		
		/*default stuff and stuff*/
		default_val = default_val_in;
		wrangler = wrangler_in;
		name = name_in.trim();
		overridden = false;
		
		wrangler.register(this);
		
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
	
	

}
