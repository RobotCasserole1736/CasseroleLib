package org.usfirst.frc.team1736.lib.CasserolePID;

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


import java.util.Timer;
import java.util.TimerTask;
import org.usfirst.frc.team1736.lib.SignalMath.DerivativeCalculator;
import org.usfirst.frc.team1736.lib.SignalMath.IntegralCalculator;


/**
 * DESCRIPTION: <br>
 * PID Controller algorithm designed by FRC1736 Robot Casserole. The WPIlib PID library is pretty
 * darn good, but we had some things we wanted done differently. Therefore, we did it ourselves.
 * <br>
 * This controller implements a "PIDFdFP2" controller, with a few selectable options. Execution runs
 * at 10ms (2x speed of *periodic() loops from FRC), this will be made adjustable in the future.
 * Output each loop is simply the sum of each term, with not memory of previous outputs (except in
 * the integral term). Output can be capped to a specific range, and the integral term can turn
 * itself off when the error is too big (prevents windup). More features to be added in the future!
 * <br>
 * TERMS IN CALCULATION:
 * <ul>
 * <li><b>Proportional</b> - The error, equal to "setpoint - actual", multiplied by a gain. The
 * simplest form of feedback, should push a system toward the setpoint.</li>
 * <li><b>Integral</b> - The integral of the error over time, multiplied by a gain. Helps with
 * correcting for small errors that exist over longer periods of time, when the P term alone is not
 * sufficient.</li>
 * <li><b>Derivative</b> - The derivative of the error, or of the actual value(user selects),
 * multiplied by a gain. Helps to rate-limit the P term's action to reduce overshoot and increase
 * stability (to a point).</li>
 * <li><b>Feed-Forward</b> - The setpoint, multiplied by a gain. Helps get the system to a zero
 * error state for systems where there is a linear relationship between setpoint and "ideal system"
 * control value (Think shooter wheel - more motor command means more speed. For a speed-based
 * setpoint, there is a linear relationship between control effort (motor command) and setpoint)
 * </li>
 * <li><b>Feed-Forward</b> - The setpoint's derivative, multiplied by a gain. Helps in scenarios
 * where when the setpoint changes, it's useful to give the system a single "knock". Think using a
 * motor to control to a certain angle - you'll need lots of effort when the setpoint first changes,
 * but ideally no effort once you've gotten to the right place.</li>
 * <li><b>Proportional Squared</b> - The error squared but with sign preserved, multiplied by a
 * gain. Helpful in non-linear systems, and when you can afford to be very aggressive when error is
 * large.</li>
 * </ul>
 * USAGE:
 * <ol>
 * <li>Create new class as a super of this one</li>
 * <li>Override methods to set PID output and return feedback to the algorithm.</li>
 * <li>Call start() method to begin background execution of algorithm.</li>
 * </ol>
 * 
 * 
 */


public abstract class CasserolePID {

    // PID Gain constants
    protected double Kp; // Proportional
    protected double Ki; // Integral
    protected double Kd; // Derivative
    protected double Kf; // Setpoint Feed-Forward
    protected double Kdf; // Setpoint Derivative Feed-Forward
    protected double Kp2; // Proportional Squared
    
    protected double curError; 

    protected boolean useErrForDerivTerm; // If true, derivative term is calculated using the error
                                          // signal. Otherwise, use the "actual" value from the PID
                                          // system.
    protected boolean invertOutput = false; // If true, we will use the opposite sign at the output.
    protected boolean invertActual = false; // If true, we will use the opposite sign when reading the input.

    // Things for doing math
    DerivativeCalculator dTermDeriv;
    DerivativeCalculator setpointDeriv;
    IntegralCalculator iTermIntegral;

    public volatile double setpoint;

    // Value limiters
    protected double outputMin; // output limit
    protected double outputMax;

    protected double integratorDisableThresh; // If the abs val of the error goes above this,
                                              // disable and reset the integrator to prevent windup

    // PID Thread
    private Timer timerThread;

    // Thread frequency
    private final long pidSamplePeriod_ms = 10;

    // Watchdog Counter - counts up every time we run a periodic loop.
    // An external obesrver can check this for positive verification the
    // PID loop is still alive.
    protected volatile long watchdogCounter;
    
    //For debugging, expose the ability to name the thread this runs in
    public String threadName = "Casserole PID Update";

    /**
     * Simple Constructor
     * 
     * @param Kp_in Proportional Term Gain
     * @param Ki_in Integral Term Gain
     * @param Kd_in Derivative Term Gain
     */
    protected CasserolePID(double Kp_in, double Ki_in, double Kd_in) {
        Kp = Kp_in;
        Ki = Ki_in;
        Kd = Kd_in;
        Kf = 0.0;
        Kdf = 0.0;
        Kp2 = 0.0;
        commonConstructor();
    }


    /**
     * More-Complex Constructor
     * 
     * @param Kp_in Proportional Term Gain
     * @param Ki_in Integral Term Gain
     * @param Kd_in Derivative Term Gain
     * @param Kf_in Setpoint Feed-Forward Term Gain
     * @param Kdf_in Setpoint Derivative Feed-Forward Term Gain
     * @param Kp2_in Proportional Squared Term Gain
     */
    protected CasserolePID(double Kp_in, double Ki_in, double Kd_in, double Kf_in, double Kdf_in, double Kp2_in) {
        Kp = Kp_in;
        Ki = Ki_in;
        Kd = Kd_in;
        Kf = Kf_in;
        Kdf = Kdf_in;
        Kp2 = Kp2_in;
        commonConstructor();
    }


    // Do the rest of the construction things, like setting defaults
    private void commonConstructor() {

        dTermDeriv = new DerivativeCalculator();
        setpointDeriv = new DerivativeCalculator();
        iTermIntegral = new IntegralCalculator(1);

        useErrForDerivTerm = true;

        outputMin = Double.NEGATIVE_INFINITY;
        outputMax = Double.POSITIVE_INFINITY;

        integratorDisableThresh = Double.POSITIVE_INFINITY;

        setpoint = 0;

    }


    /**
     * Start the PID thread running. Will begin to call the returnPIDInput and usePIDOutput methods
     * asynchronously.
     */
    public void start() {
        resetIntegrators();
        watchdogCounter = 0;
        // Kick off the multi-threaded stuff.
        // Will start calling the periodic update function at an interval of pidSamplePeriod_ms,
        // asynchronously from any other code.
        // Java magic here, don't touch!
        timerThread = new java.util.Timer(threadName);
        timerThread.scheduleAtFixedRate(new PIDTask(this), 0L, (long) (pidSamplePeriod_ms));
    }


    /**
     * Stop whatever thread may or may not be running. Will finish the current calculation loop, so
     * returnPIDInput and usePIDOutput might get called one more time after this function gets
     * called.
     */
    public void stop() {
        timerThread.cancel();
    }


    /**
     * Reset all internal integrators back to zero. Useful for returning to init-state conditions.
     */
    public void resetIntegrators() {
        iTermIntegral.resetIntegral();
    }


    /**
     * Assign a new setpoint for the algorithm to control to.
     */
    public void setSetpoint(double setpoint_in) {
        setpoint = setpoint_in;
    }
    
    
    /**
     * Sets the control effort to be inverted from the normal calculation
     * @param inv
     */
    public void setOutputInverted(boolean inv){
    	invertOutput = inv;
    }
    
    /**
     * Sets the sensor input (actual) to be inverted in the normal calculation
     * @param inv
     */
    public void setSensorInverted(boolean inv){
    	invertActual = inv;
    }
    
    public double getCurError(){
    	return curError;
    }


    /**
     * Override this method! This function must be implemented to return the present "actual" value
     * of the system under control. For example, when controlling a motor to turn a certain number
     * of rotations, this should return the encoder count or number of degrees or something like
     * that. You must implement this! Expect it to be called frequently and asynchronously by the
     * underlying PID algorithm. Make sure it runs fast!
     * 
     * @return The sensor feedback value for the PID algorithm to use.
     */
    protected abstract double returnPIDInput();


    /**
     * Override this method! This function will return the value calculated from the PID. Expect it
     * to be called frequently and asynchronously by the underlying PID algorithm. When it is
     * called, you should utilize the value given to do something useful. For example, when
     * controlling a motor to turn a certain number of rotations, your implementation of this
     * function should send the output of the PID to one of the motor controllers. Make sure it runs
     * fast!
     * 
     * @param pidOutput The control effort output calculated by the PID algorithm
     */
    protected abstract void usePIDOutput(double pidOutput);


    // The big kahuna. This is where the magic happens.
    protected void periodicUpdate() {
        double curInput = returnPIDInput();
        
        if(invertActual){
        	curInput = -1.0 * curInput;
        }
        
        double curOutput = 0.0;
        double curSetpoint = setpoint; // latch the setpoint at start of loop
        curError = curSetpoint - curInput;


        // Calculate P term
        if (Kp != 0.0) { // speed optimization when terms are turned off
            curOutput = curOutput + curError * Kp;
        }
        // Calculate I term
        if (Ki != 0.0) {
            if (Math.abs(curError) > integratorDisableThresh) {
                iTermIntegral.resetIntegral();
            } else {
                curOutput = curOutput + iTermIntegral.calcIntegral(curError) * Ki;
            }
        }
        // Calculate D term
        if (Kd != 0.0) {
            if (useErrForDerivTerm) {
                curOutput = curOutput + dTermDeriv.calcDeriv(curError) * Kd;
            } else {
                curOutput = curOutput + dTermDeriv.calcDeriv(curInput) * Kd;
            }
        }
        // Calculate FF term
        if (Kf != 0.0) {
            curOutput = curOutput + curSetpoint * Kf;
        }
        // Calculate derivative FF term
        if (Kdf != 0.0) {
            curOutput = curOutput + setpointDeriv.calcDeriv(curSetpoint) * Kdf;
        }
        // Calculate P^2 term
        if (Kp2 != 0.0) {
            if (curError >= 0) {
                curOutput = curOutput + curError * curError * Kp;
            } else {
                curOutput = curOutput - curError * curError * Kp;
            }
        }


        if(invertOutput){
        	curOutput = curOutput * -1.0;
        }
        
        // Assign output
        if (curOutput > outputMax) {
            usePIDOutput(outputMax);
        } else if (curOutput < outputMin) {
            usePIDOutput(outputMin);
        } else {
            usePIDOutput(curOutput);
        }

        // Indicate we are still doing stuff with the watchdog
        watchdogCounter = watchdogCounter + 1;
    }



    // Java multithreading magic. Do not touch.
    // Touching will incour the wrath of Cthulhu, god of java and PID.
    // May the oceans of 1's and 0's rise to praise him.
    private class PIDTask extends TimerTask {
        private CasserolePID m_pid;


        public PIDTask(CasserolePID pid) {
            if (pid == null) {
                throw new NullPointerException("Given PIDController was null");
            }
            m_pid = pid;
        }


        @Override
        public void run() {
            m_pid.periodicUpdate();
        }
    }


    /**
     * Call this method to set up the algorithm to utilize the error between setpoint and actual for
     * the derivative term calculation.
     */
    public void setErrorAsDerivTermSrc() {
        useErrForDerivTerm = true;
    }


    /**
     * Call this method to set up the algorithm to utilize only the actual (sensor feedback) value
     * for the derivative term calculation.
     */
    public void setActualAsDerivTermSrc() {
        useErrForDerivTerm = false;
    }


    /**
     * @return The present Proportional term gain
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
     * @return The present Integral term gain
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
     * @return The present Derivative term gain
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
     * @return The present feed-forward term gain
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
     * @return The present derivative feed-forward term gain
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
     * @return The present Proportional-squared term gain
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


    /**
     * Set limits on what the control effort (output) can be commanded to.
     * 
     * @param min Smallest allowed control effort
     * @param max Largest allowed control effort
     */
    public void setOutputRange(double min, double max) {
        outputMin = min;
        outputMax = max;
    }


    /**
     * @return The present setpoint
     */
    public double getSetpoint() {
        return setpoint;
    }


    /**
     * Set the Integral term disable threshold. If the absolute value of the error goes above this
     * threshold, the integral term will be set to zero AND the integrators will internally reset
     * their accumulators to zero. Once the error gets below this threshold, the integrators will
     * resume integrating and contributing to the control effort. This is a mechanism to help
     * prevent integrator windup in high-error conditions. By default it is disabled (threshold =
     * positive infinity).
     * 
     * @param integratorDisableThresh_in The new threshold to use.
     */
    public void setintegratorDisableThresh(double integratorDisableThresh_in) {
        integratorDisableThresh = integratorDisableThresh_in;
    }

}
