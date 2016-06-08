package org.usfirst.frc.team1736.lib.Hourmeter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread.State;
import java.util.TimerTask;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;


/**
 * Tracks robot runtime over its life. Records data in a special file on the USB Drive. Data is human-readable,
 * and may be merged manually if the USB drive is moved. Data is useful for determining how long various parts last,
 * 
 * @author gerthcm
 *
 */
public class CasseroleHourmeter {
	
	final String HOURMETER_FNAME = "/home/lvuser/hourmeter/runtime_data.txt";
	
	final int HOURMETER_UPDATE_RATE_MS = 5000;
	
	public double minutesTotal;
	public double minutesDisabled;
	public double minutesTeleop;
	public double minutesAutonomous;
	public double minutesTest;
	public double  numTeleopEnables;
	public double  numAutonomousEnables;
	public double  numTestEnables;
	
	public double prev_call_time_sec;
	public OperationState prev_state; 
	
	public boolean isRunning;
	
	DriverStation ds;
	
	private java.util.Timer updater;
	private HourmeterUpdater updater_task;
	
	
	/**
	 * Initialize the hourmeter. Will read from disk to get the most recent values for the hourmeter, and start updating the file periodically.
	 * If the file does not exist, it will be created initially. This is all you really need to do, unless you happen to want to read the numbers
	 * back over smartdashboard or something like that.
	 */
	public CasseroleHourmeter(){
		int ret_val;
		isRunning = false;
		
		updater = new java.util.Timer();
		updater_task = new HourmeterUpdater();
		
		ds = DriverStation.getInstance();
		
		prev_call_time_sec = Timer.getFPGATimestamp();
		prev_state = OperationState.UNKNOWN;
		
		//create new file if it doesn't exist, checking errors along the way.
		if(!checkHourmeterFileExists()){
			ret_val = makeHourmeterDirectories();
			if(ret_val == 0){
				ret_val = initNewHourmeterFile();
			}
		} else {
			ret_val = readCurrentValuesFromHourmeterFile();
		}
		
		// Presuming we were able to initialize the hourmeter, kick off the periodic update.
		if(ret_val != 0){
			System.out.println("ERROR: Cannot initialize hourmeter. Not starting it.");
		} else {
			updater.scheduleAtFixedRate(updater_task, 0, HOURMETER_UPDATE_RATE_MS);
			isRunning = true;
		}
	}
	
	/**
	 * Perform an immediate update of the hourmeter. This does add some overhead so use it sparingly. However, 
	 * it's good to use in any of the *_init methods to be sure the file is properly updated.
	 */
	public void immedeateUpdate(){
		//single-shot Call. Note this method must be synchronzied since it could be called from either the background
		//hourmeter thread or the main one.
		if(isRunning)
			updateHourmeterFile(); 
	}
	
	/**
	 * Verify the hourmeter file exists on the filesystem
	 * @return True if file exists, False if not.
	 */
	private boolean checkHourmeterFileExists(){
		File f = new File(HOURMETER_FNAME);
		if(f.exists() && !f.isDirectory()){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Creates directories for hourmeter file. Should be called during initial creation, before first write.
	 * @return 0 on directory-making success, -1 if directories were not created.
	 */
	
	private int makeHourmeterDirectories(){
		
		File tempFobj = new File(HOURMETER_FNAME);
		File tempPathObj = new File(tempFobj.getParent());
		if(tempPathObj.mkdirs()){
			return 0;
		} else {
			return -1;
		}
	}
	
	private int writeCurrentValuesToHourmeterFile(){
		try{
			//Open File
			FileWriter fstream = new FileWriter(HOURMETER_FNAME, false);
			BufferedWriter log_file = new BufferedWriter(fstream);
			
			//Write the lines. Changes here will need corresponding updates in the read function.
			log_file.write("TOTAL_MINUTES:"+Double.toString(minutesTotal)+"\n");
			log_file.write("DISABLED_MINUTES:"+Double.toString(minutesDisabled)+"\n");
			log_file.write("TELEOP_MINUTES:"+Double.toString(minutesTeleop)+"\n");
			log_file.write("AUTO_MINUTES:"+Double.toString(minutesAutonomous)+"\n");
			log_file.write("TEST_MINUTES:"+Double.toString(minutesTest)+"\n");
			log_file.write("TELEOP_ENABLES:"+Double.toString(numTeleopEnables)+"\n");
			log_file.write("AUTO_ENABLES:"+Double.toString(numAutonomousEnables)+"\n");
			log_file.write("TEST_ENABLES:"+Double.toString(numTestEnables)+"\n");
			
			log_file.close();
			
		} catch	(IOException e ){
			System.out.println("ERROR: cannot write to hourmeter file:" + e.getMessage());
			return -1;
		}
		return 0;


	}
	
	private int readCurrentValuesFromHourmeterFile(){
		double temp_double;
		try{
			//Open File
			FileReader fstream = new FileReader(HOURMETER_FNAME);
			BufferedReader log_file = new BufferedReader(fstream);
			
			//Read each line, error-checking as we go.
			temp_double = parseLineWithErrorcheck(log_file.readLine(), "TOTAL_MINUTES");
			if(temp_double < 0){
				log_file.close();
				return -1;
			} else {
				minutesTotal = temp_double;
			}
			
			temp_double = parseLineWithErrorcheck(log_file.readLine(), "DISABLED_MINUTES");
			if(temp_double < 0){
				log_file.close();
				return -1;
			} else {
				minutesDisabled = temp_double;
			}
			
			temp_double = parseLineWithErrorcheck(log_file.readLine(), "TELEOP_MINUTES");
			if(temp_double < 0){
				log_file.close();
				return -1;
			} else {
				minutesTeleop = temp_double;
			}
			
			temp_double = parseLineWithErrorcheck(log_file.readLine(), "AUTO_MINUTES");
			if(temp_double < 0){
				log_file.close();
				return -1;
			} else {
				minutesAutonomous = temp_double;
			}
			
			temp_double = parseLineWithErrorcheck(log_file.readLine(), "TEST_MINUTES");
			if(temp_double < 0){
				log_file.close();
				return -1;
			} else {
				minutesTest = temp_double;
			}
			
			temp_double = parseLineWithErrorcheck(log_file.readLine(), "TELEOP_ENABLES");
			if(temp_double < 0){
				log_file.close();
				return -1;
			} else {
				numTeleopEnables = temp_double;
			}
			
			temp_double = parseLineWithErrorcheck(log_file.readLine(), "AUTO_ENABLES");
			if(temp_double < 0){
				log_file.close();
				return -1;
			} else {
				numAutonomousEnables = temp_double;
			}
			
			temp_double = parseLineWithErrorcheck(log_file.readLine(), "TEST_ENABLES");
			if(temp_double < 0){
				log_file.close();
				return -1;
			} else {
				numTestEnables = temp_double;
			}
			
			log_file.close();
		} catch	(IOException e){
			System.out.println("ERROR: cannot read from hourmeter file:" + e.getMessage());
			return -1;
		}
		
		return 0;
	}
	
	/**
	 * Takes a string and an expected name, and parses the number out of it. Returns -1.0 if errors are found. -1 should be fine because all stored times are positive.
	 * @param line_array
	 * @param name
	 * @return
	 */
	private double parseLineWithErrorcheck(String line, String name){
		String[] working_array;
		try{
			working_array = line.split(":");
			if( working_array.length != 2 ||
				!working_array[0].equals(name)){
				System.out.println("ERROR: Cannot process line in hourmeter file. Looking for " + name);
				return -1.0;
			} else {
				double temp = Double.parseDouble(working_array[1]);
				return temp;
			}
		} catch(NumberFormatException|NullPointerException e)
		{
			return -1.0;
		}
	}
	
	
	/**
	 * This method will shut down the hourmeter operation. Not good for usual things, but if the file somehow gets corrupted, 
	 * it will keep the robot from getting bogged down or locked up on a bogus file.
	 */
	private void shutDownHourmeter(){
		isRunning = false;
		minutesTotal = -1;
		minutesDisabled = -1;
		minutesTeleop = -1;
		minutesAutonomous = -1;
		minutesTest = -1;
		numTeleopEnables = -1;
		numAutonomousEnables = -1;
		numTestEnables = -1;
		System.out.println("ERROR: Hourmeter has stopped.");
		this.updater.cancel();
		
	}
	
	/**
	 * This method will properly initialize the time/count totals and write the file to disk.
	 * @return
	 */
	private int initNewHourmeterFile(){
		minutesTotal = 0;
		minutesDisabled = 0;
		minutesTeleop = 0;
		minutesAutonomous = 0;
		minutesTest = 0;
		numTeleopEnables = 0;
		numAutonomousEnables = 0;
		numTestEnables = 0;
		
		return writeCurrentValuesToHourmeterFile();
		
	}
	
	private synchronized void updateHourmeterFile(){
		//Update hour & counts with previous call information
		double current_time_sec = Timer.getFPGATimestamp();
		double delta_time_min = (current_time_sec - prev_call_time_sec)/60;
		OperationState cur_state = OperationState.UNKNOWN;
		
		
		//Update total time
		minutesTotal += delta_time_min;
		
		//Update individual time values and present state
		if(ds.isEnabled()){
			if(ds.isOperatorControl()){
				minutesTeleop += delta_time_min;
				cur_state = OperationState.TELEOP;
			} else if (ds.isAutonomous()){
				minutesAutonomous += delta_time_min;
				cur_state = OperationState.AUTO;
			} else if(ds.isTest()){
				minutesTest += delta_time_min;
				cur_state = OperationState.TEST;
			}
		}
		else{
			minutesDisabled += delta_time_min;
			cur_state = OperationState.DISABLED;
		}
		
		//If we've changed operational state, record that
		if(cur_state != prev_state){
			if(cur_state == OperationState.TELEOP)
				numTeleopEnables++;
			else if(cur_state == OperationState.AUTO)
				numAutonomousEnables++;
			else if(cur_state == OperationState.TEST)
				numTestEnables++;	
		}
		
		//write all the updated variables to file
		if(writeCurrentValuesToHourmeterFile() != 0){
			//something weird happened, exit gracefully.
			shutDownHourmeter();
		}
		
		prev_state = cur_state;
		prev_call_time_sec = current_time_sec;
	}
	
	/**
	 * Class for timerTask which will wrapper the fixed-rate call
	 *
	 */
	private class HourmeterUpdater extends TimerTask {
		public void run() {
				updateHourmeterFile();	
		}
	}

}
