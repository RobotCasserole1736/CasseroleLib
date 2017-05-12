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

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.util.LinkedList;
import java.util.List;



/**
 * This Class provides many useful algorithms for Robot Path Planning. It uses optimization techniques and knowledge
 * of Robot Motion in order to calculate smooth path trajectories, if given only discrete waypoints. The Benefit of these optimization
 * algorithms are very efficient path planning that can be used to Navigate in Real-time.
 * 
 * This Class uses a method of Gradient Decent, and other optimization techniques to produce smooth Velocity profiles
 * for the four wheels of a mecanum drive robot.
 * 
 * This Class does not attempt to calculate quintic or cubic splines for best fitting a curve. It is for this reason, the algorithm can be ran
 * on embedded devices with very quick computation times.
 * 
 * The output of this function are independent velocity profiles for the four wheels of a mecanum drive chassis. The velocity
 * profiles start and end with 0 velocity and maintain smooth transitions throughout the path. 
 * 
 * This algorithm is a port from a similar algorithm running on a Robot used for my PhD thesis. I have not fully optimized
 * these functions, so there is room for some improvement. 
 * 
 * Initial tests on the 2015 FRC NI RoboRio, the complete algorithm finishes in under 15ms using the Java System Timer for paths with less than 50 nodes. 
 * 
 * @author Kevin Harrilal
 * @email kevin@team2168.org
 * @version 1.0
 * @date 2014-Aug-11
 *
 */
public class MecanumPathPlanner
{
	
	/**
	   * The location of a motor on the robot for the purpose of determining velocity.
	   * The kNone "motor" denotes the center of the robot 
	   */
	public enum MotorType {
		kFrontLeft(0), kFrontRight(1), kRearLeft(2), kRearRight(3), kNone(4);
	
		public final int value;
	
		private MotorType(int value) {
			this.value = value;
		}
	}

	//Path Variables
	public double[][] origPath;
	public double[][] nodeOnlyPath;
	public double[][] smoothPath;
	public double[][] leftFrontPath;
	public double[][] leftRearPath;
	public double[][] rightFrontPath;
	public double[][] rightRearPath;

	//Orig Velocity
	public double[][] origCenterVelocity;
	public double[][] origLeftFrontVelocity;
	public double[][] origLeftRearVelocity;
	public double[][] origRightFrontVelocity;
	public double[][] origRightRearVelocity;

	//smooth velocity
	public double[][] smoothCenterVelocity;
	public double[][] smoothLeftFrontVelocity;
	public double[][] smoothLeftRearVelocity;
	public double[][] smoothRightFrontVelocity;
	public double[][] smoothRightRearVelocity;

	//accumulated heading
	public double[][] heading;

	double totalTime;
	double totalDistance;
	double numFinalPoints;

	double pathAlpha;
	double pathBeta;
	double pathTolerance;

	double velocityAlpha;
	double velocityBeta;
	double velocityTolerance;


	/**
	 * Constructor, takes a Path of Way Points defined as a double array of column vectors representing the global
	 * cartesian points of the path in {x,y} coordinates and a third heading coordinate (in degrees). The waypoints 
	 * are traveled from one point to the next in sequence. This is an expansion on the original SmoothPathPlanner
	 * for use with a mecanum drive robot.
	 * 
	 * For example: here is a properly formated waypoint array
	 * 
	 * double[][][] waypointPath = new double[][][]{
				{1, 1, 0},
				{5, 1, 0},
				{9, 12, 90},
				{12, 9, 90},
				{15, 6, 135},
				{15, 4, 135}
		};
		This path goes from {1,1} -> {5,1} -> {9,12} -> {12, 9} -> {15,6} -> {15,4}
		During the transition from {5,1} to {9,12} the robot rotates from a straight heading (relative to its start)
		to a 90 degree heading.  Between position {12,9} and {15,6} the robot rotates to a heading of 135 degrees.
		The units of these coordinates are position units assumed by the user (i.e inch, foot, meters) 
	 * @param path
	 */
	public MecanumPathPlanner(double[][] path)
	{
		this.origPath = doubleArrayCopy(path);

		//default values DO NOT MODIFY;
		pathAlpha = 0.7;
		pathBeta = 0.3;
		pathTolerance = 0.0000001;

		velocityAlpha = 0.1;
		velocityBeta = 0.3;
		velocityTolerance = 0.0000001;
	}

	public static void print(double[] path)
	{
		System.out.println("X: \t Y:");

		for(double u: path)
			System.out.println(u);
	}



	/**
	 * Prints Cartesian Coordinates to the System Output as Column Vectors in the Form X	Y	Z
	 * @param path
	 */
	public static void print(double[][] path)
	{
		System.out.println("X: \t Y: \t Z: ");

		for(double[] u: path)
			System.out.println(u[0] + "\t" + u[1] + "\t" + u[2]);
	}

	/**
	 * Performs a deep copy of a 2 Dimensional Array looping thorough each element in the 2D array
	 * 
	 * BigO: Order N x M
	 * @param arr
	 * @return
	 */
	public static double[][] doubleArrayCopy(double[][] arr)
	{

		//size first dimension of array
		double[][] temp = new double[arr.length][arr[0].length];

		for(int i=0; i<arr.length; i++)
		{
			//Resize second dimension of array
			temp[i] = new double[arr[i].length];

			//Copy Contents
			for(int j=0; j<arr[i].length; j++)
				temp[i][j] = arr[i][j];
		}

		return temp;

	}

	/**
	 * Method upsamples the Path by linear injection. The result providing more waypoints along the path.
	 * 
	 * BigO: Order N * injection#
	 * 
	 * @param orig
	 * @param numToInject
	 * @return
	 */
	public double[][] inject(double[][] orig, int numToInject)
	{
		double morePoints[][];

		//create extended 2 Dimensional array to hold additional points
		morePoints = new double[orig.length + ((numToInject)*(orig.length-1))][3];

		int index=0;

		//loop through original array
		for(int i=0; i<orig.length-1; i++)
		{
			//copy first
			morePoints[index][0] = orig[i][0];
			morePoints[index][1] = orig[i][1];
			morePoints[index][2] = orig[i][2];
			index++;

			for(int j=1; j<numToInject+1; j++)
			{
				//calculate intermediate x points between j and j+1 original points
				morePoints[index][0] = j*((orig[i+1][0]-orig[i][0])/(numToInject+1))+orig[i][0];

				//calculate intermediate y points  between j and j+1 original points
				morePoints[index][1] = j*((orig[i+1][1]-orig[i][1])/(numToInject+1))+orig[i][1];
				
				//calculate intermediate rotation points between j and j+1 original points
				morePoints[index][2] = j*((orig[i+1][2]-orig[i][2])/(numToInject+1))+orig[i][2];

				index++;
			}
		}

		//copy last
		morePoints[index][0] =orig[orig.length-1][0];
		morePoints[index][1] =orig[orig.length-1][1];
		morePoints[index][2] =orig[orig.length-1][2];
		index++;

		return morePoints;
	}


	/**
	 * Optimization algorithm, which optimizes the data points in path to create a smooth trajectory.
	 * This optimization uses gradient descent. While unlikely, it is possible for this algorithm to never
	 * converge. If this happens, try increasing the tolerance level.
	 * 
	 * BigO: N^x, where X is the number of of times the while loop iterates before tolerance is met. 
	 * 
	 * @param path
	 * @param weight_data
	 * @param weight_smooth
	 * @param tolerance
	 * @return
	 */
	public double[][] smoother(double[][] path, double weight_data, double weight_smooth, double tolerance)
	{

		//copy array
		double[][] newPath = doubleArrayCopy(path);

		double change = tolerance;
		while(change >= tolerance)
		{
			change = 0.0;
			for(int i=1; i<path.length-1; i++)
				for(int j=0; j<path[i].length; j++)
				{
					double aux = newPath[i][j];
					newPath[i][j] += weight_data * (path[i][j] - newPath[i][j]) + weight_smooth * (newPath[i-1][j] + newPath[i+1][j] - (2.0 * newPath[i][j]));
					change += Math.abs(aux - newPath[i][j]);	
				}					
		}

		return newPath;

	}

	/**
	 * reduces the path into only nodes which change direction. This allows the algorithm to know at what points
	 * the original WayPoint vector changes. The direction change can come through either directional or rotational
	 * changes of the mecanum drive.
	 * 
	 * BigO: Order N + Order M, Where N is length of original Path, and M is length of Nodes found in Path
	 * @param path
	 * @return
	 */
	public static double[][] nodeOnlyWayPoints(double[][] path)
	{

		List<double[]> li = new LinkedList<double[]>();

		//save first value
		li.add(path[0]);

		//find intermediate nodes
		for(int i=1; i<path.length-1; i++)
		{
			//calculate direction
			double vector1 = Math.atan2((path[i][1]-path[i-1][1]),path[i][0]-path[i-1][0]);
			double vector2 = Math.atan2((path[i+1][1]-path[i][1]),path[i+1][0]-path[i][0]);

			//determine if both vectors or heading have a change in direction
			if(Math.abs(vector2-vector1)>=0.01 || Math.abs(path[i+1][2]-path[i][2]) > 0)
				li.add(path[i]);
		}

		//save last
		li.add(path[path.length-1]);

		//re-write nodes into new 2D Array
		double[][] temp = new double[li.size()][3];

		for (int i = 0; i < li.size(); i++)
		{
			temp[i][0] = li.get(i)[0];
			temp[i][1] = li.get(i)[1];
			temp[i][2] = li.get(i)[2];
		}	

		return temp;
	}


	/**
	 * Returns Velocity as a double array. The First Column vector is time, based on the time step, the second vector 
	 * is the velocity magnitude.
	 * 
	 * Notes:
	 * Velocity magnitude is now going to have to be able to go negative with mecanum, as the wheels will need to reverse in a case of non-forward movement
	 * 
	 * 
	 * 
	 * BigO: order N
	 * @param smoothPath
	 * @param timeStep
	 * @param motor The motor location on the robot (or kNone for center)
	 * @return
	 */
	double[][] velocity(double[][] smoothPath, double timeStep, MotorType motor)
	{
		double[] dxdt = new double[smoothPath.length];
		double[] dydt = new double[smoothPath.length];
		double[][] velocity = new double[smoothPath.length][2];

		//set first instance to zero
		dxdt[0]=0;
		dydt[0]=0;
		velocity[0][0]=0;
		velocity[0][1]=0;
		heading[0][1]=0;

		for(int i=1; i<smoothPath.length; i++)
		{
			dxdt[i] = (smoothPath[i][0]-smoothPath[i-1][0])/timeStep;
			dydt[i] = (smoothPath[i][1]-smoothPath[i-1][1])/timeStep;

			//create time vector
			velocity[i][0]=velocity[i-1][0]+timeStep;
			heading[i][0]=heading[i-1][0]+timeStep;

			//calculate velocity - if calculating center velocity do it the old way,
			//otherwise multiply by [1,1] or [-1,1] force vector
			//(multiplication by one specified for clarity)
			if(motor == MotorType.kNone)
				velocity[i][1] = Math.sqrt(Math.pow(dxdt[i],2) + Math.pow(dydt[i],2));
			else if(motor == MotorType.kFrontLeft || motor == MotorType.kRearRight)
				velocity[i][1] = dxdt[i] * 1 + dydt[i] * 1;
			else
				velocity[i][1] = dxdt[i] * -1 + dydt[i] * 1;
		}
		
		return velocity;
	}
	
	/**
	 * optimize velocity by minimizing the error distance at the end of travel
	 * when this function converges, the fixed velocity vector will be smooth, start
	 * and end with 0 velocity, and travel the same final distance as the original
	 * un-smoothed velocity profile
	 * 
	 * This Algorithm may never converge. If this happens, reduce tolerance. 
	 * 
	 * @param smoothVelocity
	 * @param origVelocity
	 * @param tolerance
	 * @return
	 */
	double[][] velocityFix(double[][] smoothVelocity, double[][] origVelocity, double tolerance)
	{

		/*pseudo
		 * 1. Find Error Between Original Velocity and Smooth Velocity
		 * 2. Keep increasing the velocity between the first and last node of the smooth Velocity by a small amount
		 * 3. Recalculate the difference, stop if threshold is met or repeat step 2 until the final threshold is met.
		 * 3. Return the updated smoothVelocity
		 */

		//calculate error difference
		double[] difference = errorSum(origVelocity,smoothVelocity);


		//copy smooth velocity into new Vector
		double[][] fixVel = new double[smoothVelocity.length][2];

		for (int i=0; i<smoothVelocity.length; i++)
		{
			fixVel[i][0] = smoothVelocity[i][0];
			fixVel[i][1] = smoothVelocity[i][1];
		}

		//optimize velocity by minimizing the error distance at the end of travel
		//when this converges, the fixed velocity vector will be smooth, start
		//and end with 0 velocity, and travel the same final distance as the original
		//un-smoothed velocity profile
		double increase = 0.0;
		while (Math.abs(difference[difference.length-1]) > tolerance)
		{
			increase = difference[difference.length-1]/1/50;

			for(int i=1;i<fixVel.length-1; i++)
				fixVel[i][1] = fixVel[i][1] - increase;

			difference = errorSum(origVelocity,fixVel);
		}

		//fixVel =  smoother(fixVel, 0.001, 0.001, 0.0000001);
		return fixVel;

	}


	/**
	 * This method calculates the integral of the Smooth Velocity term and compares it to the Integral of the 
	 * original velocity term. In essence we are comparing the total distance by the original velocity path and 
	 * the smooth velocity path to ensure that as we modify the smooth Velocity it still covers the same distance 
	 * as was intended by the original velocity path.
	 * 
	 * BigO: Order N
	 * @param origVelocity
	 * @param smoothVelocity
	 * @return
	 */
	private double[] errorSum(double[][] origVelocity, double[][] smoothVelocity)
	{
		//copy vectors
		double[] tempOrigDist = new double[origVelocity.length];
		double[] tempSmoothDist = new double[smoothVelocity.length];
		double[] difference = new double[smoothVelocity.length];


		double timeStep = origVelocity[1][0]-origVelocity[0][0];

		//copy first elements
		tempOrigDist[0] = origVelocity[0][1];
		tempSmoothDist[0] = smoothVelocity[0][1];


		//calculate difference
		for (int i=1; i<origVelocity.length; i++)
		{
			tempOrigDist[i] = origVelocity[i][1]*timeStep + tempOrigDist[i-1];
			tempSmoothDist[i] = smoothVelocity[i][1]*timeStep + tempSmoothDist[i-1];

			difference[i] = tempSmoothDist[i]-tempOrigDist[i];

		}

		return difference;
	}
	/**
	 * This method calculates the optimal parameters for determining what amount of nodes to inject into the path
	 * to meet the time restraint. This approach uses an iterative process to inject and smooth, yielding more desirable
	 * results for the final smooth path.
	 * 
	 * Big O: Constant Time
	 * 	
	 * @param numNodeOnlyPoints
	 * @param maxTimeToComplete
	 * @param timeStep
	 */
	public int[] injectionCounter2Steps(double numNodeOnlyPoints, double maxTimeToComplete, double timeStep)
	{
		int first = 0;
		int second = 0;
		int third = 0;

		double oldPointsTotal = 0;

		numFinalPoints  = 0;

		int[] ret = null;

		double totalPoints = maxTimeToComplete/timeStep;

		if (totalPoints < 100)
		{
			double pointsFirst = 0;
			double pointsTotal = 0;


			for (int i=4; i<=6; i++)
				for (int j=1; j<=8; j++)
				{
					pointsFirst = i *(numNodeOnlyPoints-1) + numNodeOnlyPoints;
					pointsTotal = (j*(pointsFirst-1)+pointsFirst);

					if(pointsTotal<=totalPoints && pointsTotal>oldPointsTotal)
					{
						first=i;
						second=j;
						numFinalPoints=pointsTotal;
						oldPointsTotal=pointsTotal;
					}
				}

			ret = new int[] {first, second, third};
		}
		else
		{

			double pointsFirst = 0;
			double pointsSecond = 0;
			double pointsTotal = 0;
			double maxPoints = 0;

			for (int i=1; i<=8; i++)
				for (int j=1; j<=10; j++)
					for (int k=1; k<10; k++)
					{
						pointsFirst = i *(numNodeOnlyPoints-1) + numNodeOnlyPoints;
						pointsSecond = (j*(pointsFirst-1)+pointsFirst);
						pointsTotal =  (k*(pointsSecond-1)+pointsSecond);

						if(pointsTotal<=totalPoints && pointsTotal > maxPoints)
						{
							first=i;
							second=j;
							third=k;
							numFinalPoints=pointsTotal;
							maxPoints = pointsTotal;
						}
					}

			ret = new int[] {first, second, third};
		}


		return ret;
	}
/**
 * Calculates all wheel paths based on robot track width and length
 * 
 * Big O: 2N
 * 
 * @param smoothPath - center smooth path of robot
 * @param robotTrackWidth - width between left and right wheels of robot of mecanum chassis.
 * @param robotTrackLength - length from front to back of robot mecanum chassis. 
 */
	public void calcWheelPaths(double[][] smoothPath, double robotTrackWidth, double robotTrackLength)
	{
		double[][] leftFrontPath = new double[smoothPath.length][2];
		double[][] leftRearPath = new double[smoothPath.length][2];
		double[][] rightFrontPath = new double[smoothPath.length][2];
		double[][] rightRearPath = new double[smoothPath.length][2];

		double[][] gradient = new double[smoothPath.length][2];

		for(int i = 0; i<smoothPath.length-1; i++)
			gradient[i][1] = Math.atan2(smoothPath[i+1][1] - smoothPath[i][1],smoothPath[i+1][0] - smoothPath[i][0]);

		gradient[gradient.length-1][1] = gradient[gradient.length-2][1];


		for (int i=0; i<gradient.length; i++)
		{
			double headingRad = Math.toRadians(smoothPath[i][2]);
			double sin = Math.sin(headingRad);
			double cos = Math.cos(headingRad);
			
			leftFrontPath[i][0] = -robotTrackWidth/2 * cos - robotTrackLength/2 * sin + smoothPath[i][0];
			leftFrontPath[i][1] = -robotTrackWidth/2 * sin + robotTrackLength/2 * cos + smoothPath[i][1];
			
			leftRearPath[i][0] = -robotTrackWidth/2 * cos - -robotTrackLength/2 * sin + smoothPath[i][0];
			leftRearPath[i][1] = -robotTrackWidth/2 * sin + -robotTrackLength/2 * cos + smoothPath[i][1];
			
			rightFrontPath[i][0] = robotTrackWidth/2 * cos - robotTrackLength/2 * sin + smoothPath[i][0];
			rightFrontPath[i][1] = robotTrackWidth/2 * sin + robotTrackLength/2 * cos + smoothPath[i][1];
			
			rightRearPath[i][0] = robotTrackWidth/2 * cos - -robotTrackLength/2 * sin + smoothPath[i][0];
			rightRearPath[i][1] = robotTrackWidth/2 * sin + -robotTrackLength/2 * cos + smoothPath[i][1];

			//convert to degrees 0 to 360 where 0 degrees is +X - axis, accumulated to align with WPI sensor
			double deg = Math.toDegrees(gradient[i][1]);

			gradient[i][1] = smoothPath[i][2] + 90;

			if(i>0)
			{
				if((deg-gradient[i-1][1])>180)
					gradient[i][1] = -360+deg;

				if((deg-gradient[i-1][1])<-180)
					gradient[i][1] = 360+deg;
			}
		}

		this.heading = gradient;
		this.leftFrontPath = leftFrontPath;
		this.leftRearPath = leftRearPath;
		this.rightFrontPath = rightFrontPath;
		this.rightRearPath = rightRearPath;
	}
	
	/**
	 * Returns the first column of a 2D array of doubles
	 * @param arr 2D array of doubles
	 * @return array of doubles representing the 1st column of the initial parameter
	 */

	public static double[] getXVector(double[][] arr)
	{
		double[] temp = new double[arr.length];

		for(int i=0; i<temp.length; i++)
			temp[i] = arr[i][0];

		return temp;		
	}

	/**
	 * Returns the second column of a 2D array of doubles
	 * 
	 * @param arr 2D array of doubles
	 * @return array of doubles representing the 1st column of the initial parameter
	 */
	public static double[] getYVector(double[][] arr)
	{
		double[] temp = new double[arr.length];

		for(int i=0; i<temp.length; i++)
			temp[i] = arr[i][1];

		return temp;		
	}
	
	/**
	 * Returns the third column of a 2D array of doubles
	 * 
	 * @param arr 2D array of doubles
	 * @return array of doubles representing the 1st column of the initial parameter
	 */
	public static double[] getZVector(double[][] arr)
	{
		double[] temp = new double[arr.length];

		for(int i=0; i<temp.length; i++)
			temp[i] = arr[i][2];

		return temp;		
	}

	public static double[][] transposeVector(double[][] arr)
	{
		double[][] temp = new double[arr[0].length][arr.length];

		for(int i=0; i<temp.length; i++)
			for(int j=0; j<temp[i].length; j++)
				temp[i][j] = arr[j][i];

		return temp;		
	}

	public void setPathAlpha(double alpha)
	{
		pathAlpha = alpha;
	}

	public void setPathBeta(double beta)
	{
		pathBeta = beta;
	}

	public void setPathTolerance(double tolerance)
	{
		pathTolerance = tolerance;
	}
	
	public void setVelocityAlpha(double alpha)
	{
		velocityAlpha = alpha;
	}
	
	public void setVelocityBeta(double beta)
	{
		velocityBeta = beta;
	}

	/**
	 * This code will calculate a smooth path based on the program parameters. If the user doesn't set any parameters, the will use the defaults optimized for most cases. The results will be saved into the corresponding
	 * class members. The user can then access .smoothPath, .leftFrontPath, .leftRearPath, .rightFrontPath, .rightRearPath, .smoothCenterVelocity, .smoothRightFrontVelocity, .smoothLeftFrontVelocity, 
	 * .smoothRightRearVelocity, .smoothLeftRearVelocity as needed.
	 * 
	 * After calling this method, the user only needs to pass .smoothRightFrontVelocity[1], .smoothRightRearVelocity[1], .smoothLeftFrontVelocity[1], and .smoothLeftRearVelocity[1] 
	 * to the corresponding speed controllers on the Robot, and step through each setPoint.
	 * 
	 * @param totalTime - time the user wishes to complete the path in seconds. (this is the maximum amount of time the robot is allowed to take to traverse the path.)
	 * @param timeStep - the frequency at which the robot controller is running on the robot. 
	 * @param robotTrackWidth - distance between left and right side wheels of a mecanum drive chassis. Known as the track width.
	 * @param robotTrackLength - distance between front and rear wheels of a mecanum drive chassis. Known as track length.
	 */
	public void calculate(double totalTime, double timeStep, double robotTrackWidth, double robotTrackLength)
	{
		/**
		 * pseudo code
		 * 
		 * 1. Reduce input waypoints to only essential (direction changing) node points
		 * 2. Calculate how many total datapoints we need to satisfy the controller for "playback"
		 * 3. Simultaneously inject and smooth the path until we end up with a smooth path with required number 
		 *    of datapoints, and which follows the waypoint path.
		 * 4. Calculate all wheel paths by calculating four points at each datapoint 
		 */


		//first find only direction changing nodes
		nodeOnlyPath = nodeOnlyWayPoints(origPath);

		//Figure out how many nodes to inject
		int[] inject = injectionCounter2Steps(nodeOnlyPath.length, totalTime, timeStep);

		//iteratively inject and smooth the path
		for(int i=0; i<inject.length; i++)
		{
			if(i==0)
			{
				smoothPath = inject(nodeOnlyPath,inject[0]);
				smoothPath = smoother(smoothPath, pathAlpha, pathBeta, pathTolerance);	
			}
			else
			{
				smoothPath = inject(smoothPath,inject[i]);
				smoothPath = smoother(smoothPath, 0.1, 0.3, 0.0000001);	
			}
		}

		//calculate mecanum wheel paths based on center path
		calcWheelPaths(smoothPath, robotTrackWidth, robotTrackLength);

		origCenterVelocity = velocity(smoothPath, timeStep, MotorType.kNone);
		origLeftFrontVelocity = velocity(leftFrontPath, timeStep, MotorType.kFrontLeft);
		origLeftRearVelocity = velocity(leftRearPath, timeStep, MotorType.kRearLeft);
		origRightFrontVelocity = velocity(rightFrontPath, timeStep, MotorType.kFrontRight);
		origRightRearVelocity = velocity(rightRearPath, timeStep, MotorType.kRearRight);

		//copy smooth velocities into fix Velocities
		smoothCenterVelocity =  doubleArrayCopy(origCenterVelocity);
		smoothLeftFrontVelocity =  doubleArrayCopy(origLeftFrontVelocity);
		smoothLeftRearVelocity = doubleArrayCopy(origLeftRearVelocity);
		smoothRightFrontVelocity =  doubleArrayCopy(origRightFrontVelocity);
		smoothRightRearVelocity = doubleArrayCopy(origRightRearVelocity);

		//set final vel to zero
		smoothCenterVelocity[smoothCenterVelocity.length-1][1] = 0.0;
		smoothLeftFrontVelocity[smoothLeftFrontVelocity.length-1][1] = 0.0;
		smoothLeftRearVelocity[smoothLeftRearVelocity.length-1][1] = 0.0;
		smoothRightFrontVelocity[smoothRightFrontVelocity.length-1][1] = 0.0;
		smoothRightRearVelocity[smoothLeftFrontVelocity.length-1][1] = 0.0;

		//Smooth velocity with zero final V
		smoothCenterVelocity = smoother(smoothCenterVelocity, velocityAlpha, velocityBeta, velocityTolerance);
		smoothLeftFrontVelocity = smoother(smoothLeftFrontVelocity, velocityAlpha, velocityBeta, velocityTolerance);
		smoothLeftRearVelocity = smoother(smoothLeftRearVelocity, velocityAlpha, velocityBeta, velocityTolerance);
		smoothRightFrontVelocity = smoother(smoothRightFrontVelocity,velocityAlpha, velocityBeta, velocityTolerance);
		smoothRightRearVelocity = smoother(smoothRightRearVelocity, velocityAlpha, velocityBeta, velocityTolerance);

		//fix velocity distance error
		smoothCenterVelocity = velocityFix(smoothCenterVelocity, origCenterVelocity, 0.0000001);
		smoothLeftFrontVelocity = velocityFix(smoothLeftFrontVelocity, origLeftFrontVelocity, 0.0000001);
		smoothLeftRearVelocity = velocityFix(smoothLeftRearVelocity, origLeftRearVelocity, 0.0000001);
		smoothRightFrontVelocity = velocityFix(smoothRightFrontVelocity, origRightFrontVelocity, 0.0000001);
		smoothRightRearVelocity = velocityFix(smoothRightRearVelocity, origRightRearVelocity, 0.0000001);
	}
	
	public double[][] transformHeadingToGyroFrameOfRef(double[][] input){
		double[][] output = new double[input.length][input[0].length];
		
		for(int i = 0; i < input.length; i++){
			output[i][0] = input[i][0];
			output[i][1] = input[i][1] - 90;
		}
		
		return output;
		
	}

	//main program
	public static void main(String[] args)
	{
		long start = System.currentTimeMillis();
		//System.setProperty("java.awt.headless", "true"); //enable this to true to emulate roboRio environment

		//create waypoint path of a fairly practical example
		double[][] waypoints = new double[][]{
			{0,0,0},
			{-5,0,0},
			{-5,2,0},
			{-5,8,0},
			{-5,10,0},
			{0,10,0},
			{8,10,0}
		};

		double totalTime = 6.5; //seconds
		double timeStep = 0.02; //period of control loop on Rio, seconds
		double robotTrackWidth = 2; //distance between left and right wheels, feet
		double robotTrackLength = 2.5; //distance between front and rear wheels, feet

		final MecanumPathPlanner path = new MecanumPathPlanner(waypoints);
		path.calculate(totalTime, timeStep, robotTrackWidth, robotTrackLength);

		System.out.println("Time in ms: " + (System.currentTimeMillis()-start));

		if(!GraphicsEnvironment.isHeadless())
		{

			FalconLinePlot fig2 = new FalconLinePlot(path.smoothCenterVelocity,null,Color.blue);
			fig2.yGridOn();
			fig2.xGridOn();
			fig2.setYLabel("Velocity (ft/sec)");
			fig2.setXLabel("time (seconds)");
			fig2.setTitle("Velocity Profile for Left and Right Wheels \n LF = Cyan, RF = Magenta, LR = Green, RR = Orange");
			fig2.addData(path.smoothRightFrontVelocity, Color.magenta);
			fig2.addData(path.smoothLeftFrontVelocity, Color.cyan);
			fig2.addData(path.smoothRightRearVelocity, Color.orange);
			fig2.addData(path.smoothLeftRearVelocity, Color.green);

			FalconLinePlot fig1 = new FalconLinePlot(path.nodeOnlyPath,Color.blue,Color.green);
			fig1.yGridOn();
			fig1.xGridOn();
			fig1.setYLabel("Y (feet)");
			fig1.setXLabel("X (feet)");
			fig1.setTitle("Top Down View of FRC Field (24ft x 27ft) \n shows global position of robot path, along with leftFront, leftRear, rightFront, and rightRear wheel trajectories");

			//force graph to show 1/2 field dimensions of 24ft x 27 feet
			fig1.setXTic(0, 30, 1);
			fig1.setYTic(0, 30, 1);
			fig1.addData(path.smoothPath, Color.red, Color.blue);


			fig1.addData(path.leftFrontPath, Color.cyan);
			fig1.addData(path.leftRearPath, Color.green);
			fig1.addData(path.rightFrontPath, Color.magenta);
			fig1.addData(path.rightRearPath, Color.orange);
			
			
			FalconLinePlot fig3 = new FalconLinePlot(path.transformHeadingToGyroFrameOfRef(path.heading),null,Color.blue);
			
			fig3.yGridOn();
			fig3.xGridOn();
			fig3.setYLabel("des heading (deg)");
			fig3.setXLabel("time (seconds)");


			//generate figure 8 path
			path.figure8Example();

		}
		//example on printing useful path information
		//System.out.println(path.numFinalPoints);
		//System.out.println(path.pathAlpha);
	}

	public void figure8Example()
	{
		/***Sweet Figure 8 with rotation example (not practical at all)***/
		//Normally you would use a 0 heading and drive in +y for forward, but this fits on the field plot better

		double[][] waypoints = new double[][]{
			{0,0,0},
			{-5,0,0},
			{-5,-2,0},
			{-5,-8,0},
			{-5,-10,0},
			{0,-10,0},
			{8,-10,0}
		};

		double totalTime = 6.5; //seconds
		double timeStep = 0.02; //period of control loop on Rio, seconds
		double robotTrackWidth = 2; //distance between left and right wheels, feet
		double robotTrackLength = 2.5; //distance between front and rear wheels, feet

		final MecanumPathPlanner path = new MecanumPathPlanner(waypoints);
		path.setPathAlpha(0.9);
		path.setPathBeta(0.5);
		path.calculate(totalTime, timeStep, robotTrackWidth, robotTrackLength);

		if(!GraphicsEnvironment.isHeadless())
		{

			FalconLinePlot fig4 = new FalconLinePlot(path.smoothCenterVelocity,null,Color.blue);
			fig4.yGridOn();
			fig4.xGridOn();
			fig4.setYLabel("Velocity (ft/sec)");
			fig4.setXLabel("time (seconds)");
			fig4.setTitle("Velocity Profile for Left and Right Wheels \n LF = Cyan, RF = Magenta, LR = Green, RR = Orange");
			fig4.addData(path.smoothRightFrontVelocity, Color.magenta);
			fig4.addData(path.smoothLeftFrontVelocity, Color.cyan);
			fig4.addData(path.smoothRightRearVelocity, Color.orange);
			fig4.addData(path.smoothLeftRearVelocity, Color.green);

			FalconLinePlot fig5 = new FalconLinePlot(path.nodeOnlyPath,Color.blue,Color.green);
			fig5.yGridOn();
			fig5.xGridOn();
			fig5.setYLabel("Y (feet)");
			fig5.setXLabel("X (feet)");
			fig5.setTitle("Top Down View of FRC Field (24ft x 27ft) \n shows global position of robot path, along with leftFront, leftRear, rightFront, and rightRear wheel trajectories");

			//force graph to show 1/2 field dimensions of 24ft x 27 feet
			fig5.setXTic(0, 30, 1);
			fig5.setYTic(0, 30, 1);
			fig5.addData(path.smoothPath, Color.red, Color.blue);


			fig5.addData(path.leftFrontPath, Color.cyan);
			fig5.addData(path.leftRearPath, Color.green);
			fig5.addData(path.rightFrontPath, Color.magenta);
			fig5.addData(path.rightRearPath, Color.orange);
			
			FalconLinePlot fig6 = new FalconLinePlot(path.transformHeadingToGyroFrameOfRef(path.heading),null,Color.blue);
			
			fig6.yGridOn();
			fig6.xGridOn();
			fig6.setYLabel("des heading (deg)");
			fig6.setXLabel("time (seconds)");
		}
	}
}