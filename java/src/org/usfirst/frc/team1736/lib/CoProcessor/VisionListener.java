package org.usfirst.frc.team1736.lib.CoProcessor;

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

import java.util.concurrent.locks.ReentrantLock;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import edu.wpi.first.wpilibj.Timer;

/**
* DESCRIPTION: <br>
* Full class to set up a UDP listener, look for UDP packets from a vision coprocessor (such as a Beaglebone Black (BBB)), and interpret 
* the json data into target observations. Note that the JSON object structures and the content of the data is
* tightly tied with the functionality implemented on the coprocessor - this code can likely not be fully understood
* without looking into that code as well! <br>
* <br>
* USAGE:
* <ol>
* <li>Instantiate the reciever with the listen address and port. </li>
* <li>Call the start() method when you are ready to start listening for frames. </li>
* <li>Once per control loop, call the sampleLatestData() method to load data from the latest frame. </li>
* <li>During the control loop, call the other getter functions to extract info about this latest frame. </li>
* </ol>
* 
* 
*/

public class VisionListener {
    UDPReceiver listenClient;
    boolean coprocessorActive;
    
    private String addr;
    private int port;
    JSONParser parser;
    
    //We expect the BBB to send updates at a 30Hz rate (one per frame), so we'll say 35 just for yucks.
    // This constant is used to help the timing of the background update thread
    // so it doesn't chew too many clock cycles just waiting
    private double EXPECTED_MAX_UPDATE_RATE_HZ = 35.0;
    
    //If this much time goes by without getting a packet, we will start to say the processor is no longer turned on.
    private static final double COPROCESSOR_ACTIIVE_TIMEOUT_SEC = 2.0;
    
    // Mutithreading academic dissertation time! 
    // The present and previous observations represent a total set of data which must be updated atomically. 
    // We expect them to be updated asynchronously in the background by the listener thread (with updates    
    // driven by udp packet arrivals). While we are in the process of updating them, they are in an _inconsistant_
    // state - that is to say they are not valid for use by other threads. Therefore, during the whole update process,
    // we must LOCK that critical section so other threads which access either observation. 
    private JSONObject currObservation;
    private ReentrantLock observationLock;
    private double mostRecentPacketTime;
    
    //This is the json object which the control system considers as "current". Other observations will continue to be gathered in the background,
    //but this object will not be updated until the control system thread calls the sampleLatestData() method.
    private JSONObject userCurrObservation;
    private double userCurrPacketRxTime_sys_sec;
    
    //Values used for axis M1011
    double targetWidthFeet = 1.25;
    int FOVWidthPixel = 640;
    double tangentTheta = 1.0724;
    
    /**
     * Constructor for the Vision Coprocessor listener socket. Sets up internal variables to get ready
     * for information to be transmitted from the coprocessor.
     * @param listen_to_addr String of the IP address of the coprocessor (For example, "10.17.36.8")
     * @param listen_on_port integer port number to listen on. Usually between 5800 and 5810 per FMS whitepaper. Must match whatever port the coprocessor is sending information to.
     */
    public VisionListener(String listen_to_addr, int listen_on_port){
        addr = listen_to_addr;
        port = listen_on_port;
        coprocessorActive = false;
        currObservation = new JSONObject();
        userCurrObservation = new JSONObject();
        parser = new JSONParser();
        observationLock = new ReentrantLock();
        
    }
    
    /**
     * Starts the listener in the background to look for and interpret UDP packets from the coprocessor. <br><br>
     * This method MUST be called after the constructor, but before any attempt is made to use the data from the coprocessor.
     */
    public void start(){
        
        listenClient = new UDPReceiver(addr, port);
        coprocessorActive = false;
        
        //Ensure lock starts unlocked.
        if(observationLock.isLocked()){
            observationLock.unlock();
        }
        
        Thread listenerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(true){
                        update();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        
        listenerThread.setName("CasseroleVisionCoprocessorListenerThread");
        listenerThread.setPriority(Thread.MIN_PRIORITY+2); //Sure, this seems like a reasonable priority!
        listenerThread.start();
    }
    
    private void update(){
        String rx_data = listenClient.getPacket();
        
        
        if(rx_data.length() != 0){
        	mostRecentPacketTime = Timer.getFPGATimestamp();
            while(observationLock.tryLock()==false){} //lazy man's spinlock
            //Begin critical section
            try {
                currObservation = (JSONObject) parser.parse(rx_data); 
            } catch (ParseException e) {
                System.out.println("Error: Cannot parse recieved UDP json data: " + e.toString());
                e.printStackTrace();
            }
            //End critical section
            observationLock.unlock();
        }
        
        //Sleep for the expected update interval, to keep this thread from chewing too many clock cycles.
        try {
            Thread.sleep((long)((1.0/EXPECTED_MAX_UPDATE_RATE_HZ)*1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Locks the most recent data from the coprocessor for use. Should be called by controls thread to indicate the control algorithms
     * are ready to start to use a new frame. The information returned by all the other getter methods in this class is based on the
     * data available at the most recent call to this method. Call this method once per control loop, before attempting to use any data
     * from the coprocessor.
     */
    public void sampleLatestData(){
        while(observationLock.tryLock()==false){} //lazy man's spinlock
        userCurrObservation = currObservation;
        userCurrPacketRxTime_sys_sec = mostRecentPacketTime;
        
        if(Timer.getFPGATimestamp() > (mostRecentPacketTime + COPROCESSOR_ACTIIVE_TIMEOUT_SEC)){
            coprocessorActive = false;
        } else {
            coprocessorActive = true;
        }
        observationLock.unlock();
    }
    
    private double genericDoubleGet(String objname){
        double val = -1.0;
        if(userCurrObservation.containsKey(objname)){
        	if(userCurrObservation.get(objname).getClass().equals(Long.class)){
              val = (double) ((Long)userCurrObservation.get(objname)).doubleValue();
        	} else if (userCurrObservation.get(objname).getClass().equals(Double.class)){
              val = (double) userCurrObservation.get(objname);
        	} else {
        		System.out.println("Warning: Cannot parse " + objname + " into a number. Malformed Json?");
        	}
             
        } 
        return val;
    }
    
    private double genericDouble1DArrayIndexGet(String objname, int idx){
        double val = -1.0;
        
        if(userCurrObservation.containsKey(objname)){
            JSONArray tmp = (JSONArray) userCurrObservation.get(objname);
            if(tmp.size() > idx){
                val = ((Long)tmp.get(idx)).doubleValue();
            }
        } 
        return val;
    }
    
    private double genericAlreadyDouble1DArrayIndexGet(String objname, int idx){
        double val = -1.0;
        
        if(userCurrObservation.containsKey(objname)){
            JSONArray tmp = (JSONArray) userCurrObservation.get(objname);
            if(tmp.size() > idx){
                val = (double)tmp.get(idx);
            }
        } 
        return val;
    }
    
    private double genericDouble1DArraySizeGet(String objname){
        
        if(userCurrObservation.containsKey(objname)){
             JSONArray tmp = (JSONArray) userCurrObservation.get(objname);
             return tmp.size();
        } else {
            return -1.0;
        }
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Public interface methods
    ///////////////////////////////////////////////////////////////////////////
    
    /**
     * @return True if a valid packet has been decoded from the coprocessor recently, false otherwise
     */
    public boolean isCoProcessorAlive(){
        return coprocessorActive;
    }
    
    /**
     * @return Number of targets observed in frame, or -1 if no valid frame available.
     */
    public double getNumTargetsObserved(){
        return genericDouble1DArraySizeGet("Xs");
    }

    /**
     * @param tgt_idx The integer index of the target of interest (should be between 0 and getNumTargetsObserved()-1)
     * @return X coordinate of the specified target observed in frame, or -1 if no target matches provided index.
     */
    public double getX(int tgt_idx){
        return genericDouble1DArrayIndexGet("Xs",tgt_idx);
    }

    /**
     * @param tgt_idx The integer index of the target of interest (should be between 0 and getNumTargetsObserved()-1)
     * @return Y coordinate of the specified target observed in frame, or -1 if no target matches provided index.
     */
    public double getY(int tgt_idx){
        return genericDouble1DArrayIndexGet("Ys",tgt_idx);
    }

    /**
     * @param tgt_idx The integer index of the target of interest (should be between 0 and getNumTargetsObserved()-1)
     * @return Enclosed area of the specified target observed in frame, or -1 if no target matches provided index.
     */
    public double getArea(int tgt_idx){
        return genericAlreadyDouble1DArrayIndexGet("boundedAreas",tgt_idx);
    }

    /**
     * @param tgt_idx The integer index of the target of interest (should be between 0 and getNumTargetsObserved()-1)
     * @return Width of the specified target observed in frame, or -1 if no target matches provided index.
     */
    public double getWidth(int tgt_idx){
        return genericDouble1DArrayIndexGet("widths",tgt_idx);
    }

    /**
     * @param tgt_idx The integer index of the target of interest (should be between 0 and getNumTargetsObserved()-1)
     * @return Height of the specified target observed in frame, or -1 if no target matches provided index.
     */
    public double getHeight(int tgt_idx){
        return genericDouble1DArrayIndexGet("heights",tgt_idx);
    }

    /**
     * @return Coprocessor averaged data output rate in Frames per Second
     */
    public double getFPS(){
        return genericDoubleGet("fps");
    }

    /**
     * @return Number of frames processed by the coprocessor so far. 
     */
    public double getFrameCounter(){
        return genericDoubleGet("frameCounter");
    }

    /**
     * @return Number of milliseconds it took to process the current frame from image down to target data
     */
    public double getProcTimeMs(){
        return genericDoubleGet("procTime");
    }

    /**
     * @return Percent load of the coprocessor CPU's
     */
    public double getCpuLoad(){
        return genericDoubleGet("cpuLoad");
    }

    /**
     * @return Percent load of the coprocessor's onboard RAM
     */
    public double getMemLoad(){
        return genericDoubleGet("memLoad");
    }
    
    /**
     * @return RIO System time when the most recent packet was received.
     */
    public double getPacketRxSystemTime(){
    	return (double)userCurrPacketRxTime_sys_sec;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // End Public interface methods
    ///////////////////////////////////////////////////////////////////////////
    
    /**
     * The main function is used as a test which can be run on a user's PC. It is NOT intended to be called
     *  by any part of the robot code!
     */
    public static void main(String[] args) {
        VisionListener testVisionListener = new VisionListener("127.0.0.1", 5800);
        testVisionListener.start();
        
        while(true){
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            testVisionListener.sampleLatestData();
            System.out.print(testVisionListener.getProcTimeMs());
            System.out.print(" | ");
            System.out.print(testVisionListener.getNumTargetsObserved());
            System.out.print(" | ");
            System.out.print(testVisionListener.getX(2));
            System.out.print(" | ");
            System.out.print(testVisionListener.getY(2));
            System.out.print(" | ");
            System.out.print(testVisionListener.isCoProcessorAlive());
            System.out.println(" | ");
        }

    }

}
