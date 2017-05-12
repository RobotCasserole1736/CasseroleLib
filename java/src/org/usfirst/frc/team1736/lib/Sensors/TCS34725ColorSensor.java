package org.usfirst.frc.team1736.lib.Sensors;

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


import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.I2C.Port;

/**
 * DESCRIPTION: <br>
 * Simple API for the TCS34725 color sensor from adafruit Provides interfacing for initializing the
 * sensor and performing qualified reads of color values. Coded with lots and lots of help from
 * https://github.com/adafruit/Adafruit_TCS34725. Thanks KTOWN!!! If I ever meet you at a non-FRC
 * event, I owe you a drink. <br>
 * USAGE:
 * <ol>
 * <li>Instantiate class.</li>
 * <li>Call init() once to set up the sensor</li>
 * <li>Once per periodic loop, call the readColors() method to get data from the sensor.</li>
 * <li>After the readColors() call, use the getCOLOR() methods to access the most recent color
 * readings (RGBC) from the sensor.</li>
 * </ol>
 *
 */
public class TCS34725ColorSensor {
    // FRC I2C object, since color sensor will work over that
    I2C color_sen;

    // I2C constants
    @SuppressWarnings("unused")
	    private static final class I2C_constants{
	    private static final int TCS34725_I2C_ADDR = (0x29);
	    private static final int TCS34725_COMMAND_BIT = (0x80);
	
	    private static final int TCS34725_ENABLE = (0x00);
	    private static final int TCS34725_ENABLE_AIEN = (0x10); /* RGBC Interrupt Enable */
	    private static final int TCS34725_ENABLE_WEN =
	            (0x08); /* Wait enable - Writing 1 activates the wait timer */
	    private static final int TCS34725_ENABLE_AEN =
	            (0x02); /* RGBC Enable - Writing 1 actives the ADC, 0 disables it */
	    private static final int TCS34725_ENABLE_PON =
	            (0x01); /* Power on - Writing 1 activates the internal oscillator, 0 disables it */
	    private static final int TCS34725_CONFIG = (0x0D);
	    private static final int TCS34725_CONFIG_WLONG =
	            (0x02); /* Choose between short and long (12x) wait times via TCS34725_WTIME */
	    private static final int TCS34725_CONTROL = (0x0F); /* Set the gain level for the sensor */
	    private static final int TCS34725_ID =
	            (0x12); /* 0x44 = TCS34721/TCS34725, 0x4D = TCS34723/TCS34727 */
	    private static final int TCS34725_STATUS = (0x13);
	    private static final int TCS34725_STATUS_AINT = (0x10); /* RGBC Clean channel interrupt */
	    private static final int TCS34725_STATUS_AVALID =
	            (0x01); /* Indicates that the RGBC channels have completed an integration cycle */
	    private static final int TCS34725_CDATAL = (0x14); /* Clear channel data */
	    private static final int TCS34725_CDATAH = (0x15);
	    private static final int TCS34725_RDATAL = (0x16); /* Red channel data */
	    private static final int TCS34725_RDATAH = (0x17);
	    private static final int TCS34725_GDATAL = (0x18); /* Green channel data */
	    private static final int TCS34725_GDATAH = (0x19);
	    private static final int TCS34725_BDATAL = (0x1A); /* Blue channel data */
	    private static final int TCS34725_BDATAH = (0x1B);
	    private static final int TCS34725_ATIME = (0x01); /* Integration time */
	
	    private static final int TCS34725_INTEGRATIONTIME_2_4MS = 0xFF;
	    /** < 2.4ms - 1 cycle - Max Count: 1024 */
	    private static final int TCS34725_INTEGRATIONTIME_24MS = 0xF6;
	    /** < 24ms - 10 cycles - Max Count: 10240 */
	    private static final int TCS34725_INTEGRATIONTIME_50MS = 0xEB;
	    /** < 50ms - 20 cycles - Max Count: 20480 */
	    private static final int TCS34725_INTEGRATIONTIME_101MS = 0xD5;
	    /** < 101ms - 42 cycles - Max Count: 43008 */
	    private static final int TCS34725_INTEGRATIONTIME_154MS = 0xC0;
	    /** < 154ms - 64 cycles - Max Count: 65535 */
	    private static final int TCS34725_INTEGRATIONTIME_700MS = 0x00;
	    /** < 700ms - 256 cycles - Max Count: 65535 */
	
	    private static final int TCS34725_GAIN_1X = 0x00;
	    /** < No gain */
	    private static final int TCS34725_GAIN_4X = 0x01;
	    /** < 4x gain */
	    private static final int TCS34725_GAIN_16X = 0x02;
	    /** < 16x gain */
	    private static final int TCS34725_GAIN_60X = 0x03; /** < 60x gain */
    }
    


    // State Variables
    /** True if sensor has been initialized, false if not */
    public boolean sensor_initalized;
    /** True if the last read from the sensor was good, bad if data was corrupted */
    public boolean good_data_read;
    private int red_val;
    private int green_val;
    private int blue_val;
    private int clear_val;


    /**
     * Constructor for TCS34725 Color Sensor from Adafruit. Initializes internal data structures and
     * opens I2C coms to the device.
     */
    TCS34725ColorSensor() {
        color_sen = new I2C(Port.kOnboard, I2C_constants.TCS34725_I2C_ADDR);
        sensor_initalized = false;
        good_data_read = false;

    }


    /**
     * Initializes the actual sensor state so colors can be read. By default the sensor powers up to
     * a "disabled" state. This enables it. Additionally, checks the sensor has the proper internal
     * ID and sets hard-coded gains and integrator times.
     * 
     * @return 0 on success, -1 on failure to initialize
     */
    public int init() {
        sensor_initalized = false;

        System.out.print("Initalizing Color Sensor...");

        byte[] whoamiResponse = new byte[1];
        whoamiResponse[0] = 0x00;

        // Check we're actually connected to the sensor
        color_sen.read(I2C_constants.TCS34725_ID, 1, whoamiResponse);
        if ((whoamiResponse[0] != 0x44) && (whoamiResponse[0] != 0x10)) {
            System.out.println("\nError - whoami register mismatch on Color Sensor! Cannot Initalize!");
            return -1;
        }

        // Set the integration time
        color_sen.write(I2C_constants.TCS34725_ATIME, I2C_constants.TCS34725_INTEGRATIONTIME_2_4MS);

        // Set the gain
        color_sen.write(I2C_constants.TCS34725_CONTROL, I2C_constants.TCS34725_GAIN_1X);

        // Power-on the sensor's internals (it defaults to off)
        color_sen.write(I2C_constants.TCS34725_ENABLE, I2C_constants.TCS34725_ENABLE_PON);
        safeSleep(3);
        color_sen.write(I2C_constants.TCS34725_ENABLE, I2C_constants.TCS34725_ENABLE_PON | I2C_constants.TCS34725_ENABLE_AEN);

        System.out.println("done!");
        sensor_initalized = true;
        return 0;

    }


    /**
     * Queries the sensor for the red, green, blue, and clear values Qualifies the read to ensure
     * the sensor has not been reset since the last read.
     * 
     * @return 0 on read success, -1 on failure.
     */
    public int readColors() {
        byte[] red_bytes = {0, 0};
        byte[] green_bytes = {0, 0};
        byte[] blue_bytes = {0, 0};
        byte[] clear_bytes = {0, 0};

        byte[] enable_test_buf = {0};

        // Don't bother doing anything if the sensor isn't initialized
        if (!sensor_initalized) {
            System.out.println("Error: Attempt to read from color sensor, but it's not initalized!");
            return -1;
        }

        // Call the read bad if the enable register isn't set properly
        // (this gets reset to a different value if the sensor is power-cycled)
        color_sen.read(I2C_constants.TCS34725_ENABLE, 1, enable_test_buf);
        if (enable_test_buf[0] != (I2C_constants.TCS34725_ENABLE_PON | I2C_constants.TCS34725_ENABLE_AEN)) {
            System.out.println(
                    "Error: Attempt to read from color sensor, but the enable register did not read as expected! Sensor has probably been reset.");
            sensor_initalized = false;
            good_data_read = false;
            return -1;
        }


        // Read data off of the sensor
        color_sen.read(I2C_constants.TCS34725_RDATAH, 2, red_bytes);
        color_sen.read(I2C_constants.TCS34725_GDATAH, 2, green_bytes);
        color_sen.read(I2C_constants.TCS34725_BDATAH, 2, blue_bytes);
        color_sen.read(I2C_constants.TCS34725_CDATAH, 2, clear_bytes);

        // Perform typecasting and bit-shifting on the recieved data.
        red_val = (int) ((red_bytes[1] << 8) | (red_bytes[0] & 0xFF));
        green_val = (int) ((green_bytes[1] << 8) | (green_bytes[0] & 0xFF));
        blue_val = (int) ((blue_bytes[1] << 8) | (blue_bytes[0] & 0xFF));
        clear_val = (int) ((clear_bytes[1] << 8) | (clear_bytes[0] & 0xFF));

        // Set that we've got good data and return.
        good_data_read = true;
        return 0;

    }


    /**
     * Returns the most recent red intensity read from the sensor
     * 
     * @return most recently read red intensity
     */
    public int getRedVal() {
        return red_val;
    }


    /**
     * Returns the most recent green intensity read from the sensor
     * 
     * @return most recently read green intensity
     */
    public int getGreenVal() {
        return green_val;
    }


    /**
     * Returns the most recent blue intensity read from the sensor
     * 
     * @return most recently read blue intensity
     */
    public int getBlueVal() {
        return blue_val;
    }


    /**
     * Returns the most recent overall intensity read from the sensor
     * 
     * @return most recently read overall intensity
     */
    public int getClearVal() {
        return clear_val;
    }


    /**
     * Wrapped Thread.sleep call to safely delay for a given period of time. Presumes there is
     * nothing to do if sleeping is interrupted.
     * 
     * @param milliseconds Time to delay for in ms.
     */
    private void safeSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
