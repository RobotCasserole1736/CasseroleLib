package org.usfirst.frc.team1736.lib.PathPlanner;

import java.util.ArrayList;

public class CasserolePath {
	
	public ArrayList<double[]> waypoints = new ArrayList<double[]>(0);
	
	public String name = "New Autonomous Mode";
	public boolean invertSetpoints = false;
	
	public double path_execution_time_s = 15;
	
	/**
	 * Call this to add a new waypoint to the end of the path. 
	 * @param x_in X coordinate of waypoint
	 * @param y_in Y coordinate of waypoint
	 */
	public void addWaypoint(double x_in, double y_in){
		double[] temp = {x_in, y_in};
		waypoints.add(temp);
	}
	
	public double[][] getWaypointsAsArray(){
		return (double[][]) waypoints.toArray();
	}

}
