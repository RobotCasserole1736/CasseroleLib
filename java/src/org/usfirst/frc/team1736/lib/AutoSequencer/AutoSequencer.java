package org.usfirst.frc.team1736.lib.AutoSequencer;

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

import java.util.ArrayList;

import edu.wpi.first.wpilibj.Timer;

/**
 * Casserole Autonomous mode event sequencer. Provides an infrastructure for defining autonomous
 * actions in a multi-layer state-machine like fashion. <br>
 * <br>
 * Events will be executed in the same order they are added. Once start() is called, an event's
 * update() method will be called repeatedly until isDone() returns true. At this point, the next
 * event in the primary timeline is run. During each timeline event, in addition to repeatedly
 * calling the update() method, all child events are evaluated to see if they too need to be
 * updated.
 */
public class AutoSequencer {

    static ArrayList<AutoEvent> events = new ArrayList<AutoEvent>();

    static AutoEvent activeEvent = null;

    public static long globalUpdateCount = 0;

    public static int globalEventIndex = 0;


    /**
     * Add sequential event to the primary timeline.
     * 
     * @param event_in
     */
    public static void addEvent(AutoEvent event_in) {
        events.add(event_in);
        System.out.println("[Auto] New event registered - " + event_in.getClass().getName());
    }
    
    public static void clearAllEvents() {
        events.clear();
        System.out.println("[Auto] Cleared event list");
    }


    /**
     * Reset to the start of the autonomous sequence.
     */
    public static void start() {
        globalEventIndex = 0;
        globalUpdateCount = 0;
        
        System.out.println("[Auto] Starting...");

        if (events.size() > 0) {
            activeEvent = events.get(globalEventIndex);
            System.out.println("[Auto] Starting new auto event " + activeEvent.getClass().getName());
            activeEvent.userStart();
        }
    }


    /**
     * Stop anything which might be running now. Will call the userForceStop() on any presently
     * running events.
     */
    public static void stop() {
        // if something is running, we'll need to stop it.
        if (activeEvent != null) {

            // Force stop this event and its children
            activeEvent.forceStopAllChildren();
            activeEvent.userForceStop();
        }
        System.out.println("[Auto] Stopping...");

        // Set activeEvent to nothing running state.
        activeEvent = null;
    }


    public static void update() {

        // Don't bother to do anything if there is no active event right now.
        if (activeEvent != null) {

            // Update the active event. This will probably set motors or stuff like that.
            activeEvent.update();

            // Check if there are any children to update.
            if (activeEvent.childEvents.size() > 0) {
                for (AutoEvent child : activeEvent.childEvents) {

                    // Evaluate if child needs to start running
                    if (child.isRunning == false & child.isTriggered()) {
                        child.isRunning = true;
                    }
                    // Call update if the child is running
                    if (child.isRunning == true) {
                        child.update();
                    }
                    // Evaluate if the child needs to be stopped
                    if (child.isRunning == true & child.isDone()) {
                        child.isRunning = false;
                    }

                }
            }

            // Check if active event has completed. Move on to the next one if this one is done.
            // Note this sequence guarantees each event's update is called at least once.
            if (activeEvent.isDone()) {
            	//This event is done - determine if we are done with auto, or need to do the next event.
            	
                activeEvent.forceStopAllChildren(); // Just in case the user is sloppy and leaves
                                                    // child events running when the parent
                                                    // finishes.
                globalEventIndex++;
                
                // See what our new current event is.
                if (globalEventIndex >= events.size()) {
                    // terminal condition. we have no more states to run. Stop running things.
                    activeEvent = null;
                    System.out.println("[Auto] Finished all events in sequence.");
                    return;
                } 
                
                activeEvent = events.get(globalEventIndex);
                System.out.println("[Auto] Starting new auto event " + activeEvent.getClass().getName());
                activeEvent.userStart();
            }
            
        if(globalUpdateCount % 50 == 0){
        	System.out.println("[Auto] Running. timestep = " + Double.toString(globalUpdateCount*0.02) + "s | ActualTime = " + Double.toString(Timer.getFPGATimestamp()));
        }


        }
        globalUpdateCount++;
        

    }


    /**
     * Determine if the sequencer is active. As long as this is true, any calls to update() will
     * trigger various events run.
     * 
     * @return True if the auto sequencer is executing something, false otherwise
     */
    public static boolean isRunning() {
        return activeEvent != null;
    }


}