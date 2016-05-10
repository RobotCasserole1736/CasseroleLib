///////////////////////////////////////////////////////////////////////////////
// Copyright (c) FRC Team 1736 2015. All Rights Reserved.
///////////////////////////////////////////////////////////////////////////////
//
// CLASS NAME: I2CGyro
// DESCRIPTION: I2C Gyro class - driver for L3G4200D MEMS Gyro
// 				Includes filtering and descrete integral for angle calculation.
//
// NOTES: Multithreaded support. Initializing the gyro kicks off a periodic
//        read function which asynchronously reads from they gyro in the
//        background. 
//     
//        The periodic update function is designed to be called by this 
//        background thread. Each time it is called, it gets the latest value
//        from the gyro, performs any filtering required, and integrates
//        the gyro value to calculate an angle. Class-private variables are 
//        updated by this function. 
//
//
//  CHANGE HISTORY:
//      Jan 19 2015 - Chris G. 
//         - Created
//
//
///////////////////////////////////////////////////////////////////////////////


package org.usfirst.frc.team1736.robot;
import java.util.Arrays; //For median filter sorting
import java.util.TimerTask; //For multithreading support

import edu.wpi.first.wpilibj.I2C; //FRC's internal I2C libraries for the RoboRio
import edu.wpi.first.wpilibj.I2C.Port;


public class I2CGyro {
	
	///////////////////////////////////////////////////////////////////////////////
	//Class & Multithread objects
	///////////////////////////////////////////////////////////////////////////////
	
	//wpilibj object for an i2c device
	//We will name it gyro since it will reference the gyro
	I2C gyro; 
	
	//Internal object required for multi-threading
	I2CGyro m_gyro; 
	
	// Thread to run the periodic update function on 
	java.util.Timer timerThread; 

	//period between periodic function calls
	//in milliseconds
	double m_sample_period_ms = 10; 
	
	///////////////////////////////////////////////////////////////////////////////
	//I2C constants
	///////////////////////////////////////////////////////////////////////////////

	//I2C device address - the address that the gyro will respond to
	//This address assumes our gyro has SDO tied to Gnd.
	private static final int I2C_ADDR = 0b01101000; 
	
	//Gyro internal register addresses
	//These are the addreses of data that we care about
	//It is not a complete list, only the ones we care about.
	private static final int WHOAMI_REG_ADDR = 0x0F;
	private static final int CTRL_REG1_ADDR = 0x20;
	private static final int CTRL_REG2_ADDR = 0x21;
	private static final int CTRL_REG3_ADDR = 0x22;
	private static final int CTRL_REG4_ADDR = 0x23;
	private static final int CTRL_REG5_ADDR = 0x24;
	private static final int OUTZ_L_REG_ADDR = 0x2C;
	private static final int OUTZ_H_REG_ADDR = 0x2D;
	
	//desired gyro settings
	//See the datasheet, pages 29-43 to see exactly what each bit is doing.
	//The basic gist of what is accomplished with each write is specified below
	//but the datasheet will give you specifics on how it's accomplished.
	
	//Set output data rate to 400 and LPF cutoff to 25Hz
	//Enable only Z axis, and disable power down.
	//Yes, this is one of these devices which defaults to powered off.
	//It's stupid.
	private static final int CTRL_REG1_CONTENTS = 0b10011100;
	//enable High Pass Filter, cutoff at 0.5 Hz
	private static final int CTRL_REG2_CONTENTS = 0b00100110;
	//Disable all interrupt trigger pins
	private static final int CTRL_REG3_CONTENTS = 0b00000000;
	//Set Full-Scale range to 2000Deg/Sec
	private static final int CTRL_REG4_CONTENTS = 0b00100000;
	//Disable Onboard FIFO
	private static final int CTRL_REG5_CONTENTS = 0b00000000;
	
	//In order to use this variable, bitwise OR it with the first register
	//you want to read from, and then read multiple bytes.
	private static final int AUTO_INCRIMENT_REG_PTR_MASK = 0x80;
	
	//Expected contents of the WHOAMI register
	private static final byte WHOAMI_EXPECTED = (byte) 0b11010011;
	
	///////////////////////////////////////////////////////////////////////////////
	//Internal Calculations objects
	///////////////////////////////////////////////////////////////////////////////
	
	//The current angle, as measured by the gyroscope
	private volatile double angle;
	
	//Buffer for current and previous gyro values
	//[0] is current, [1] is the previous value, [2] is the previous previous value....
	//It is required that we store the last three gyro readings for our integration 
	// method. 
	private volatile double[] gyro_z_val_deg_per_sec = {0, 0, 0};
	
	
	//Number of reads to do on init
	public static final int GYRO_INIT_READS = 500;
	
	//Nominal Z value
	//This is populated at startup with the average value of all the reads we
	// take from the gyro.
	public short zero_motion_offset;
	
	//Max range before we declare the gyro overloaded
	private final short gyro_max_limit = 0x7FB0; //directly in bits, same scale as gyro's registers
	
	//Deadzone 
	public static final double gyro_deadzone = 0.1; //(in deg/sec)
	
	//Conversion factor from bits to degrees per sec
	private static final double degPerSecPerLSB = 0.07;
	
	//False when gyro values are not to be considered good
	//true when gyro values are known to be reasonable. I guess...
	private boolean gyro_read_status = false;
	
	//False when the gyro for sure isn't initalized
	//True when it is. If gyro not init'd, we won't even attempt to 
	//get values from it.
	private boolean gyro_initalized = false;
	
	private long system_time_at_last_call = 0;
	
	//To calculate the integral, we have a small state machine. In order to
	//make sure the state machine gets properly initalize, we will need to 
	//run some special code the first time the periodic update funciton is called.
	//This variable keeps track of whether we've called the code or not yet.
	//It starts false, and then gets set to true as soon as we've run the periodic
	//code once. It stays true until the software is restarted.
	private boolean periodic_called_once = false;
	
	//Gyro integration mode: 0 = linear interpolation, 1 = simpson's method
	private static final int integration_method = 1;
	
	//state for calculating simpsons method of numerical integration.
	//Since we need three samples, this keeps track of state in our really simple state machine that
	//ensures we have three samples before calculating the integral
	private volatile int cur_interrupt_state = 0;
	//0 means no data in buffers yet
	//1 means 1 datapoint in buffer.
	//2 means two pieces of data in buffer. Add a third and calculate output!
	
	
	///////////////////////////////////////////////////////////////////////////////
	//FIR Filter Calculations objects
	///////////////////////////////////////////////////////////////////////////////
	//
	// FIR Filters are something every ECE major learns about sometime during
	// their sophomore year of college. They teach you the digital version late on
	// too. To figure out why they do what they do takes a lot of math.
	//
	// I don't like math. So I'm not going to bother telling you about it in these
	// already absurdly long comments.
	//
	// Even the name is nasty. "FIR" means Finite Impulse Response, which is a 
	// whole graduate level course unto itself to explain what it means. 
	//
	// The happy conclusion to months of math is that if you take a weighted
	// average of some number of previous digital samples, you can cut out
	// some of the noise in your input signal. I'll try to focus on how
	// you efficently calculate that weighted average.
	//
	
	//This constant defines how many previous samples we will average.
	//The fancy-schmancy electrical engineering term for this number of samples
	//is "Filter Length"
	private static final int FILTER_LENGTH = 53;
	
	//We assume that our noise causes the signal to change really fast (high frequency)
	//and the signal we care about changes relatively slowly (low frequency)
	
	//These are the coefficents of the filter.
	//"Coefficents" is a fancy-schmancy term for the weights in the weighted average.
	//Notice how they're symmetric (first and last are the same, etc.). There's more math behind that!
	//
	//You have to pick these nubmbers very carefully to eliminate the high frequencies you don't want
	// but not damage the low frequencies you do want.
	//
	// And by "pick carefully" what I mean is go to some website like http://t-filter.appspot.com/fir/index.html
	// and put in the parameters you want for the filter
	// and copy and paste the numbers it gives you. 
	//
	// How does that website work you ask? Answer: Math.
	//
	//Anyhoo, these constants I got are for:
    //
	//53 tap FIR, sampling frequency of 50Hz
	//passband from 0 to 10 Hz, 4.00db ripple, unit gain
	//stopband from 12 to 25 Hz, -40.23db ripple, ideally zero gain
	private static final double[] FILTER_COEFS = {
		0.0023092487065988227,
		0.014096933201830508,
		0.01882863576981003,
		0.02638335286474602,
		0.029521978086505703,
		0.027134360076108553,
		0.018569615380437842,
		0.005598205417752613,
		-0.008101003513074865,
		-0.01797433887398908,
		-0.020258649100198693,
		-0.013651315575190996,
		-0.00020833142072014616,
		0.014965417634094582,
		0.025336496823333894,
		0.025436882849386642,
		0.01331851701527296,
		-0.007998829315667583,
		-0.03080398758687758,
		-0.044851058492634356,
		-0.040775023377656615,
		-0.013618934982137149,
		0.03477631720424106,
		0.09551396923065877,
		0.1548091828592016,
		0.19794181683885595,
		0.2137118638623725,
		0.19794181683885595,
		0.1548091828592016,
		0.09551396923065877,
		0.034776317204241076,
		-0.013618934982137149,
		-0.040775023377656615,
		-0.044851058492634356,
		-0.03080398758687758,
		-0.007998829315667583,
		0.01331851701527296,
		0.025436882849386642,
		0.025336496823333894,
		0.014965417634094582,
		-0.0002083314207201463,
		-0.013651315575190996,
		-0.020258649100198693,
		-0.017974338873989075,
		-0.008101003513074865,
		0.005598205417752613,
		0.018569615380437842,
		0.027134360076108553,
		0.029521978086505696,
		0.026383352864746017,
		0.01882863576981003,
		0.014096933201830508,
		0.0023092487065988227,
	};
	
	//To calculate the weighted average, we will use a "circular buffer" to hold the previous values of the gyroscope.
	//A circular buffer is a fancy schmancy term for an array where, when you try to index past one end of the array, it
	//just loops back to the opposite end. This is useful, for reasons we will see later.
	private double[] filter_circ_buffer = new double[FILTER_LENGTH];
	
	//Now, each time we read the gyro, we will want to place the new gyro value into the circular buffer
	//The newest value from the gyro will replace the oldest value in the buffer
	//and all other values will shift accordingly.
	//There is a processor efficency problem here:
	//If we were using a standard buffer, we would have to have a for loop to shift each value
	//within the buffer to the next location. This operation takes longer, depending on how
	//long the buffer is. This is generally undeseriable, especially considering the filter output
	//value is calculated very frequency. The more efficent we make this, the better.
	//
	//So, to do this, we introduce the "Circular buffer". It's like a queue. However think of it like this:
	// In a standard queue (waiting line), when we want to add someone new to it, we put them in the back of the line, and 
	// make everyone else shift one spot (discarding the oldest guy). But what if that waiting line was actually shaped like
	// a circle, and we just stuck a flag next to the person who was in the front of the line? That way, every time we have 
	// a new person to put into the line, we just move the "front of the line" flag toward the person who has been waiting 
	// the longest, kick the out of the line, and put the new guy by that flag. Now, the guy who's right next to the flag
	// is the newest to the line, the guy behind him is the second newest, and the guy in front of him is actually the oldest.
	//
	// Hopefully that made some sense. Anyway, that's what we're doing here. This next variable just represents that little
	// "Front of the line" flag. It's the index in the circular buffer that is the newest sample.
	private int filter_buf_start_pointer = 0;
	
	///////////////////////////////////////////////////////////////////////////////
	//Median Filter Calculations objects
	///////////////////////////////////////////////////////////////////////////////
	//An alternate to a FIR filter is the Median filter.
	//A median filter is good for when you usually get accurate samples from the gyro,
	//but occasionally there's a value or two which are really off.
	//
	//In the world of ECE, we call this "Shot noise". Not that you need to know that.
	//But if someone asks, the technically correct answer is that median filters are good
	// for getting rid of shot noise.
	//
	// Anyhoo.
	//
	// Median filters are pretty simple. Just store the previous X samples from the gyro,
	// and return the median (not average) of all those values. If you recall what the median
	// is, it's pretty easy to see why outliers on your input will never go to the output of a median filter.
	//
	//It's also nicer cuz the only thing to tune on the filter is "X", or how many previous samples 
	// you consider for your median calcualtion. That's this variable:
	//Median filter length
	private static final int MED_FILT_LEN= 10;
	
	//I'm assuming that MED_FILT_LEN is fairly small, so I didn't bother with a circular buffer on this one.
	//Although, it's perfectly reasonable to use one here.
	private double[] med_filt_buffer = new double[MED_FILT_LEN];
	
	//That's all the data! On to the constructor!
	
	
	///////////////////////////////////////////////////////////////////////////////
	//Gyro Constructor
	///////////////////////////////////////////////////////////////////////////////
	
	//Constructor initalizes the data associated with the gyro, starts talking to it over I2C
	//sets inital register values
	//reads from the gyro a few times to figure out the zero-offset value
	//sets the zero-offset value
	I2CGyro(){ 
		byte[] rx_byte = {0}; //temp variable to store a byte to transmit over I2C
		
		filter_buf_start_pointer = 0; //initalize the poitner to an aribtrary location. I like zero. It's circular, so it doesn't actually matter.
		
		//Call the wpilib's functions to define a new I2C port, using the onboard roboRIO one.
		//It's the one labeled "I2C" and has a diagram, not the one on the expansion port.
		gyro = new I2C(Port.kOnboard, I2C_ADDR); 
		 
		//Validate that we are actually plugged into a gyro by
		//reading the whoami register, and comparing to the expected value
		gyro.read(WHOAMI_REG_ADDR, 1, rx_byte);
		if(WHOAMI_EXPECTED != rx_byte[0]){
			System.out.println("ERROR: WhoAmI register mismatch for Gyro! Cannot Initalize!");
			gyro_initalized = false;
			return;
		}
		
		//Control register setup
		gyro.write(CTRL_REG1_ADDR, CTRL_REG1_CONTENTS);
		gyro.write(CTRL_REG2_ADDR, CTRL_REG2_CONTENTS);
		gyro.write(CTRL_REG3_ADDR, CTRL_REG3_CONTENTS);
		gyro.write(CTRL_REG4_ADDR, CTRL_REG4_CONTENTS);
		gyro.write(CTRL_REG5_ADDR, CTRL_REG5_CONTENTS);
		
		//sleep breifly to ensure settings take effect
		//probably not actually required, but doesn't hurt either
		try {
			Thread.sleep(10); //in ms
		} catch (InterruptedException e) { //java requires us to catch exceptions, don't know why exactly...
			System.out.println("ERROR YOU INTERRUPTED ME WHILE I'm SLEEPING!!! DO NOT WAKE THE ANGRY BEAST!!!!1!!!");
			e.printStackTrace();
		}
		
		//Read from the gyro a few times to get the zero-movement offset.
		//Be sure the robot doesn't get touched during this, or the gyro will
		//always have the wrong angle!!!
		System.out.print("Calibrating gyro, do not touch robot...");
		zero_motion_offset = 0;
		double gyro_zero_read_accumulator = 0;
		for(int i = 0; i < GYRO_INIT_READS; i++){
			gyro_zero_read_accumulator += (double)read_gyro_z_reg(); //add up all the reads
			try {
				Thread.sleep(10); //pause for 10ms between each read.
			} catch (InterruptedException e) {
				System.out.println("ERROR YOU INTERRUPTED ME WHILE I'm SLEEPING!!!");
				e.printStackTrace();
			}
		}
		//Calculate the average of all the reads by dividing their total sum by the number of reads
		zero_motion_offset = (short)((double)gyro_zero_read_accumulator/(double)GYRO_INIT_READS);
		System.out.println("Done! \nDetermined a zero-offset of " + zero_motion_offset); 
		
		//Kick off the multi-threaded stuff.
		//Will start calling the periodic update function at an interval of m_sample_period_ms,
		//asynchronously from any other code.
		//Java magic here, don't touch!
        timerThread = new java.util.Timer();
        timerThread.schedule(new GyroTask(this), 0L, (long) (m_sample_period_ms));
		
        
        gyro_initalized = true;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//Public methods
	///////////////////////////////////////////////////////////////////////////////
	
	//Must be synchronized due to multi-threaded stuff
	
	//Returns the most recent gyro reading in degrees per second
	public  double get_gyro_z(){
		if(gyro_initalized)
			return gyro_z_val_deg_per_sec[0];
		else
			return Double.NaN;
	}
	
	//returns the most recently calculated gyro angle in degrees
	//Angle can vary between -Infinity and Infinity, you must wrap this
	// to 0-360 if desired
	public  double get_gyro_angle(){
		if(gyro_initalized)
			return angle;
		else
			return Double.NaN;
		
	}
	
	//Resets the current angle of the gyro to zero
	public  void reset_gyro_angle(){
		angle = 0;		
	}
	
	public  boolean get_gyro_read_status(){
		return gyro_read_status;
	}
	
	public  boolean get_gyro_initalized(){
		return gyro_initalized;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//Private Methods
	///////////////////////////////////////////////////////////////////////////////
	
	//Initiates a request over I2C to get the Z-rotation data from teh gyro.
	//Assumes all I2C communication initialization has been already done.
	private  short read_gyro_z_reg(){
		byte[] buffer_low_and_high_bytes = {0, 0}; //buffer for I2C to read into
		byte[] buffer_config_test_vals = {0}; // buffer to test the config values to make sure we didn't hit brownout
		
		//test gyro status by first reading from the first config register. 
		gyro.read(CTRL_REG1_ADDR, 1, buffer_config_test_vals);
		if(buffer_config_test_vals[0] == (byte)CTRL_REG1_CONTENTS){
			//gyro is properly configured
			//read high and low bytes from I2C
			gyro.read(OUTZ_L_REG_ADDR|AUTO_INCRIMENT_REG_PTR_MASK, 2, buffer_low_and_high_bytes);
			gyro_read_status = true; //a good read has occured. probably.
		}
		else {//gyro read was bad. Set buffer to zeros and try to reinitalize
			gyro_read_status = false; //gyro value not ok!!!
			System.out.println("ERROR BAD READ FROM GYRO. Got " + buffer_config_test_vals[0] + " for CtrlReg1, expected " + CTRL_REG1_CONTENTS);
			System.out.println("attempting full I2C Reset...");
			//Control register setup
			gyro.write(CTRL_REG1_ADDR, CTRL_REG1_CONTENTS);
			gyro.write(CTRL_REG2_ADDR, CTRL_REG2_CONTENTS);
			gyro.write(CTRL_REG3_ADDR, CTRL_REG3_CONTENTS);
			gyro.write(CTRL_REG4_ADDR, CTRL_REG4_CONTENTS);
			gyro.write(CTRL_REG5_ADDR, CTRL_REG5_CONTENTS);
			buffer_low_and_high_bytes[0] = 0;
			buffer_low_and_high_bytes[1] = 0;
			
		}
		//The desired 16-bit reading is split into two eight-bit bytes in memory and over I2C com's. This line just recombines those
		//two bytes into a 16bit number, and applies the zero-motion-offset.
		//Typecasting magic here, don't touch!
		short ret_val = (short)(((buffer_low_and_high_bytes[1] << 8) | (buffer_low_and_high_bytes[0] & 0xFF)) - (short)zero_motion_offset);
		//Detect if the gyro has exceeded its measurement range
		//If so, print a debugging message.
		if(ret_val > gyro_max_limit || ret_val < -gyro_max_limit)
			System.out.println("!WARNING GYRO VALUE HAS OVERLOADED!!!!!!!!!!");
		return (ret_val); //return assembled 16-bit result
		
	}
	
	//This function is called at regular intervals by the multithreaded part of the gyro code.
	//It commands a gyro read, scales the raw data to degrees/sec, and then calculates the current angle
	// using the desired integration method
	@SuppressWarnings("unused") //suppress compiler warnings because I swear, we do actually use this function.
	public  void periodic_update() {
		long cur_period_start_time = System.nanoTime(); //Record the time the current sample is being taken at.
		//shift existing values
    	gyro_z_val_deg_per_sec[2] = gyro_z_val_deg_per_sec[1]; //note we discard the oldest sample
    	gyro_z_val_deg_per_sec[1] = gyro_z_val_deg_per_sec[0];
    	gyro_z_val_deg_per_sec[0] = gyro_median_filter((double)read_gyro_z_reg()*degPerSecPerLSB); //Read new value, scale, and add to gyro_z_vals array
    	//Apply deadzone to gyro reading
		if(gyro_z_val_deg_per_sec[0] < gyro_deadzone && gyro_z_val_deg_per_sec[0] > -gyro_deadzone)
			gyro_z_val_deg_per_sec[0] = 0;
	
		//If we're using simpsons method....
		if(integration_method == 1) {
		    if(cur_interrupt_state == 0) { //initalize variables on first call of asynchronous function
		    	system_time_at_last_call = cur_period_start_time; //record First sample time
		    	cur_interrupt_state = 1;
		    }
		    else if(cur_interrupt_state == 1) { //Wait for another sample
		    	cur_interrupt_state = 2;
		    }
		    else if(cur_interrupt_state == 2) { //We've got three samples, go ahead and calculate!
		    	long cur_period_ns = (cur_period_start_time - system_time_at_last_call); //Calculate the amount of time since the last time we calculated an integral
		    	//Integrate using simpsons method
				angle = angle + (double)cur_period_ns/(double)1000000000 * 1/6 * (gyro_z_val_deg_per_sec[2] + 4*gyro_z_val_deg_per_sec[1] + gyro_z_val_deg_per_sec[0]); //simpson's method
				//record current time for usage the next time we try to integrate
				system_time_at_last_call = cur_period_start_time;
				//move the state machine along.
				cur_interrupt_state = 1; 
		    }
		}
		//But if we're using a linear method...
		else if(integration_method == 0) {
			long cur_period_ns = (cur_period_start_time - system_time_at_last_call); //Calculate the amount of time since the last time we calculated an integral
			angle = angle + (double)cur_period_ns/(double)1000000000 * 1/2 * (gyro_z_val_deg_per_sec[0] + gyro_z_val_deg_per_sec[1]); //calculate integral using linear method
			system_time_at_last_call = cur_period_start_time; //record the current sample time for usage next time we calcualte an integral
		}
	}
	
	//Lowpass filter for gyro.
	//Shifts a new value into the circular buffer
	//outputs the current filter value (based on current and previous values given as input)
	@SuppressWarnings("unused")
	private  double gyro_LP_filter(double input){
		int circ_buffer_index = 0;
		double accumulator = 0;
		
		//Add the newest sample to the buffer, discarding the oldest sample
		filter_circ_buffer[filter_buf_start_pointer] = input;
		
		//iterate over the whole circular buffer
		//calculate the output as a weighted average of all the 
		//things currently in the buffer
		for(int i = 0; i < FILTER_LENGTH; i++){
			//wrap the net pointer to the proper range.
			//This is what makes the buffer "circular"
			if((filter_buf_start_pointer - i) >= 0) 
				circ_buffer_index = (filter_buf_start_pointer - i) % FILTER_LENGTH;
			else
				circ_buffer_index = ((filter_buf_start_pointer - i) % FILTER_LENGTH)+FILTER_LENGTH;
			
			//Add up all of the current buffer values, multiplied by the approprate weight (coefficent)
			accumulator += filter_circ_buffer[circ_buffer_index]*FILTER_COEFS[i]; 

		}
		
		//move the "starting flag"
		filter_buf_start_pointer = (filter_buf_start_pointer + 1) % FILTER_LENGTH ; //shift buffer
		return accumulator; //return filter value
	}
	
	//returns the median of some values. Pretty straightworward, figure it out yourself.
	private  double gyro_median_filter(double input){
		double[] sorted_array = new double[MED_FILT_LEN];
		
		//shift the buffer the really slow way.
		for(int i = MED_FILT_LEN-1; i > 0; i--) {
			med_filt_buffer[i] = med_filt_buffer[i-1];
			sorted_array[i] = med_filt_buffer[i-1];
		}
		med_filt_buffer[0] = input;
		sorted_array[0] = input;
		
		//sort the array
		Arrays.sort(sorted_array);
		
		//the median is either the middle value, or
		//the average of the two middle values in 
		//a sorted array.
	    int middle = sorted_array.length / 2;
	    if (sorted_array.length % 2 == 0)
	    {
	      double left = sorted_array[middle - 1];
	      double right = sorted_array[middle];
	      return (left + right) / 2;
	    }
	    else
	    {
	      return sorted_array[middle];
	    }
	
	}
	
	
	//Java multithreading magic. Do not touch.
	//Touching will incour the wrath of Cthulhu, god of java and gyros.
	//May the oceans of 1's and 0's rise to praise him.
    private class GyroTask extends TimerTask 
    {
        private I2CGyro m_gyro;

        public GyroTask(I2CGyro gyro) 
        {
            if (gyro == null) 
            {
                throw new NullPointerException("Given PIDController was null");
            }
            m_gyro = gyro;
        }

        @Override
        public void run() 
        {
            m_gyro.periodic_update();
        }
    }

}
