package org.usfirst.frc.team1736.robot;

import java.util.Timer;
import java.util.TimerTask;

import edu.wpi.first.wpilibj.ADXRS450_Gyro;

public class casserolePathAuto {

	//Path Planner Constants
	final double[][] waypoints_apchDfns = new double[][]{ // go up to defenses
		{0,0},
		{-4,0}
	};
	
	final double[][] waypoints_crsLwBr = new double[][]{ //cross low-bar defense
		{0,0},
		{-15,0}		
	};
	
	final double[][] waypoints_crossShootLow = new double[][]{ //cross and shoot
		{0,0},
		{-17.83,0},
		{-20.666,11.20555}
	};
	final double[][] waypoints_crossShootHigh = new double[][]{ //cross and shoot
		{0,0},
		{18.63,0},
		{19.4666,9.00555}
	};
	final double[][] waypoints_HighGoalGyro = new double[][]{ //cross and shoot?
		{0,0},
		{16.875,0},
		{20.46,6.207}//to add 6 inches, add 0.25 to x and 0.433 to y
	};
	
	
	
	//Gyro heading compensation gain
	private double headingCorrectionPGain;
	//Gyro fudge factor
	final double gyroFudgeFactor = 1.125; //9/8 cuz gyro is silly
	
	final double totalPathPlannerTime_apchDfns = 5;
	final double totalPathPlannerTime_crsLwBr = 5;
	final double totalPathPlannerTime_crossShootLow = 10;
	final double totalPathPlannerTime_crossShootHigh = 10;
	final double totalPathPlannerTime_HighGoalGyro = 10; //change this to 10 once real points are added
	
	final double PLANNER_SAMPLE_RATE_S = 0.02; //200ms update rate 
	final double ROBOT_TRACK_WIDTH_FT = 1.9; //1.9ft wide tracks
	
	//THESE MUST CHANGE IF THE SAMPLE TIME OF THE PLAYBACk THREAD CHANGES
	final int intakeLowerTimeStep_crossShootLow = 0;
	final int intakeRaiseTimeStep_crossShootLow = 250;
	//THESE MUST CHANGE IF THE SAMPLE TIME OF THE PLAYBACk THREAD CHANGES
	final int intakeLowerTimeStep_crossShootHigh = 15;
	final int intakeRaiseTimeStep_crossShootHigh = 175;
	//THESE MUST CHANGE IF THE SAMPLE TIME OF THE PLAYBACk THREAD CHANGES
/**TOM CHANGED THIS!!!!!!!!!!!!!! up there^^^            it was 250 and too late so it hit the wall       */  
	final int intakeLowerTimeStep_HighGoalGyro = 15;
	final int intakeRaiseTimeStep_HighGoalGyro = 250;
	int timestep = 0;
	
	FalconPathPlanner path;
	
	//Output Device - the drivetrain (and sometimes the shooter)
	DriveTrain dt;
	DriveMotorsPIDVelocity motors;
	IntakeLauncherStateMachine ilsm;
	ADXRS450_Gyro gyro;
	
	//Constraints
	boolean invertSetpoints = false;
	boolean cycleIntakeArm = false;
	
	//Playback thread
	Timer timerThread;
	boolean playbackActive = false;
	
	//End-of-path event variables
	boolean shootHighGoal = false;
	boolean shootLowGoal = false;
	edu.wpi.first.wpilibj.Timer shotTimer;
	final double HIGH_GOAL_SHOT_TIME_S = 3.0;
	final double LOW_GOAL_SHOT_TIME_S = 4.0;
	
	//State variables exposed to the outside
	public double angle_err_deg;
	public double pp_des_heading;
	
	
	/**
	 * Constructor
	 * 
	 */
	casserolePathAuto(DriveTrain dt_in, IntakeLauncherStateMachine ilsm_in, ADXRS450_Gyro gyro_in){
		dt = dt_in;
		ilsm = ilsm_in;
		gyro = gyro_in;
		shotTimer = new edu.wpi.first.wpilibj.Timer();
		shotTimer.reset();
		motors = new DriveMotorsPIDVelocity(dt);
		angle_err_deg = 0;
		pp_des_heading = 0;
	}
	
	
	/**
	 * Calc Path - must be run in auto_init to calculate a path based on 
	 * the selected auto mode.
	 * @param auto_mode
	 */
	public void calcPath(int auto_mode){
		if(auto_mode == 0){
			System.out.println("Calculating path Approach Defense");
			path = new FalconPathPlanner(waypoints_apchDfns);
			path.calculate(totalPathPlannerTime_apchDfns, PLANNER_SAMPLE_RATE_S, ROBOT_TRACK_WIDTH_FT);
			invertSetpoints = false;
			shootHighGoal = false;
			shootLowGoal = false;
			cycleIntakeArm = false;
			headingCorrectionPGain = 0.0;
		}
		else if(auto_mode == 1){
			System.out.println("Calculating path CrossLowBar");
			path = new FalconPathPlanner(waypoints_crsLwBr);
			path.calculate(totalPathPlannerTime_crsLwBr, PLANNER_SAMPLE_RATE_S, ROBOT_TRACK_WIDTH_FT);
			invertSetpoints = false;
			shootHighGoal = false;
			shootLowGoal = false;
			cycleIntakeArm = false;
			headingCorrectionPGain = 0.0;
		}
		else if(auto_mode == 2){
			System.out.println("Calculating path CrossShootLow");
			path = new FalconPathPlanner(waypoints_crossShootLow);
			path.setPathBeta(0.6);
			path.setPathAlpha(0.3);
			path.setVelocityAlpha(0.01);
			path.setVelocityBeta(0.8);
			path.calculate(totalPathPlannerTime_crossShootLow, PLANNER_SAMPLE_RATE_S, ROBOT_TRACK_WIDTH_FT);
			invertSetpoints = false;
			shootHighGoal = false;
			shootLowGoal = true;
			cycleIntakeArm = true;
			headingCorrectionPGain = 0.0;
		}
		else if(auto_mode == 3){
			System.out.println("Calculating path CrossShootHigh");
			path = new FalconPathPlanner(waypoints_crossShootHigh);
			path.setPathBeta(0.6);
			path.setPathAlpha(0.3);
			path.setVelocityAlpha(0.01);
			path.setVelocityBeta(0.8);
			path.calculate(totalPathPlannerTime_crossShootHigh, PLANNER_SAMPLE_RATE_S, ROBOT_TRACK_WIDTH_FT);
			invertSetpoints = true;
			shootHighGoal = true;
			shootLowGoal = false;
			cycleIntakeArm = true;
			headingCorrectionPGain = 0.0;
		}
		else if(auto_mode == 4){
			System.out.println("Calculating path HighGoalGyro");
			path = new FalconPathPlanner(waypoints_HighGoalGyro);
			path.setPathBeta(0.1);
			path.setPathAlpha(0.5);
			path.setVelocityAlpha(0.01);
			path.setVelocityBeta(0.8);
			path.calculate(totalPathPlannerTime_HighGoalGyro, PLANNER_SAMPLE_RATE_S, ROBOT_TRACK_WIDTH_FT);
			invertSetpoints = true;
			shootHighGoal = true; 
			shootLowGoal = false;
			cycleIntakeArm = true; 
			headingCorrectionPGain = 0.14; 
		}
		else{
			System.out.println("ERROR: bad path selected, tell software they did something wrong!!!");
		}
	}
	
	/**
	 * begins background thread commanding motor values through
	 * the determined path 
	 * @return
	 */
	public int startPlayback(){
		System.out.println("Starting Path Planner");
		dt.setSafetyEnabled(false);
		timestep = 0; //reset timestamp
		motors.lmpid.setSetpoint(0); //zero out motor controllers
		motors.rmpid.setSetpoint(0);
		motors.lmpid.enable(); //enable both drivetrain PID's
		motors.rmpid.enable();
		timerThread = new java.util.Timer(); //create new thread for the playback function
		shotTimer.reset(); //Make sure the shot timer is ready to be used (zeroed out)
		playbackActive = true; //Mark that playback has begun (or, will begin shortly)
		timerThread.schedule(new PathPlanningPlayback(this), 0L, (long) ((double)PLANNER_SAMPLE_RATE_S*1000)); //Kick off playback thread. Here we go!
		return 0;
	}
	
	/**
	 * Forcibly stops any background playback occurring
	 * @return
	 */
	public int stopPlayback(){
		System.out.println("Stopping Path Planner");
		if(timerThread != null)
			timerThread.cancel(); //kill thread, assuming it was running
		playbackActive = false; //set status to not running
		motors.lmpid.setSetpoint(0); //zero out motor controllers
		motors.rmpid.setSetpoint(0);
		//Don't disable the PID's yet, it's important to keep them alive so they actually stop the wheels from turning.
		shotTimer.stop(); //Stop and reset whatever shot timer might be running
		shotTimer.reset();
		timestep = 0; //reset time (just in case? probably not needed)
		ilsm.periodicStateMach(false, false, false, false, false); //shut everything down
		angle_err_deg = 0; //no more error!
		pp_des_heading = 0;
		return 0;
	}
	
	/**
	 * Returns true if playback is currently running, false if not.
	 * @return
	 */
	public boolean isPlaybackActive(){
		return playbackActive;
	}

	/**
	 * Playback function = should be called 
	 */
	public void plannerStep(){
		System.out.println("Running Planner Step " + timestep);
		//detect end condition where path planner has finished playback
		if(timestep >= path.numFinalPoints){
			if(timestep == path.numFinalPoints){
				shotTimer.start(); //only does something on the first call - make sure the shot timer is in fact running. Assumes it was reset at the start of path-planner auto
			}
			motors.lmpid.setSetpoint(0); //zero out motor controllers
			motors.rmpid.setSetpoint(0);
			if(shootHighGoal){ //high-goal end shot
				if(shotTimer.get() > HIGH_GOAL_SHOT_TIME_S){
					stopPlayback();
				} 
				else {
					System.out.println("Commanding High-Goal Shot, " + (HIGH_GOAL_SHOT_TIME_S-shotTimer.get()) + " s remaining.");
					ilsm.periodicStateMach(false, false, false, true, false); //command high-goal shot
				}
			}
			else if(shootLowGoal){ //low-goal end shot
				if(shotTimer.get() > LOW_GOAL_SHOT_TIME_S){
					stopPlayback();
				} 
				else {
					System.out.println("Commanding Low-Goal Shot, " + (LOW_GOAL_SHOT_TIME_S-shotTimer.get()) + " s remaining.");
					ilsm.periodicStateMach(false, true, false, false, false); //command low-goal shot (eject)
				}
			}
			else{ //no end shot
				stopPlayback(); 
			}
		}
		else{ //otherwise, continue playback
			double left_motor_vel;
			double right_motor_vel;
			//Calculate the heading error, and adjust the left/right assigned velocities based on error and the P gain
			//use proper inversion
			//Due to the way the path planner is returning angle for our given points, I have this hacky way of getting error. No idea if it works.
			//Not enought time to make everything right. Just need to make it work.
			if(invertSetpoints){
				pp_des_heading = path.heading[timestep][1]; //high goal shot
			} 
			else {
				pp_des_heading = 180.0 - path.heading[timestep][1]; //low-goal setpoints seem to cause heading to be backward and offset by 180 from what the gyro actually reads??
			}

			
			if(invertSetpoints){ //high goal shot
				if(gyro != null)
					angle_err_deg = (gyro.getAngle()*gyroFudgeFactor - path.heading[timestep][1]);
				else
					angle_err_deg = 0;
				left_motor_vel = -1*(path.smoothLeftVelocity[timestep][1] + angle_err_deg*headingCorrectionPGain);
				right_motor_vel = -1*(path.smoothRightVelocity[timestep][1] - angle_err_deg*headingCorrectionPGain);
			}
			else{
				if(gyro != null)
					angle_err_deg = (gyro.getAngle()*gyroFudgeFactor - path.heading[timestep][1]);
				else
					angle_err_deg = 0;
				left_motor_vel = path.smoothLeftVelocity[timestep][1] + angle_err_deg*headingCorrectionPGain;
				right_motor_vel = path.smoothRightVelocity[timestep][1] - angle_err_deg*headingCorrectionPGain;
			}
			
			//command the motors
			motors.lmpid.setSetpoint(left_motor_vel);
			motors.rmpid.setSetpoint(right_motor_vel);
			
			if(cycleIntakeArm){
				//Handle arm raise-lower events
				if(timestep == intakeLowerTimeStep_crossShootLow){
					System.out.println("Lowering Intake Arm");
					Pneumatics.intakeDown();
				}
				if(timestep == intakeRaiseTimeStep_crossShootLow){
					System.out.println("Raising Intake Arm");
					Pneumatics.intakeUp();
				}
				if(timestep == intakeLowerTimeStep_crossShootHigh){
					System.out.println("Lowering Intake Arm");
					Pneumatics.intakeDown();
				}
				if(timestep == intakeRaiseTimeStep_crossShootHigh){
					System.out.println("Raising Intake Arm");
					Pneumatics.intakeUp();
				}
			}
		}
		
		timestep++;
	}
	
	//Java multithreading magic. Do not touch.
	//Touching will incour the wrath of Cthulhu, god of java and path planning.
	//May the oceans of 1's and 0's rise to praise him.
    public class PathPlanningPlayback extends TimerTask 
    {
        private casserolePathAuto m_planner;

        public PathPlanningPlayback(casserolePathAuto planner) 
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
