package org.usfirst.frc.team1736.robot;

import java.util.Arrays;
/**
 * Battery parameter estimator - calculates an estimate for a battery's open circuit voltage (Voc) and
 * equivilant series resistance (ESR) based on a windows of system current/voltage measurements.
 * Ensures enough spread in the measurement window to ensure confidence in the estimate. Based on a 
 * whitepaper detailing an algorithm developed for the 2016 FRC season by FRC1736 RobotCasserole.
 * @author Chris Gerth
 *
 */
public class BatteryParamEstimator {

	static double VocEstInit = 13;
	static double EsrEstInit = 0.025;
	static double IdrawInit = 0.025;
	
	double VocEst = VocEstInit;
	double ESREst = EsrEstInit;
	boolean confident = false;
	double min_spread_thresh = 7.0;
	double prev_best_spread = 0;
	double prev_best_esr = EsrEstInit;
	
	int lms_window_length;
	double[] circ_buf_SysCurDraw_A;
	double[] circ_buf_SysVoltage_V;
	int index;
	
	AveragingFilter input_V_filt;
	AveragingFilter input_I_filt;
	AveragingFilter esr_output_filt;
	
	public BatteryParamEstimator(int length){
		lms_window_length = length;
		index = 0;
		circ_buf_SysCurDraw_A = new double[lms_window_length];
		circ_buf_SysVoltage_V = new double[lms_window_length];
		Arrays.fill(circ_buf_SysCurDraw_A, IdrawInit);
		Arrays.fill(circ_buf_SysVoltage_V, VocEstInit);
		input_V_filt = new AveragingFilter(5, VocEstInit);
		input_I_filt = new AveragingFilter(5, 0.0);
		esr_output_filt = new AveragingFilter(20,EsrEstInit);
		
	}
	
	public void setConfidenceThresh(double Thresh_A){
		min_spread_thresh = Thresh_A;
	}

	/**
	 * updateEstimate - Update the internal estimates with a new measured system voltage and current. Should be called
	 * once per control loop.
	 * @param measSysVoltage_V - Battery voltage as measured by PDB
	 * @param measSysCurrent_A - Current draw as measured by PDB
	 */
	public void updateEstimate(double measSysVoltage_V, double measSysCurrent_A){
		
		//Update buffers with new inputs (filtered)
		circ_buf_SysCurDraw_A[index] = input_I_filt.filter(measSysCurrent_A);
		circ_buf_SysVoltage_V[index] = input_V_filt.filter(measSysVoltage_V);
		index = (index + 1)%lms_window_length;
		
		//Perform Least Mean Squares estimation utilizing algorithm
		// outlined at http://faculty.cs.niu.edu/~hutchins/csci230/best-fit.htm
		double sumV = findSum(circ_buf_SysVoltage_V);
		double sumI = findSum(circ_buf_SysCurDraw_A);
		double sumIV = findDotProd(circ_buf_SysCurDraw_A,circ_buf_SysVoltage_V);
		double sumI2 = findDotProd(circ_buf_SysCurDraw_A,circ_buf_SysCurDraw_A);
		double meanV = sumV/lms_window_length;
		double meanI = sumI/lms_window_length;
		
		ESREst = - (sumIV - sumI * meanV)/(sumI2 - sumI * meanI);
		
		//Calculate the spread of the system current drawn
		//The Standard Deviation of the input windodw of points is used.
		double spread_I = findStdDev(circ_buf_SysCurDraw_A);
		
		//If the spread is above the preset threshold, we will be confident for this window
		if(spread_I > min_spread_thresh){
			confident = true;
			//Additionally, if this is the best spread we've seen so far, 
			//record the spread and ESR values for future use
			if(spread_I > prev_best_spread){
				prev_best_spread = spread_I;
				prev_best_esr = ESREst;
			}
		} else { //If the spread is too small, we're not confident, and reset previous best spread.
			confident = false;
			prev_best_spread = 0;
		}
		
		//If we weren't confident in that ESR we just calculated, pick the
		//last known best value instead.
		if(!confident){
			ESREst = prev_best_esr;
		}
		
		//Filter the output ESR to prevent drastic spikes
		ESREst = esr_output_filt.filter(ESREst);
		
		//From the ESR, calculate the open-circuit voltage
		VocEst = meanV + ESREst * meanI;
		
		return; //nothing to return, params are gotten with other "getter" functions
		
	}
	
	/**
	 * getEstESR - returns the most recent calculated equivilant series resistance
	 * @return the most recent calculated equivilant series resistance
	 */
	public double getEstESR(){
		return ESREst;
	}

	/**
	 * getEstVoc - returns the most recent calculated open circuit resistance
	 * @return the most recent calculated open circuit resistance
	 */
	public double getEstVoc(){
		return VocEst;
	}
	
	/**
	 * getConfidence - returns the most recent confidenc decision
	 * @return True if the most recent window had enough spread for a reasonable estimate, false if not.
	 */
	public boolean getConfidence(){
		return confident;
	}
	
	/**
	 * getEstVsys - given a system current draw, estimate the resulting system voltage given the battery parameters.
	 * @param Idraw_A - estimated system current draw in Amps
	 * @return Estimated Vsys in Volts
	 */
	public double getEstVsys(double Idraw_A){
		return VocEst - Idraw_A * ESREst;
	}
	
	/**
	 * getMaxIdraw - given a minimum system voltage allowable, estimate the maximum current which may be drawn from the battery at present time..
	 * @param VsysMin_V - minimum desireable system voltage
	 * @return The maximum current which the robot may pull in amps.
	 */
	public double getMaxIdraw(double VsysMin_V){
		return (VocEst - VsysMin_V) / ESREst;
	}
	
	
	/**
	 * findSum - calculates the sum of all elements in an array of doubles
	 * @param input - array to add up.
	 * @return sum of all values in the array.
	 */
	private double findSum(double[] input){
		double sum = 0;
		for(double i : input){
			sum += i;
		}
		return sum;
	}
	
	/**
	 * findDotProd - calculates the dot product of two equally-sized arrays of doubles A and B. Dot product is found by multiplying each
	 * element of A with the corresponding element of B, and adding together the result of each multiplication. 
	 * @param A First Array
	 * @param B Second Array
	 * @return dot product of A and B.
	 */
	private double findDotProd(double[] A, double[] B){
		double sum = 0;
		if(A.length != B.length){
			System.out.println("Error - A and B vectors are not the same length! Cannot compute dot product.");
			return Double.NaN;
		}
		
		for(int i = 0; i < A.length ; i++ ){
			sum += A[i] * B[i];
		}
		
		return sum;
	}
	
	/**
	 * findStdDev - Calculates the standard deviation of a set of doubles. Standard deviation is defined as the square root of the 
	 * sum of the squares of the distances of each element from the average of the dataset.
	 * @param input - doubles to take the Standard Deviation of.
	 * @return standard deviation of the input set.
	 */
	private double findStdDev(double[] input){
		double avg_input = findSum(input)/input.length;
		double sum = 0;
		
		for(int i = 0; i < input.length; i++){
			sum += Math.pow((input[i] - avg_input), 2);
		}
		return Math.sqrt(sum/input.length);
	}
}
