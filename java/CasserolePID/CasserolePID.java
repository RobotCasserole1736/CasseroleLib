package org.usfirst.frc.team1736.robot;

import java.util.Timer;
import java.util.TimerTask;


public abstract class CasserolePID {

	// PID Gain constants
	public double Kp;  //Proportional
	public double Ki;  //Integral
	public double Kd;  //Derivative
	public double Kf;  //Setpoint Feed-Forward
	public double Kdf; //Setpoint Derivative Feed-Forward
	public double Kp2; //Proportional Squared
	
	public boolean useErrForDerivTerm; //If true, derivative term is calculated using the error signal. Otherwise, use the "actual" value from the PID system.
	
	//Things for doing math
	DerivativeCalculator dTermDeriv;
	DerivativeCalculator setpointDeriv;
	IntegralCalculator   iTermIntegral;

	public volatile double setpoint;
	
	//Value limiters
	public double outputMin; //output limit
	public double outputMax;
	
	public double integratorDisableThresh; // If the abs val of the error goes above this, disable and reset the integrator to prevent windup
	
	//PID Thread
	private Timer timerThread;
	
	//Thread frequency
	private final long pidSamplePeriod_ms = 10;
	
	//Watchdog Counter - counts up every time we run a periodic loop.
	//An external obesrver can check this for positive verification the
	//PID loop is still alive.
	public volatile long watchdogCounter;
	
	
	//Simple constructor. Makes nice PID easy
	CasserolePID(double Kp_in, double Ki_in, double Kd_in){
		Kp  = Kp_in;
		Ki  = Ki_in;
		Kd  = Kd_in;
		Kf  = 0.0;
		Kdf = 0.0;
		Kp2 = 0.0;
		commonConstructor();
	}
	
	//More-things-exposed constructor
	CasserolePID(double Kp_in, double Ki_in, double Kd_in, double Kf_in, double Kdf_in, double Kp2_in){
		Kp  = Kp_in;
		Ki  = Ki_in;
		Kd  = Kd_in;
		Kf  = Kf_in;
		Kdf = Kdf_in;
		Kp2 = Kp2_in;
		commonConstructor();
	}
	
	//Do the rest of the construction things, like setting defaults
	private void commonConstructor(){
		
		dTermDeriv    = new DerivativeCalculator();
		setpointDeriv = new DerivativeCalculator();
		iTermIntegral = new IntegralCalculator();
		
		useErrForDerivTerm = true;
		
		outputMin   = Double.NEGATIVE_INFINITY;
		outputMax   = Double.POSITIVE_INFINITY;
		
		integratorDisableThresh = Double.POSITIVE_INFINITY;
		
		setpoint = 0;
		
        timerThread = new java.util.Timer();
		
	}
	
	//Start the PID thread
	public void start(){
		resetIntegrators();
		watchdogCounter = 0;
		//Kick off the multi-threaded stuff.
		//Will start calling the periodic update function at an interval of pidSamplePeriod_ms,
		//asynchronously from any other code.
		//Java magic here, don't touch!
        timerThread.scheduleAtFixedRate(new PIDTask(this), 0L, (long) (pidSamplePeriod_ms));
	}
	
	public void stop(){
		//Stop whatever thread may or may not be running
        timerThread.cancel();	
	}
	
	//Reset all internal integrators
	public void resetIntegrators(){
		iTermIntegral.resetIntegral();
	}
	
	//Assign a setpoint. 
	public void setSetpoint(double setpoint_in){
			setpoint = setpoint_in;	
	}
	
	
	/**
	 * Override this method!
	 * This function must be implemented to return the present "actual" value of the system under control.
	 * For example, when controlling a motor to turn a certain number of rotations, this should return the encoder count
	 * or number of degrees or something like that. You must implement this! Expect it to be called frequently and
	 * asynchronously by the underlying PID algorithm. Make sure it runs fast!
	 * @return
	 */
	protected abstract double returnPIDInput();
	
	/**
	 * Override this method!
	 * This function will return the value calculated from the PID. Expect it to be called frequently and asynchronously
	 * by the underlying PID algorithm. When it is called, you should utilize the value given to do something useful.
	 * For example, when controlling a motor to turn a certain number of rotations, your implementation of this function
	 * should send the output of the PID to one of the motor controllers. Make sure it runs fast!
	 * @return
	 */
	protected abstract void usePIDOutput(double pidOutput);

	//The big kahuna. This is where the magic happens.
	private void periodicUpdate(){
		double curInput = returnPIDInput();
		double curOutput = 0.0;
		double curSetpoint = setpoint; //latch the setpoint at start of loop
		double curError = curSetpoint - curInput;
		
		
		//Calculate P term
		if(Kp != 0.0){ //speed optimization when terms are turned off
			curOutput = curOutput + curError*Kp;
		}
		//Calculate I term
		if(Ki != 0.0){
			if(Math.abs(curError) > integratorDisableThresh){
				iTermIntegral.resetIntegral();
			} 
			else {
				curOutput = curOutput + iTermIntegral.calcIntegral(curError)*Ki;
			}
		}
		//Calculate D term
		if(Kd != 0.0){
			if(useErrForDerivTerm){
				curOutput = curOutput + dTermDeriv.calcDeriv(curError)*Kd;
			}
			else {
				curOutput = curOutput + dTermDeriv.calcDeriv(curInput)*Kd;	
			}
		}
		//Calculate FF term
		if(Kf != 0.0){
			curOutput = curOutput + curSetpoint*Kf;
		}
		//Calculate derivative FF term
		if(Kdf != 0.0){
			curOutput = curOutput + setpointDeriv.calcDeriv(curSetpoint)*Kdf;
		}
		//Calculate P^2 term
		if(Kp2 != 0.0){
			if(curError >= 0){
				curOutput = curOutput + curError*curError*Kp;
			}
			else{
				curOutput = curOutput - curError*curError*Kp;
			}		
		}
		
		
		//Assign output
		if(curOutput > outputMax){
			usePIDOutput(outputMax);
		}
		else if (curOutput < outputMin){
			usePIDOutput(outputMin);
		}
		else{
			usePIDOutput(curOutput);
		}	
		
		//Indicate we are still doing stuff with the watchdog
		watchdogCounter = watchdogCounter + 1;
	}
	
	
	
	//Java multithreading magic. Do not touch.
	//Touching will incour the wrath of Cthulhu, god of java and PID.
	//May the oceans of 1's and 0's rise to praise him.
    private class PIDTask extends TimerTask 
    {
        private CasserolePID m_pid;

        public PIDTask(CasserolePID pid) 
        {
            if (pid == null) 
            {
                throw new NullPointerException("Given PIDController was null");
            }
            m_pid = pid;
        }

        @Override
        public void run() 
        {
        	m_pid.periodicUpdate();
        }
    }
	
	//Nice getters and setters follow
	public void setErrorAsDerivTermSrc(){
		useErrForDerivTerm = true;
	}

	public void setActualAsDerivTermSrc(){
		useErrForDerivTerm = false;
	}
	
	/**
	 * @return the kp
	 */
	public double getKp() {
		return Kp;
	}

	/**
	 * @param kp the kp to set
	 */
	public void setKp(double kp) {
		Kp = kp;
	}

	/**
	 * @return the ki
	 */
	public double getKi() {
		return Ki;
	}

	/**
	 * @param ki the ki to set
	 */
	public void setKi(double ki) {
		Ki = ki;
	}

	/**
	 * @return the kd
	 */
	public double getKd() {
		return Kd;
	}

	/**
	 * @param kd the kd to set
	 */
	public void setKd(double kd) {
		Kd = kd;
	}

	/**
	 * @return the kf
	 */
	public double getKf() {
		return Kf;
	}

	/**
	 * @param kf the kf to set
	 */
	public void setKf(double kf) {
		Kf = kf;
	}

	/**
	 * @return the kdf
	 */
	public double getKdf() {
		return Kdf;
	}

	/**
	 * @param kdf the kdf to set
	 */
	public void setKdf(double kdf) {
		Kdf = kdf;
	}

	/**
	 * @return the kp2
	 */
	public double getKp2() {
		return Kp2;
	}

	/**
	 * @param kp2 the kp2 to set
	 */
	public void setKp2(double kp2) {
		Kp2 = kp2;
	}
	
	public void setOutputRange(double min, double max){
		outputMin = min;
		outputMax = max;
	}
	
	public double getSetpoint(){
		return setpoint;
	}
	
	
}
