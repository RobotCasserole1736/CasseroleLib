package org.usfirst.frc.team1736.lib.Logging;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import edu.wpi.first.wpilibj.RobotState;

import java.io.BufferedWriter;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodType.*;

///////////////////////////////////////////////////////////////////////////////
// Copyright (c) FRC Team 1736 2016. See the License file.
//
// Can you use this code? Sure! We're releasing this under GNUV3, which
// basically says you can take, modify, share, publish this as much as you
// want, as long as you don't make it closed source.
//
// If you do find it useful, we'd love to hear about it! Check us out at
// http://robotcasserole.org/ and leave us a message!
///////////////////////////////////////////////////////////////////////////////

/**
 * DESCRIPTION: <br>
 * Provides an API for FRC 1736 Robot Casserole datalogging on the robot during testing or matches.
 * Will write lines into a CSV file with a unique name between calls to init() and close().
 * output_dir is hardcoded to point to a specific 2016 folder on a flash drive connected to the
 * roboRIO. This implementation utilizes java's "methodHandles" libraries to refer to the data which
 * is to be put into the log. It is also a static class, which needs no instantiation. <br>
 * <br>
 * USAGE:
 * <ol>
 * <li>During init, call addLoggingFieldDouble method for each field to be logged.</li>
 * <li>During teleop init or autonomous init, call the init() function to start logging data to a
 * new file.</li>
 * <li>Each loop, call the logData</li>
 * <li>During DisabledInit, call the close() method to close out any file which was being written to
 * while the robot was doing something.</li>
 * <li>Post-match or -practice, extract the data logs from the USB drive(maybe using FTP?) and view
 * with excel or your favourite software.</li>
 * </ol>
 * 
 * 
 */
public class CsvLogger {

    static long log_write_index;
    static String log_name = null;
    static String output_dir = "/U/data_captures/"; // USB drive is mounted to /U on roboRIO
    static BufferedWriter log_file = null;
    static boolean log_open = false;

    static Vector<String> dataFieldNames = new Vector<String>();
    static Vector<String> unitNames = new Vector<String>();
    static Vector<MethodHandle> methodHandles = new Vector<MethodHandle>();
    static Vector<Vector<Object>> mhReferenceObjects = new Vector<Vector<Object>>();
    static double lastLeftMotorCurrent = 0;
    static double lastRightMotorCurrent = 0;


    /**
     * Clears the IO buffer in memory and forces things to file. Generally a good idea to use this
     * as infrequently as possible (because it increases logging overhead), but definitely use it
     * before the roboRIO might crash without a proper call to the close() method (ie, during
     * brownout)
     * 
     * @return 0 on flush success or -1 on failure.
     */
    public static int forceSync() {
        if (log_open == false) {
            System.out.println("Error - Log is not yet opened, cannot sync!");
            return -1;
        }
        try {
            log_file.flush();
        }
        // Catch ALL the errors!!!
        catch (IOException e) {
            System.out.println("Error flushing IO stream file: " + e.getMessage());
            return -1;
        }

        return 0;

    }



    /**
     * Closes the log file and ensures everything is written to disk. init() must be called again in
     * order to write to the file.
     * 
     * @return -1 on failure to close, 0 on success
     */
    public static int close() {

        if (log_open == false) {
            System.out.println("Warning - Log is not yet opened, nothing to close.");
            return 0;
        }

        try {
            log_file.close();
            log_open = false;
        }
        // Catch ALL the errors!!!
        catch (IOException e) {
            System.out.println("Error Closing Log File: " + e.getMessage());
            return -1;
        }
        return 0;

    }
    
    /**
     * Check whether there is presently a log file open and being written to.
     * @return True if there is an open log file, false otherwise.
     */
    public static boolean isLogOpen(){
    	return log_open;
    }

    private static String getOpModeName() {
    	if(RobotState.isAutonomous()){
    		return "Auto";
    	} else {
    		return "Teleop";
    	}
    }

    private static String getDateTimeString() {
    	//Yes, I could have made this ISO, but I'm american and this format looks nicer to me.
        DateFormat df = new SimpleDateFormat("dd-MMM-yyyy_hh.mm.ssa");
        df.setTimeZone(TimeZone.getTimeZone("US/Central"));
        return df.format(new Date());
    }


    /**
     * Determines a unique file name based on current timestamp, and opens a file in the data
     * captures directory and writes the initial lines to it. Timestamp determination is done based
     * on info received from the Driver station or FMS, so improper time there will cause
     * oddly-named log files. Takes no action if log is already open.
     * 
     * @return 0 on successful log open, -1 on failure
     */
    public static int init() {

        if (log_open) {
            System.out.println("Warning - log is already open!");
            return 0;
        }

        log_open = false;

        // Determine a unique file name
        log_name = output_dir + "log_" + getDateTimeString() + "_" + getOpModeName() + ".csv";
        System.out.println("Initalizing Log file  " + log_name);
        
        
        try {
            // Reset state variables
            log_write_index = 0;


            
            // create directories, if they don't exist
            File tempPathObj = new File(output_dir);
            tempPathObj.mkdirs();

            // Open File
            FileWriter fstream = new FileWriter(log_name, true);
            log_file = new BufferedWriter(fstream);

            // Write user-defined header line
            for (String header_txt : dataFieldNames) {
                log_file.write(header_txt + ", ");
            }
            // End of line
            log_file.write("\n");


            // Write user-defined units line
            for (String header_txt : unitNames) {
                log_file.write(header_txt + ", ");
            }
            // End of line
            log_file.write("\n");

        }
        // Catch ALL the errors!!!
        catch (IOException e) {
            System.out.println("ERROR - cannot initalize log file: " + e.getMessage());
            return -1;
        }

        log_open = true;
        return 0;

    }


    /**
     * Logs data for all stored method handles. Methods that are not considered "simple" should be
     * handled accordingly within this method. This method should be called once per loop.
     * 
     * @param forceSync set true if a forced write is desired (i.e. brownout conditions)
     * @return 0 if log successful, -1 if log is not open, and -2 on other errors
     */
    public static int logData(boolean forceSync) {
        if (!log_open) {
            //System.out.println("ERROR - Log is not yet opened, cannot write!");
            return -1;
        }

        if (forceSync)
            forceSync();

        try {
            for (int i = 0; i < methodHandles.size(); i++) {
                MethodHandle mh = methodHandles.get(i);
                String fieldName = dataFieldNames.get(i);
                Vector<Object> mhArgs = mhReferenceObjects.get(i);
                log_file.write(getStandardLogData(mh, mhArgs) + ", ");
            }
            log_file.write("\n");
        } catch (Exception ex) {
            System.out.println("Error writing to log file: " + ex.getMessage());
            return -2;
        }

        return 0;
    }


    /**
     * Add a field to be logged at each loop of the robot code. A method handle will be created and
     * stored. This method is for non-static methods which return a data type of double.
     * 
     * @param dataFieldName Name of the field/column in the output data. Also used for determining
     *        what to do for "complex" methods
     * @param unitName Name of the units for field/column in the output data.
     * @param classRef Class where the method is held, such as Joystick.class for getRawButton()
     * @param methodName Actual method name to be called for this field, such as "getRawButton"
     * @param reference A reference to the object whose method will be called. If static method,
     *        this should be null.
     * @param args Optional list of arguments that are passed to the method, such as 0 in
     *        getRawButton(0)
     */
    public static void addLoggingFieldDouble(String dataFieldName, String unitName,
            String methodName, Object reference, Object... args) {
        MethodType methodType = methodType(double.class);
        for (Object arg : args)
            methodType = methodType.appendParameterTypes(arg.getClass());
        methodType = methodType.unwrap(); // assumes primitive wrappers should be primitives
        addLoggingField(methodType, dataFieldName, unitName, reference.getClass(), methodName, reference, args);
    }
    
    /**
     * Add a field to be logged at each loop of the robot code. A method handle will be created and
     * stored. This method is for STATIC methods which return a data type of double.
     * 
     * @param dataFieldName Name of the field/column in the output data. Also used for determining
     *        what to do for "complex" methods
     * @param unitName Name of the units for field/column in the output data.
     * @param methodName Actual method name to be called for this field, such as "getRawButton"
     * @param classRef Static Class where the method is held, such as Joystick.class for getRawButton()
     * @param args Optional list of arguments that are passed to the method, such as 0 in
     *        getRawButton(0)
     */
    public static void addLoggingFieldDouble(String dataFieldName, String unitName,
            String methodName, Class<?> classRef, Object... args) {
        MethodType methodType = methodType(double.class);
        for (Object arg : args)
            methodType = methodType.appendParameterTypes(arg.getClass());
        methodType = methodType.unwrap(); // assumes primitive wrappers should be primitives
        addLoggingField(methodType, dataFieldName, unitName, classRef, methodName, null, args);
    }


    /**
     * Add a field to be logged at each loop of the robot code. A method handle will be created and
     * stored. This method is for non-static methods which return a data type of boolean.
     * 
     * @param dataFieldName Name of the field/column in the output data. Also used for determining
     *        what to do for "complex" methods
     * @param unitName Name of the units for field/column in the output data.
     * @param methodName Actual method name to be called for this field, such as "getRawButton"
     * @param reference A reference to the object whose method will be called. If static method,
     *        this should be null.
     * @param args Optional list of arguments that are passed to the method, such as 0 in
     *        getRawButton(0)
     */
    public static void addLoggingFieldBoolean(String dataFieldName, String unitName,
            String methodName, Object reference, Object... args) {
        MethodType methodType = methodType(boolean.class);
        for (Object arg : args)
            methodType = methodType.appendParameterTypes(arg.getClass());
        methodType = methodType.unwrap(); // assumes primitive wrappers should be primitives
        addLoggingField(methodType, dataFieldName, unitName, reference.getClass(), methodName, reference, args);
    }
    
    /**
     * Add a field to be logged at each loop of the robot code. A method handle will be created and
     * stored. This method is for STATIC methods which return a data type of boolean.
     * 
     * @param dataFieldName Name of the field/column in the output data. Also used for determining
     *        what to do for "complex" methods
     * @param unitName Name of the units for field/column in the output data.
     * @param methodName Actual method name to be called for this field, such as "getRawButton"
     * @param classRef Class where the method is held, such as Joystick.class for getRawButton()
     * @param args Optional list of arguments that are passed to the method, such as 0 in
     *        getRawButton(0)
     */
    public static void addLoggingFieldBoolean(String dataFieldName, String unitName, 
            String methodName, Class<?> classRef, Object... args) {
        MethodType methodType = methodType(boolean.class);
        for (Object arg : args)
            methodType = methodType.appendParameterTypes(arg.getClass());
        methodType = methodType.unwrap(); // assumes primitive wrappers should be primitives
        addLoggingField(methodType, dataFieldName, unitName, classRef, methodName, null, args);
    }


    /*
     * Implementation of above convenience classes. Creates method handle and stores relevant
     * information in class level Vector objects.
     */
    private static void addLoggingField(MethodType methodType, String dataFieldName, String unitName, Class<?> classRef,
            String methodName, Object reference, Object... args) {
        if (log_open) {
            System.out.println("Error: cannot add logging field while log file is open");
            return;
        }
        for (int i = 0; i < dataFieldNames.size(); i++) {
            String fieldName = dataFieldNames.get(i);
            if (dataFieldName.equals(fieldName)) {
                Vector<Object> mhArgs = new Vector<Object>();
                mhArgs.add(reference);
                for (Object arg : args)
                    mhArgs.add(arg);
                mhReferenceObjects.set(i, mhArgs);
                System.out.println("Warning: log field already present. Reference updated");
                return;
            }
        }
        MethodHandle methodHandle = null;
        try {
            methodHandle = MethodHandles.lookup().findVirtual(classRef, methodName, methodType);
        } catch (NoSuchMethodException e) {
            System.out.println("Error: Could not add logging field " + dataFieldName + " (no such method)");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            try {
                methodHandle = MethodHandles.lookup().findStatic(classRef, methodName, methodType);
            } catch (Exception ex) {
                System.out.println("Error: Could not add logging field " + dataFieldName);
                ex.printStackTrace();
            }
        }
        dataFieldNames.add(dataFieldName);
        unitNames.add(unitName);
        methodHandles.add(methodHandle);
        Vector<Object> mhArgs = new Vector<Object>();
        if (reference != null) // will be null for static methods
            mhArgs.add(reference);
        for (Object arg : args)
            mhArgs.add(arg);
        mhReferenceObjects.add(mhArgs);
    }


    /***
     * This is used for any non-special logging parameters (those that don't require extra math on
     * the output)
     * 
     * @param methodHandle A MethodHandle stored in the class level Vector
     * @param args Arguments for the given Method Handle
     * @return double value for double return types, 1 or 0 for boolean return types
     */
    private static double getStandardLogData(MethodHandle methodHandle, Vector<Object> args) {
        try {
            if (methodHandle.type().returnType() == double.class)
                return (double) methodHandle.invokeWithArguments(args);
            else if (methodHandle.type().returnType() == boolean.class)
                return ((boolean) methodHandle.invokeWithArguments(args)) ? 1.0 : 0.0;
        } catch (Throwable e) {
            System.out.println("Error running method for data logging");
            e.printStackTrace();
        }
        return 0;
    }

}
