package org.usfirst.frc.team1736.lib.FalconPathPlanner;

import org.usfirst.frc.team1736.lib.AutoSequencer.AutoEvent;
import org.usfirst.frc.team1736.lib.globalConstants.RobotConstants;
import org.usfirst.frc.team1736.robot.CasserolePID;

public class PathPlannerAutoEvent extends AutoEvent{

	public FalconPathPlanner path;
	private double[][] waypoints;
	private double time_duration_s;
	boolean pathCalculated;
	
	private int timestep;
	
	private CasserolePID leftDT;
	private CasserolePID rightDT;
	
	PathPlannerAutoEvent(double [][] waypoints_in, double timeAllowed_in, double robotTrackWidth, CasserolePID leftDT_in, CasserolePID rightDT_in) {
		super();
		waypoints = waypoints_in;
		time_duration_s = timeAllowed_in;
		leftDT = leftDT_in;
		rightDT = rightDT_in;
	}

	public void userInit(){
		path = new FalconPathPlanner(waypoints);
		pathCalculated = false;
	}
	
	public void userUpdate(){
		if(pathCalculated == false){
			path.calculate(time_duration_s, RobotConstants.MAIN_TASK_RATE_S, RobotConstants.DRIVETRAIN_WIDTH_FT);
			timestep = 0;
		}
		
		leftDT.setSetpoint(path.smoothLeftVelocity[timestep][1]);
		rightDT.setSetpoint(path.smoothRightVelocity[timestep][1]);
		
		timestep++;
		
	}
	
	public void userForceStop(){
		leftDT.setSetpoint(0);
		rightDT.setSetpoint(0);
	}
	
	public boolean isTriggered(){
		return true; //we're always ready to go
	}
	
	public boolean isDone(){
		return timestep >= path.numFinalPoints;
	}
	
	
}
