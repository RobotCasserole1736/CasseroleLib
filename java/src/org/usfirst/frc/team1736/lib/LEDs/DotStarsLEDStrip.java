package org.usfirst.frc.team1736.lib.LEDs;

import java.util.Arrays;
import java.util.TimerTask;
import edu.wpi.first.wpilibj.SPI;


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
 * Driver for Adafruit APA102 DotStar LED strips. Maintains an internal buffer of all LED data, and
 * periodically sends it off to the data over the SPI bus. It is currently configured to utilize the
 * MXP SPI port (not the primary one on labeled the RIO). This will be made configurable in the
 * future. <br>
 * Datasheet - https://www.adafruit.com/datasheets/APA102.pdf <br>
 * <br>
 * USAGE:
 * <ol>
 * <li>Instantiate Class, with number of LEDS in strip. This will start periodic updates to the
 * LED's in the background</li>
 * <li>Call color classes to change led colors. Changes are automatically pushed out to the strip.
 * </li>
 * </ol>
 * 
 * 
 */


public class DotStarsLEDStrip implements CasseroleLEDInterface {

    // Thread stuff
    // Thread to run the periodic update function on
    java.util.Timer timerThread;

    // Constant values for fixed things in the serial data stream
    static byte[] startFrame = {0x00, 0x00, 0x00, 0x00};
    static byte[] endFrame = {(byte) (255), (byte) (255), (byte) (255), (byte) (255)}; // Java is
                                                                                       // stupid and
                                                                                       // doesn't
                                                                                       // like
                                                                                       // unsigned
                                                                                       // things.

    // Offsets within the stream
    static int globalOffset = 0;
    static int blueOffset = 2; // Note that green/blue are inverted from what the datasheet says
                               // they should be
    static int greenOffset = 1;
    static int redOffset = 3;
    static int bytesPerLED = 4;
    static int led0Offset = startFrame.length;
    static int endFrameOffset; // dependent on number of LED's

    int num_leds;

    // SPI coms object from FRC
    SPI spi;
    static final int SPI_CLK_RATE = 13000000; // Total guess at max clock rate - 512KHz is called
                                              // out in an example in the datasheet, but 13MHz seems
                                              // to also work.

    // Color Buffer - all bytes to send out from the
    byte[] ledBuffer;

    // byte restrictions
    int ledMaxVal = 255;

    // State variables
    boolean newBuffer; // true when the ledBuffer has been updated since the last time it was
                       // written to the LEDs


    /**
     * Constructor for led strip class
     * 
     * @param numLEDs number of LED's in the total strip.
     */
    public DotStarsLEDStrip(int numLEDs) {

        // Number of bytes in color buffer needed - each LED has 4 bytes (1 brightness, then 1 for
        // RGB each),
        // plus the start and end frame.
        num_leds = numLEDs;
        int num_bytes_for_strip = 4 * numLEDs + startFrame.length + endFrame.length;
        endFrameOffset = 4 * numLEDs + startFrame.length;

        // Initialize color buffer
        ledBuffer = new byte[num_bytes_for_strip];

        // Write in the start/end buffers
        for (int i = 0; i < startFrame.length; i++)
            ledBuffer[i] = startFrame[i];
        for (int i = 0; i < endFrame.length; i++)
            ledBuffer[i + endFrameOffset] = endFrame[i];

        // mark buffer as not-yet-written-to-the-LEDs
        newBuffer = true;

        // Initialize SPI coms on the Offboard port
        spi = new SPI(SPI.Port.kMXP);
        spi.setMSBFirst();
        spi.setClockActiveLow();
        spi.setClockRate(SPI_CLK_RATE);
        spi.setSampleDataOnFalling();
        timerThread = new java.util.Timer("DotStar LED Strip Control");
        timerThread.schedule(new DotStarsTask(this), (long) (m_update_period_ms), (long) (m_update_period_ms));

    }


    /**
     * Send the current ledBuffer to the string IF it needs to be sent
     * 
     * @return 0 on successful write, -1 on failure
     */
    private int updateColors() {
    	@SuppressWarnings("unused")
        int ret_val = 0;
    	final int CHUNK_SIZE = 127; // we can only TX 127 bytes at a time

        // If we need to write something, attempt to put it on the SPI port
        if (newBuffer) {
            // Make local copy of ledBuffer to help make color changing clean
            byte[] temp_ledBuff = Arrays.copyOfRange(ledBuffer, 0, ledBuffer.length);

            // Chunk the TX'es into smaller size, otherwise we get -1 returned from spi.write
            //This is undocumented, and was found by experimentation.
            for (int offset = 0; offset < temp_ledBuff.length; offset = offset + CHUNK_SIZE) {
                int start_index = offset;
                int end_index = Math.min(offset + CHUNK_SIZE, temp_ledBuff.length);
                int size = end_index - start_index;
                byte[] tx_array = Arrays.copyOfRange(temp_ledBuff, start_index, end_index);
            	ret_val = spi.write(tx_array, size); //I think this returns number of bytes actually written?
        	}

            

        }

        // Mark the buffer as written to the LEDs
        newBuffer = false;

        return 0;
    }


    /**
     * Clears all contents in the color buffer. This turns off all LED's. Be sure to call the
     * updateColors() class some time after this one to actually send the commanded colors to the
     * actual strip.
     */

    public void clearColorBuffer() {
        for (int i = 0; i < num_leds; i++) {
            setLEDColor(i, 0, 0, 0);
        }
        // Mark the buffer as updated
        newBuffer = true;
        // we're done!
        return;
    }


    /**
     * sets a particular LED in the string to a certain color
     * 
     * @param index Index in the LED to set. 0 is the furthest from the roboRIO, N is the closest.
     * @param r Red value for the color. Provide as a double in the range of 0 (off) to 1 (full on)
     * @param g Green value for the color. Provide as a double in the range of 0 (off) to 1 (full
     *        on)
     * @param b Blue value for the color. Provide as a double in the range of 0 (off) to 1 (full on)
     */

    public void setLEDColor(int index, double r, double g, double b) {

        ledBuffer[index * bytesPerLED + led0Offset + globalOffset] = convDoubletoByte(1);
        ledBuffer[index * bytesPerLED + led0Offset + blueOffset] = convDoubletoByte(b);
        ledBuffer[index * bytesPerLED + led0Offset + greenOffset] = convDoubletoByte(g);
        ledBuffer[index * bytesPerLED + led0Offset + redOffset] = convDoubletoByte(r);
        // Mark the buffer as updated
        newBuffer = true;
        // we're done!
        return;
    }


    /**
     * convert a double in the range 0-1 to a byte of value 0x00 to 0xFF. This normalizes the full
     * range of the LED brightness to the 0-1 range, hiding the implementation from the users.
     * 
     * @param in
     * @return
     */
    private byte convDoubletoByte(double in) {
        // Constrain the input to the defined [0,1] input range
        in = Math.min(Math.max(in, 0.0), 1.0);
        // Scale and round
        in = Math.round(in * ledMaxVal);
        // Stupid offsetting b/c java doesn't support unsigned operations
        // This is 2's complement sign conversion. If you don't know what that
        // means, please don't touch this logic.
        if (in > (ledMaxVal + 1) / 2)
            in = in - (ledMaxVal + 1);

        return (byte) in;
    }


    // Java multithreading magic. Do not touch.
    // Touching will incour the wrath of Cthulhu, god of java and LED Strips.
    // May the oceans of 1's and 0's rise to praise him.
    private class DotStarsTask extends TimerTask {
        private DotStarsLEDStrip m_leds;


        public DotStarsTask(DotStarsLEDStrip leds) {
            if (leds == null) {
                throw new NullPointerException("Given DotStars Controller Class was null");
            }
            m_leds = leds;
        }


        @Override
        public void run() {
            m_leds.updateColors();
        }
    }



}
