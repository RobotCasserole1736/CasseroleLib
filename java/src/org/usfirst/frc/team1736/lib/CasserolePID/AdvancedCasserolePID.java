package org.usfirst.frc.team1736.lib.CasserolePID;
import org.usfirst.frc.team1736.lib.Util.MapLookup2D;

/**
 * DESCRIPTION: <br>
 * Fancy PID Controller algorithm designed by FRC1736 Robot Casserole. The WPIlib PID library is pretty
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
 * runtime based on some variable. It's useful for when you have different operating conditions
 * under which you want your system to behave differently. If you notice that one set of gains
 * works well in some conditions, but a different set of gains works well in different conditions,
 * Gain Scheduling will allow you to change which set you use depending on your operating condition.
 * This class allows "operating condition" to be defined by this PID's setpoint, error, or some
 * external value that the user passes in at regular intervals. It used to be common to see this
 * sort of thing in aerospace, but it's pretty advanced for FRC. Then again, you might find it
 * useful... anyway.<br>
 *<br>
 * The other feature provided is a way to limit the max value of the integrators (pre-gain). This can 
 * force windup to not occur, but could cause some instability if not used carefully.
 * 
 * 
 */
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
     * is set to the "External" type. This is useful when the gains to be used should depending
	 * on something completely isolated from the PID-controlled system. For example, imagine
	 * a system where a PID controlled gripper is at the end of an arm. If the arm position
	 * is extended, gravity helps out so you need less gains. If the arm position is retracted,
	 * you now have to work against gravity, so you want more agressive gains. The arm position
	 * could be passed in here as the external scheduler variable, and two sets of gains for the
	 * gripper could be set into the gain scheduler.
     * 
     * @param val_in present value of the external scheduler variable
     */
    public void setGainSchedulerVal(double val_in) {
        if (GainSchedulerType == GainScheduleTypes.EXTERNAL) {
            schedulerVal = val_in;
        }
    }


    @Override
    // The big kahuna. This is where the magic happens. This is based on
    // CassersolePID's update, but adds more.
	/** Periodic update for this PID. Automatically called, user shouldn't ever have to call it. */
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
