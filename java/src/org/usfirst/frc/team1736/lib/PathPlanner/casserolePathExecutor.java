package org.usfirst.frc.team1736.lib.PathPlanner;

import java.util.Timer;
import java.util.TimerTask;

import edu.wpi.first.wpilibj.ADXRS450_Gyro;

public abstract class casserolePathExecutor {

	CasserolePath[] paths;
	
	
	final double PLANNER_SAMPLE_RATE_S = 0.02; //200ms update rate 
	final double ROBOT_TRACK_WIDTH_FT = 1.9; //1.9ft wide tracks
	
	int timestep = 0;
	
	FalconPathPlanner path;
	
	//Playback thread
	Timer timerThread;
	boolean playbackActive = false;
	
	boolean setpoints_inverted;
	
	
	/**
	 * Constructor
	 * 
	 */
	casserolePathExecutor(int num_auto_modes){
		paths = new CasserolePath[num_auto_modes];
	}
	
	
	
	/**
	 * Calc Path - must be run in auto_init to calculate a path based on 
	 * the selected auto mode.
	 * @param auto_mode
	 */
	public void calcPath(int auto_mode){
		System.out.println("Calculating path INSERT SOMETHING HERE");
		
		setpoints_inverted = paths[auto_mode].invertSetpoints;
		path = new FalconPathPlanner(paths[auto_mode].getWaypointsAsArray());
		path.calculate(paths[auto_mode].path_execution_time_s, PLANNER_SAMPLE_RATE_S, ROBOT_TRACK_WIDTH_FT);
	}
	
	/**
	 * begins background thread commanding motor values through
	 * the determined path 
	 * @return
	 */
	public int startPlayback(){
		System.out.println("Starting Path Planner");
		startPlaybackCallback();
		timestep = 0; //reset timestamp
		timerThread = new java.util.Timer(); //create new thread for the playback function
		playbackActive = true; //Mark that playback has begun (or, will begin shortly)
		timerThread.schedule(new PathPlanningPlayback(this), 0L, (long) ((double)PLANNER_SAMPLE_RATE_S*1000)); //Kick off playback thread. Here we go!
		return 0;
	}
	
	public abstract void startPlaybackCallback();
	
	/**
	 * Forcibly stops any background playback occurring
	 * @return
	 */
	public int stopPlayback(){
		System.out.println("Stopping Path Planner");
		if(timerThread != null)
			timerThread.cancel(); //kill thread, assuming it was running
		playbackActive = false; //set status to not running
		//Don't disable the PID's yet, it's important to keep them alive so they actually stop the wheels from turning.
		timestep = 0; //reset time (just in case? probably not needed)
		stopPlaybackCallback();
		return 0;
	}
	
	/**
	 * This method gets called as soon as playback has finished. Use it to clean up any user-specific things needed for autonomous.
	 */
	public abstract void stopPlaybackCallback();
	
	/**
	 * Returns true if playback is currently running, false if not.
	 * @return state of playback.
	 */
	public boolean isPlaybackActive(){
		return playbackActive;
	}

	/**
	 * Will be called periodically to command the pathPlanner headings. 
	 */
	public void plannerStep(){
		System.out.println("Running Planner Step " + timestep);
		//detect end condition where path planner has finished playback
		if(timestep >= path.numFinalPoints){
			stopPlayback(); 
		}
		else{ //otherwise, continue playback
			double left_motor_vel;
			double right_motor_vel;
			double pp_des_heading;
			

			if(setpoints_inverted){ //high goal shot
				pp_des_heading = path.heading[timestep][1]; //high goal shot
				left_motor_vel = -1*(path.smoothLeftVelocity[timestep][1]);
				right_motor_vel = -1*(path.smoothRightVelocity[timestep][1]);
			}
			else{
				pp_des_heading = 180.0 - path.heading[timestep][1];
				left_motor_vel = path.smoothLeftVelocity[timestep][1];
				right_motor_vel = path.smoothRightVelocity[timestep][1];
			}
			
			setAutonomousDrivetrainSpeeds(left_motor_vel, right_motor_vel, pp_des_heading);
		}
		
		timestep++;
	}
	
	public abstract void setAutonomousDrivetrainSpeeds(double left_motor_vel, double right_motor_vel, double pp_des_heading);
	
	//Java multithreading magic. Do not touch.
	//Touching will incour the wrath of Cthulhu, god of java and path planning.
	//May the oceans of 1's and 0's rise to praise him.
    public class PathPlanningPlayback extends TimerTask 
    {
        private casserolePathExecutor m_planner;

        public PathPlanningPlayback(casserolePathExecutor planner) 
        {
            if (planner == null) 
            {
                throw new NullPointerException("Given PathPlanner was null");
            }
            m_planner = planner;
        }

        @Override
        public void run() 
        {
        	m_planner.plannerStep();
        }
    }
}
