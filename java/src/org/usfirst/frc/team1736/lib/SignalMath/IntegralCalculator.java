package org.usfirst.frc.team1736.robot;

import edu.wpi.first.wpilibj.Timer;

public class IntegralCalculator {
	private double prev_time;
	private double accumulator;
	private double[] point_num;
	private int choice;
	private double value;
	
	IntegralCalculator(int choice_in){
		prev_time = Timer.getFPGATimestamp();
		accumulator = 0;
		point_num = new double [5];
		choice = choice_in;
	}
	
	
	public double calcIntegral(double in){
		double cur_time = Timer.getFPGATimestamp();
		/*Integration method - "Sample and hold"*/
		point_num[4] = point_num[3];
		point_num[3] = point_num[2];
		point_num[2] = point_num[1];
		point_num[1] = point_num[0];
		point_num[0] = in;
		
		if (choice == 0) /*trapezoid rule*/{
			value = ((cur_time-prev_time)/2)*(point_num[0] + point_num[1]);
		}
		if (choice == 1) /*simpson's rule*/{
			value = (1/2)*((cur_time-prev_time)/6)*(point_num[0] + (4*point_num[1]) + point_num[2]);
		}
		if (choice == 2) /*simpson's 3/8 rule*/{
			value = (1/3)*((cur_time-prev_time)/8)*(point_num[0] + (3*point_num[1]) + (3*point_num[2])+ point_num[3]);
		}
		if (choice == 3) /*boole's rule*/{
			value = (1/4)*((cur_time-prev_time)/90)*((7*point_num[0]) + (32*point_num[1]) + (12*point_num[2])+ (32*point_num[3]) + (7 * point_num[4]));
		}
		if (choice == 4) /*rectangular rule*/ {
			value += in * (cur_time - prev_time);
		}
		/*Save values for next loop and return*/
		accumulator = accumulator + value;
		prev_time = cur_time;
		return accumulator;
	}
	
	public void resetIntegral(){
		accumulator = 0;
	}

}
