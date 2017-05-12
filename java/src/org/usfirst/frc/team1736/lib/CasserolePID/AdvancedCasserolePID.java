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

/**
 * DESCRIPTION: <br>
 * PID Controller algorithm designed by FRC1736 Robot Casserole. The WPIlib PID library is pretty
 * darn good, but we had some things we wanted done differently. Therefore, we did it ourselves.
 * This is the Advanced flavor of our PID algorithms. We have no idea if it will actually be useful
 * for FRC purposes, but the concepts are not beyond what we believe an advanced FRC student could
 * understand. <br>
 * <br>
 * See the CasserolePID class documentation for more info on how different PID stuff should be
 * utilized. This class wrappers/extends that base PID class with more functionality. However, its
 * fundamental usage should be the same as the main class. <br>
 * <br>
 * There are number of new features added by the Advanced flavor. <br>
 * <br>
 * Gain Scheduling is a controls technique where the gains for a PID controller are modified at
 * runtime based on some variable.
 * 
 * 
 */


import org.usfirst.frc.team1736.lib.Util.MapLookup2D;

public abstract class AdvancedCasserolePID extends CasserolePID {


    /** Supported types of Gain Scheduling */
    public enum GainScheduleTypes {
        /** Do not use gain scheduling. */
        NONE,
        /** Schedule PID gains based on present Setpoint value */
        SETPOINT,
        /** Schedule PID gains based on present Error value */
        ERROR,
        /**
         * Schedule PID gains based on an external value, set by the setGainSchedulerVal() method
         */
        EXTERNAL
    };

    GainScheduleTypes GainSchedulerType = GainScheduleTypes.NONE;
    double schedulerVal = 0;

    double IntegralMaxVal = 0;

    double activeKp;
    double activeKi;
    double activeKd;


    MapLookup2D pScheduledVals = null;
    MapLookup2D iScheduledVals = null;
    MapLookup2D dScheduledVals = null;


    /**
     * More advanced PID algorithm, with all the bells and whistles!
     * 
     * @param Kp_in Proportional gain
     * @param Ki_in Integral gain
     * @param Kd_in Derivative Gain
     * @param Kf_in Feed-Forward Gain
     * @param Kdf_in Deriviative Feed-Forward Gain
     * @param Kp2_in Proportional Squared Gain
     * @param Gst_in Gain Scheduling Type (schedule PID gains on Setpoint, error, or nothing). If
     *        scheduling is requested, the add*GainSchedulePoint() methods must be called to set up
     *        the schedulers.
     * @param IntMagMax_in Maximum allowable magnititude of the integrator (before applying gain).
     *        Will allow windup, but not pass on giant integral terms to the rest of the system.
     */
    AdvancedCasserolePID(double Kp_in, double Ki_in, double Kd_in, double Kf_in, double Kdf_in, double Kp2_in,
            GainScheduleTypes Gst_in, double IntMagMax_in) {
        super(Kp_in, Ki_in, Kd_in, Kf_in, Kdf_in, Kp2_in);
        activeKp = Kp;
        activeKi = Ki;
        activeKd = Kd;
        GainSchedulerType = Gst_in;
        IntegralMaxVal = Math.abs(IntMagMax_in);
    }


    /**
     * Add a new point to the Proportional Gain Scheduler. Will utilize a variable P gain depending
     * on the present value of the scheduler variable. The scheduler variable is defined by the
     * setting given to this class's constructor. Currently, scheduling gains based on Setpoint,
     * Error, or some external value is supported.
     * 
     * @param scheduler_var_value scheduler variable value at which to define a gain
     * @param gain Gain value to use when the scheduler variable.
     */
    public void addPScheduleVal(double scheduler_var_value, double gain) {
        if (pScheduledVals == null) {
            pScheduledVals = new MapLookup2D();
        }
        pScheduledVals.insertNewPoint(scheduler_var_value, gain);
        return;
    }


    /**
     * Add a new point to the Integral Gain Scheduler. Will utilize a variable I gain depending on
     * the present value of the scheduler variable. The scheduler variable is defined by the setting
     * given to this class's constructor. Currently, scheduling gains based on Setpoint, Error, or
     * some external value is supported.
     * 
     * @param scheduler_var_value scheduler variable value at which to define a gain
     * @param gain Gain value to use when the scheduler variable.
     */
    public void addIScheduleVal(double scheduler_var_value, double gain) {
        if (iScheduledVals == null) {
            iScheduledVals = new MapLookup2D();
        }
        iScheduledVals.insertNewPoint(scheduler_var_value, gain);
        return;
    }


    /**
     * Add a new point to the Derivative Gain Scheduler. Will utilize a variable D gain depending on
     * the present value of the scheduler variable. The scheduler variable is defined by the setting
     * given to this class's constructor. Currently, scheduling gains based on Setpoint, Error, or
     * some external value is supported.
     * 
     * @param scheduler_var_value scheduler variable value at which to define a gain
     * @param gain Gain value to use when the scheduler variable.
     */
    public void addDScheduleVal(double scheduler_var_value, double gain) {
        if (dScheduledVals == null) {
            dScheduledVals = new MapLookup2D();
        }
        dScheduledVals.insertNewPoint(scheduler_var_value, gain);
        return;
    }


    /**
     * Set the gain scheduler variable value. Only useful if Gain Scheduling is being used, and it
     * is set to the "External" type.
     * 
     * @param val_in
     */
    public void setGainSchedulerVal(double val_in) {
        if (GainSchedulerType == GainScheduleTypes.EXTERNAL) {
            schedulerVal = val_in;
        }
    }


    @Override
    // The big kahuna. This is where the magic happens. This is based on
    // CassersolePID's update, but adds more.
    protected void periodicUpdate() {
        double curInput = returnPIDInput();
        double curOutput = 0.0;
        double curSetpoint = setpoint; // latch the setpoint at start of loop
        double curError = curSetpoint - curInput;


        // Calculate the PID gains this loop if gain scheduling used
        if (GainSchedulerType != GainScheduleTypes.NONE) {
            if (GainSchedulerType == GainScheduleTypes.SETPOINT) {
                schedulerVal = curSetpoint;
            } else if (GainSchedulerType == GainScheduleTypes.ERROR) {
                schedulerVal = curError;
            } // else, external, so schedulerVal is already updated.

            // Use PID scheduling only if at least one point has been defined
            // into the scheduler table.
            if (pScheduledVals != null) {
                activeKp = pScheduledVals.lookupVal(schedulerVal);
            }
            if (iScheduledVals != null) {
                activeKi = iScheduledVals.lookupVal(schedulerVal);
            }
            if (dScheduledVals != null) {
                activeKd = dScheduledVals.lookupVal(schedulerVal);
            }
        }

        // Calculate P term
        if (activeKp != 0.0) { // speed optimization when terms are turned off
            curOutput = curOutput + curError * activeKp;
        }
        // Calculate I term
        if (activeKi != 0.0) {
            if (Math.abs(curError) > integratorDisableThresh) {
                iTermIntegral.resetIntegral();
            } else {
                double integral_val = iTermIntegral.calcIntegral(curError);
                if (integral_val > IntegralMaxVal) {
                    integral_val = IntegralMaxVal;
                    iTermIntegral.accumulator = integral_val;
                } else if (integral_val < -IntegralMaxVal) {
                    integral_val = -IntegralMaxVal;
                    iTermIntegral.accumulator = integral_val;
                }
                curOutput = curOutput + integral_val * activeKi;
            }
        }
        // Calculate D term
        if (activeKd != 0.0) {
            if (useErrForDerivTerm) {
                curOutput = curOutput + dTermDeriv.calcDeriv(curError) * activeKd;
            } else {
                curOutput = curOutput + dTermDeriv.calcDeriv(curInput) * activeKd;
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
        if (activeKp != 0.0) {
            if (curError >= 0) {
                curOutput = curOutput + curError * curError * activeKp;
            } else {
                curOutput = curOutput - curError * curError * activeKp;
            }
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

}
