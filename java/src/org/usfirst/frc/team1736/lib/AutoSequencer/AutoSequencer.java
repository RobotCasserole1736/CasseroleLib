package org.usfirst.frc.team1736.lib.AutoSequencer;

import java.util.ArrayList;
/**
 * Casserole Autonomous mode event sequencer. Provides an infrastructure for defining autonomous actions in a multi-layer state-machine like fashion.
 * <br><br>
 * Events will be executed in the same order they are added. Once start() is called, an event's update() method
 * will be called repeatedly until isDone() returns true. At this point, the next event in the primary timeline is run. 
 * During each timeline event, in addition to repeatedly calling the update() method, all child events are evaluated to
 * see if they too need to be updated.
 */
public class AutoSequencer {

	static ArrayList<AutoEvent> events = new ArrayList<AutoEvent>();
	
	static AutoEvent activeEvent = null;
	
	public static long globalUpdateCount = 0;
	
	public static int globalEventIndex = 0;
	
	/** 
	 * Add sequential event to the primary timeline. 
	 * @param event_in 
	 */
	public static void addEvent(AutoEvent event_in){
		events.add(event_in);
	}

	/**
	 * Reset to the start of the autonomous sequence.
	 */
	public static void start(){
		globalEventIndex = 0;
		globalUpdateCount = 0;
		
		if(events.size()>0){
			activeEvent = events.get(globalEventIndex);
		}
	}
	
	/**
	 * Stop anything which might be running now. Will call the userForceStop() on any presently running events.
	 */
	public static void stop(){
		//if something is running, we'll need to stop it.
		if(activeEvent != null){
			
			//Force stop this event and its children
			activeEvent.forceStopAllChildren();
			activeEvent.userForceStop();
		}
		
		//Set activeEvent to nothing running state.
		activeEvent = null;
	}
	
	public static void update(){
		
		//Don't bother to do anything if there is no active event right now.
		if(activeEvent != null){
			//See what our current event is.
			if(globalEventIndex >= events.size()){
				//terminal condition. we have no more states to run. Stop running things.
				activeEvent = null;
				return;
			} else {
				//update the active event.
				activeEvent = events.get(globalEventIndex);
			}
			
			//Update the active event. This will probably set motors or stuff like that.
			activeEvent.update();
			
			//Check if there are any children to update.
			if(activeEvent.childEvents.size() > 0){
				for(AutoEvent child : activeEvent.childEvents){
					
					//Evaluate if child needs to start running
					if(child.isRunning == false & child.isTriggered()){
						child.isRunning = true;
					}
					//Call update if the child is running
					if(child.isRunning == true)
					{
						child.update();
					}
					//Evaluate if the child needs to be stopped
					if(child.isRunning == true & child.isDone()){
						child.isRunning = false;
					}
					
				}
			}
			
			//Check if active event has completed. Move on to the next one if this one is done.
			//Note this sequence guarantees each event's update is called at least once.
			if(activeEvent.isDone()){
				activeEvent.forceStopAllChildren(); //Just in case the user is sloppy and leaves child events running when the parent finishes.
				globalEventIndex++;
			}
			
		}
		globalUpdateCount++;
		
	}
	
	/** 
	 * Determine if the sequencer is active. As long as this is true, any calls to update() will trigger various events run.
	 * @return True if the auto sequencer is executing something, false otherwise
	 */
	public static boolean isRunning(){
		return activeEvent != null;
	}
	
	
}
