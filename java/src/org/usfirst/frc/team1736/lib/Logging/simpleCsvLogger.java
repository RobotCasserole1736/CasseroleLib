package org.usfirst.frc.team1736.lib.Logging;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.io.BufferedWriter;


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
 * Provides an API for FRC 1736 Robot Casserole datalogging on the robot during testing or matches.
 * Will write lines into a CSV file with a unique name between calls to init() and close().
 * output_dir is hardcoded to point to a specific 2016 folder on a flash drive connected to the
 * roboRIO. <br>
 * <br>
 * USAGE:
 * <ol>
 * <li>Instantiate Class</li>
 * <li>Create global variables containing arrays of strings to represent the column (data vector)
 * names and units</li>
 * <li>During teleop init or autonomous init, call the init() function to start logging data to a
 * new file.</li>
 * <li>Once per loop, call the writeData() method with the full list of values to write to file (all
 * must be converted to doubles).</li>
 * <li>During DisabledInit, call the close() method to close out any file which was being written to
 * while the robot was doing something.</li>
 * <li>Post-match or -practice, extract the data logs from the USB drive(maybe using FTP?) and view
 * with excel or your favourite software.</li>
 * </ol>
 * 
 * 
 */

public class simpleCsvLogger {

    long log_write_index;
    String log_name = null;
    String output_dir = "/U/data_captures/"; // USB drive is mounted to /U on roboRIO
    BufferedWriter log_file = null;
    boolean log_open = false;



    /**
     * Determines a unique file name, and opens a file in the data captures directory and writes the
     * initial lines to it.
     * 
     * @param data_fields A set of strings for signal names to write into the file
     * @param units_fields A set of strings for signal units to write into the file
     * @return 0 on successful log open, -1 on failure
     */
    public int init(String[] data_fields, String[] units_fields) {

        if (log_open) {
            System.out.println("Warning - log is already open!");
            return 0;
        }

        log_open = false;
        System.out.println("Initalizing Log file...");
        try {
            // Reset state variables
            log_write_index = 0;

            // Determine a unique file name
            log_name = output_dir + "log_" + getDateTimeString() + ".csv";

            // Open File
            FileWriter fstream = new FileWriter(log_name, true);
            log_file = new BufferedWriter(fstream);

            // Write user-defined header line
            for (String header_txt : data_fields) {
                log_file.write(header_txt + ", ");
            }
            // End of line
            log_file.write("\n");


            // Write user-defined units line
            for (String header_txt : units_fields) {
                log_file.write(header_txt + ", ");
            }
            // End of line
            log_file.write("\n");

        }
        // Catch ALL the errors!!!
        catch (IOException e) {
            System.out.println("Error initializing log file: " + e.getMessage());
            return -1;
        }
        System.out.println("done!");
        log_open = true;
        return 0;

    }



    /**
     * Write a list of doubles to the output file, assuming it's open. Creates a new line in the
     * .csv log file.
     * 
     * @param data_elements Values to write (any number of doubles, each as its own argument).
     *        Should have the same number of arguments here as signal names/units set during the
     *        call to init()
     * @return 0 on write success, -1 on failure.
     */
    public int writeData(double... data_elements) {
        String line_to_write = "";

        if (log_open == false) {
            System.out.println("Error - Log is not yet opened, cannot write!");
            return -1;
        }

        try {

            // Write user-defined data
            for (double data_val : data_elements) {
                line_to_write = line_to_write.concat(Double.toString(data_val) + ", ");
            }

            // End of line
            line_to_write = line_to_write.concat("\n");

            // write constructed string out to file
            log_file.write(line_to_write);

        }
        // Catch ALL the errors!!!
        catch (IOException e) {
            System.out.println("Error writing to log file: " + e.getMessage());
            return -1;
        }

        log_write_index++;
        return 0;
    }



    /**
     * Clears the buffer in memory and forces things to file. Generally a good idea to use this as
     * infrequently as possible (because it increases logging overhead), but definitely use it
     * before the roboRIO might crash without a proper call to the close() method (during brownout,
     * maybe?)
     * 
     * @return Returns 0 on flush success or -1 on failure.
     */
    public int forceSync() {
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
     * order to write to a new file.
     * 
     * @return -1 on failure to close, 0 on success
     */
    public int close() {

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


    private String getDateTimeString() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        df.setTimeZone(TimeZone.getTimeZone("US/Central"));
        return df.format(new Date());
    }

}
