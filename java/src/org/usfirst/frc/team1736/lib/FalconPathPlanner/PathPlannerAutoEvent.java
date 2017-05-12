package org.usfirst.frc.team1736.lib.FalconPathPlanner;

/*
 *******************************************************************************************
 * Copyright (C) 2017 FRC Team 1736 Robot Casserole - www.robotcasserole.org
 *******************************************************************************************
 *
 * This software is released under the MIT Licence - see the license.txt
 *  file in the root of this repo.
 *
 * Non-legally-binding statement from Team 1736:
 *  Thank you for taking the time to read through our software! We hope you
 *   find it educational and informative! 
 *  Please feel free to snag our software for your own use in whatever project
 *   you have going on right now! We'd love to be able to help out! Shoot us 
 *   any questions you may have, all our contact info should be on our website
 *   (listed above).
 *  If you happen to end up using our software to make money, that is wonderful!
 *   Robot Casserole is always looking for more sponsors, so we'd be very appreciative
 *   if you would consider donating to our club to help further STEM education.
 */

import org.usfirst.frc.team1736.lib.AutoSequencer.AutoEvent;
import org.usfirst.frc.team1736.lib.CasserolePID.CasserolePID;
import org.usfirst.frc.team1736.robot.DriveTrain;
import org.usfirst.frc.team1736.robot.DriveTrainWheelSpeedPI;

/**
 * Interface into the Casserole autonomous sequencer for a path-planned traversal. Simply wraps
 * path-planner functionality into the AutoEvent abstract class.
 */

public class PathPlannerAutoEvent extends AutoEvent {

    /* Path planner wrapped by this auto event */
    public MecanumPathPlanner path;
    private double[][] waypoints;
    private double time_duration_s; 
    boolean pathCalculated;
    
    private static final double SIDEWAYS_FUDGE_FACTOR = 9.5/6.3333; //cuz mechnum and it's nearly midnight

    private int timestep;
    private double taskRate = 0.02;
    private double trackLength = 23.56 / 12;
    private double trackWidth = 24.75 / 12;
    private DriveTrainWheelSpeedPI leftFrontMotor;
    private DriveTrainWheelSpeedPI rightFrontMotor;
    private DriveTrainWheelSpeedPI leftRearMotor;
    private DriveTrainWheelSpeedPI rightRearMotor;
    /**
     * Constructor. Set up the parameters of the planner here.
     * 
     * @param waypoints_in Set of x/y points which define the path the robot should take.
     * @param timeAllowed_in Number of seconds the path traversal should take. Must be long enough
     *        to allow the path planner to output realistic speeds.
     * @param leftFrontMotor_in Reference to the PID which controls the left front wheel of the drivetrain.
     *        Presumes the .set() method accepts units of ft/sec.
     * @param rightFrontMotor_inReference to the PID which controls the right front wheel of the drivetrain.
     *        Presumes the .set() method accepts units of ft/sec.
     * @param leftRearMotor_in Reference to the PID which controls the left rear wheel of the drivetrain.
     *        Presumes the .set() method accepts units of ft/sec.
     * @param rightRearMotor_in Reference to the PID which controls the right rear wheel of the drivetrain.
     *        Presumes the .set() method accepts units of ft/sec.              
     */
    public PathPlannerAutoEvent(double[][] waypoints_in, double timeAllowed_in, DriveTrainWheelSpeedPI leftFrontMotor_in,
    		DriveTrainWheelSpeedPI rightFrontMotor_in, DriveTrainWheelSpeedPI leftRearMotor_in,DriveTrainWheelSpeedPI rightRearMotor_in) {        super();
        waypoints = waypoints_in;
        time_duration_s = timeAllowed_in;
        leftFrontMotor = leftFrontMotor_in;
        rightFrontMotor = rightFrontMotor_in;
        rightRearMotor = rightRearMotor_in;
        leftRearMotor = leftRearMotor_in;
        
        //"Fix" waypoints
        for(int i = 0; i < waypoints.length; i++)
        	waypoints[i][0] *= SIDEWAYS_FUDGE_FACTOR;
        
        path = new MecanumPathPlanner(waypoints);
        pathCalculated = false;
    }


    /**
     * On the first loop, calculates velocities needed to take the path specified. Later loops will
     * assign these velocities to the drivetrain at the proper time.
     */
    public void userUpdate() {
        if (pathCalculated == false) {
            path.calculate(time_duration_s, taskRate,trackWidth,trackLength);
            timestep = 0;
            pathCalculated = true;
        }
        
        //leftFrontMotor.setDesiredHeading(path.heading[timestep][1]-90); //-90 to match gyro orientation
        //rightFrontMotor.setDesiredHeading(path.heading[timestep][1]-90);
        //leftRearMotor.setDesiredHeading(path.heading[timestep][1]-90);
        //rightRearMotor.setDesiredHeading(path.heading[timestep][1]-90);
        
        leftFrontMotor.setDesiredHeading(0); //Hack. All path planning is done with translation.
        rightFrontMotor.setDesiredHeading(0);
        leftRearMotor.setDesiredHeading(0);
        rightRearMotor.setDesiredHeading(0);
        
        leftFrontMotor.setSetpoint(DriveTrain.FtPerSec_to_RPM(path.smoothLeftFrontVelocity[timestep][1]));
        rightFrontMotor.setSetpoint(DriveTrain.FtPerSec_to_RPM(path.smoothRightFrontVelocity[timestep][1]));
        leftRearMotor.setSetpoint(DriveTrain.FtPerSec_to_RPM(path.smoothLeftRearVelocity[timestep][1]));
        rightRearMotor.setSetpoint(DriveTrain.FtPerSec_to_RPM(path.smoothRightRearVelocity[timestep][1]));

        
        
        timestep++;
    }


    /**
     * Force both sides of the drivetrain to zero
     */
    public void userForceStop() {
        leftFrontMotor.setGyroCompEnabled(false);
        leftRearMotor.setGyroCompEnabled(false);
        rightFrontMotor.setGyroCompEnabled(false);
        rightRearMotor.setGyroCompEnabled(false);
    	
        leftFrontMotor.setSetpoint(0);
        rightFrontMotor.setSetpoint(0);
        leftRearMotor.setSetpoint(0);
        rightRearMotor.setSetpoint(0);
    }


    /**
     * Always returns true, since the routine should run as soon as it comes up in the list.
     */
    public boolean isTriggered() {
        return true; // we're always ready to go
    }


    /**
     * Returns true once we've run the whole path
     */
    public boolean isDone() {
        return timestep >= path.numFinalPoints;
    }


	@Override
	public void userStart() {
		path.calculate(time_duration_s, taskRate,trackWidth,trackLength);
        timestep = 0;
        pathCalculated = true;
        
        leftFrontMotor.setGyroCompEnabled(true);
        leftRearMotor.setGyroCompEnabled(true);
        rightFrontMotor.setGyroCompEnabled(true);
        rightRearMotor.setGyroCompEnabled(true);
        
        //rightFrontMotor.setGyroCompInverted(true);
        //rightRearMotor.setGyroCompInverted(true);
	}


}
