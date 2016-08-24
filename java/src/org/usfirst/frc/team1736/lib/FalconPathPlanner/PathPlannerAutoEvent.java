package org.usfirst.frc.team1736.lib.FalconPathPlanner;

import org.usfirst.frc.team1736.lib.AutoSequencer.AutoEvent;
import org.usfirst.frc.team1736.lib.globalConstants.RobotConstants;
import org.usfirst.frc.team1736.robot.CasserolePID;

/**
 * Interface into the Casserole autonomous sequencer for a path-planned
 * traversal. Simply wraps path-planner functionality into the AutoEvent abstract
 * class.
 */

public class PathPlannerAutoEvent extends AutoEvent{

	/* Path planner wrapped by this auto event */
	public FalconPathPlanner path;
	private double[][] waypoints;
	private double time_duration_s;
	boolean pathCalculated;

	private int timestep;

	private CasserolePID leftDT;
	private CasserolePID rightDT;

	/**
	 * Constructor. Set up the parameters of the planner here.
	 * @param  waypoints_in Set of x/y points which define the path the robot should take.
	 * @param  timeAllowed_in Number of seconds the path traversal should take. Must be long enough to allow the path planner to output realistic speeds.
	 * @param  leftDT_in Reference to the PID which controls the left side of the drivetrain. Presumes the .set() method accepts units of ft/sec.
	 * @param  rightDT_inReference to the PID which controls the right side of the drivetrain. Presumes the .set() method accepts units of ft/sec.
   */
	public PathPlannerAutoEvent(double [][] waypoints_in, double timeAllowed_in, CasserolePID leftDT_in, CasserolePID rightDT_in) {
		super();
		waypoints = waypoints_in;
		time_duration_s = timeAllowed_in;
		leftDT = leftDT_in;
		rightDT = rightDT_in;
	}

  /**
	 * Initalizes the path planner with waypoints, but does not calculate a path yet
	 */
	public void userInit(){
		path = new FalconPathPlanner(waypoints);
		pathCalculated = false;
	}

  /**
	 * On the first loop, calculates velocities needed to take the path specified. Later loops
	 * will assign these velocities to the drivetrain at the proper time.
	 */
	public void userUpdate(){
		if(pathCalculated == false){
			path.calculate(time_duration_s, RobotConstants.MAIN_TASK_RATE_S, RobotConstants.DRIVETRAIN_WIDTH_FT);
			timestep = 0;
		}

		leftDT.setSetpoint(path.smoothLeftVelocity[timestep][1]);
		rightDT.setSetpoint(path.smoothRightVelocity[timestep][1]);

		timestep++;
	}

 /**
  * Force both sides of the drivetrain to zero
	*/
	public void userForceStop(){
		leftDT.setSetpoint(0);
		rightDT.setSetpoint(0);
	}


	/**
   * Always returns true, since the routine should run as soon as it comes up in the list.
 	*/
	public boolean isTriggered(){
		return true; //we're always ready to go
	}


	/**
   * Returns true once we've run the whole path
 	*/
	public boolean isDone(){
		return timestep >= path.numFinalPoints;
	}


}
