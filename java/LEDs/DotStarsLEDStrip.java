/**
 * 
 */
package org.usfirst.frc.team1736.robot;

import java.util.Arrays;
import java.util.TimerTask;

import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.SPI;

/**
 * @author gerthcm
 *
 */
public class DotStarsLEDStrip {
	//Datasheet - https://www.adafruit.com/datasheets/APA102.pdf
	
	//Thread stuff
	// Thread to run the periodic update function on 
	java.util.Timer timerThread; 

	//period between periodic function calls
	//in milliseconds
	double m_update_period_ms = 100; 
	
	//Constant values for fixed things in the serial data stream
	static byte[] startFrame = {0x00, 0x00, 0x00, 0x00};
	static byte[] endFrame = {(byte)(255), (byte)(255), (byte)(255), (byte)(255)}; //Java is stupid and doesn't like unsigned things. 
	
	//Offsets within the stream
	static int globalOffset = 0;
	static int blueOffset = 2; //Note that green/blue are inverted from wh at the dtatsheet says they should be
	static int greenOffset = 1;
	static int redOffset = 3;	
	static int bytesPerLED = 4;
	static int led0Offset = startFrame.length;
	static int endFrameOffset; //dependent on number of LED's
	
	int num_leds;

	//SPI coms object from FRC
	SPI spi;
	static final int SPI_CLK_RATE = 13000000; //Total guess at max clock rate - 512KHz is called out in an example in the datasheet, so I used that
	
	//Color Buffer - all bytes to send out from the 
	byte[] ledBuffer;
	
	//byte restrictions
	int ledMaxVal = 255;
	
	//State variables
	boolean newBuffer; //true when the ledBuffer has been updated since the last time it was written to the LEDs
	
	//Audio Input
	AnalogInput audioIn;
	private static final int AUDIO_IN_PORT = 2;
	//scale/offset for microphone input
	private final double AUDIO_MIN_V = 0.2;
	private final double AUDIO_MAX_V = 3.0;
	
	/**
	 * Constructor for led strip class
	 * @param numLEDs - number of LED's in the total strip.
	 */
	DotStarsLEDStrip(int numLEDs){
		//Initalize the audio input
		audioIn = new AnalogInput(AUDIO_IN_PORT);
		
		//Number of bytes in color buffer needed - each LED has 4 bytes (1 brightness, then 1 for RGB each),
		// plus the start and end frame.
		num_leds = numLEDs;
		int num_bytes_for_strip = 4*numLEDs + startFrame.length + endFrame.length;
		endFrameOffset = 4*numLEDs + startFrame.length;
		
		//Initialize color buffer
		ledBuffer = new byte[num_bytes_for_strip];
		
		//Write in the start/end buffers
		for(int i = 0; i < startFrame.length; i++)
			ledBuffer[i] = startFrame[i];
		for(int i = 0; i < endFrame.length; i++)
			ledBuffer[i+endFrameOffset] = endFrame[i];
		
		//mark buffer as not-yet-written-to-the-LEDs
		newBuffer = true;
		
		//Initialize SPI coms on the Offboard port
		spi = new SPI(SPI.Port.kMXP);
		spi.setMSBFirst();
		spi.setClockActiveLow();
		spi.setClockRate(SPI_CLK_RATE); 
		spi.setSampleDataOnFalling();
		
        timerThread = new java.util.Timer();
        timerThread.schedule(new DotStarsTask(this), (long) (m_update_period_ms), (long) (m_update_period_ms));
		
	}
	
	
	/**
	 * Send the current ledBuffer to the string IF it needs to be sent
	 * @return 0 on successful write, -1 on failure
	 */
	public int updateColors(){
		int ret_val = 0;
		
		//If we need to write something, attempt to put it on the SPI port
		if(newBuffer){
			//Make local copy of ledBuffer to help make color changing clean
			byte[] temp_ledBuff = Arrays.copyOfRange(ledBuffer, 0, ledBuffer.length);
			
			//Chunk the TX'es into smaller size, since it looks like
			//we can only TX 128 bytes at a time
			for(int offset = 0; offset < temp_ledBuff.length; offset = offset+128){
				int start_index = offset;
				int end_index = Math.min(offset+128, temp_ledBuff.length);
				int size = end_index - start_index;
				byte[] tx_array = Arrays.copyOfRange(temp_ledBuff, start_index, end_index);
				ret_val =  spi.write(tx_array, size);
			}
			
		}
		
		//Mark the buffer as written to the LEDs 
		newBuffer = false;

		return 0;
	}
	
	
	/**
	 * Clears all contents in the color buffer. This turns off all LED's. Be sure to call the updateColors() class 
	 * some time after this one to actually send the commanded colors to the actual strip.
	 */
	
	public void clearColorBuffer(){
		for(int i = 0; i < num_leds; i++){
			setLEDColor(i,0,0,0);
		}
		//Mark the buffer as updated
		newBuffer = true;
		//we're done!
		return;
	}
	
	
	/**
	 * sets a particular LED in the string to a certain color
	 * @param index - index in the LED to set. 0 is the furthest from the roboRIO, N is the closest.
	 * @param r - red value for the color. Provide as a double in the range of 0 (off) to 1 (full on)
	 * @param g - green value for the color. Provide as a double in the range of 0 (off) to 1 (full on)
	 * @param b - blue value for the color. Provide as a double in the range of 0 (off) to 1 (full on)
	 */
	
	public void setLEDColor(int index, double r, double g, double b){
		
		ledBuffer[index*bytesPerLED + led0Offset + globalOffset] = convDoubletoByte(1);
		ledBuffer[index*bytesPerLED + led0Offset + blueOffset] = convDoubletoByte(b);
		ledBuffer[index*bytesPerLED + led0Offset + greenOffset] = convDoubletoByte(g);
		ledBuffer[index*bytesPerLED + led0Offset + redOffset] = convDoubletoByte(r);
		//Mark the buffer as updated
		newBuffer = true;
		//we're done!
		return;
	}
	
	/**
	 * Returns the current volume of sound outside the robot in a range of 0 to 1
	 * where 0 is the quietest and 1 is the loudest
	 * @return
	 */
	
	public double getAudioLevel(){
		return Math.min(1, Math.max(((audioIn.getVoltage() - AUDIO_MIN_V) / (AUDIO_MAX_V-AUDIO_MIN_V)),0));
	}
	
	/**
	 * convert a double in the range 0-1 to a byte of value 0x00 to 0xFF. This normalizes the full range of 
	 * the LED brightness to the 0-1 range, hiding the implementation from the users.
	 * @param in
	 * @return
	 */
	private byte convDoubletoByte(double in){
		//Constrain the input to the defined [0,1] input range
		in = Math.min(Math.max(in, 0.0), 1.0);
		//Scale and round
		in = Math.round(in * ledMaxVal);
		//Stupid offsetting b/c java doesn't support unsigned operations
		//This is 2's complement sign conversion. If you don't know what that
		//means, please don't touch this logic.
		if(in > (ledMaxVal+1)/2)
			in = in - (ledMaxVal + 1);
		
		return (byte)in;
	}
	
	
	//Java multithreading magic. Do not touch.
	//Touching will incour the wrath of Cthulhu, god of java and LED Strips.
	//May the oceans of 1's and 0's rise to praise him.
    private class DotStarsTask extends TimerTask 
    {
        private DotStarsLEDStrip m_leds;

        public DotStarsTask(DotStarsLEDStrip leds) 
        {
            if (leds == null) 
            {
                throw new NullPointerException("Given DotStars Controller Class was null");
            }
            m_leds = leds;
        }

        @Override
        public void run() 
        {
        	m_leds.updateColors();
        }
    }
	
	
	

}
