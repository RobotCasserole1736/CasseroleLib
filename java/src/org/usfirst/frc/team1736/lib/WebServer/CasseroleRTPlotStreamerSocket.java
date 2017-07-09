package org.usfirst.frc.team1736.lib.WebServer;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TimerTask;
import java.util.TreeSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 * DESCRIPTION: <br>
 * Private socket definition class that Jetty wants me to make public even though it doesn't
 * actually have to be. Don't use this for anything unless you know preciisely what you are doing.
 */

public class CasseroleRTPlotStreamerSocket extends WebSocketAdapter {
    private java.util.Timer updater = null;
    private int updatePeriodMS = 250; // default update rate of 4 Hz

    @Override
    public void onWebSocketText(String message) {
        /*
         * Process commands from the client page (non-json)
         * All messages should take the form <cmd>:<arglist> where <cmd> is a string
         * indicating what to do, : is required, and <arglist> is an optional comma-separated 
         * list of arguments for the command.
         */
        String[] msg_parts = message.split(":");
        String cmd = "";
        String args = "";
        
        if(msg_parts.length == 2){
        	cmd = msg_parts[0];
        	args = msg_parts[1];
        } else if(msg_parts.length == 1){
        	cmd = msg_parts[0];
        } else  {
        	System.out.println("ERROR: RT Plot client sent bad command/arg set -  " + message);
        	return;
        }
        
        /*
         * Handle the command & args parsed from the incoming message
         */
        if(cmd.compareTo("start")==0){
        	handleSignalAcqList(args); 
        } else if (cmd.compareTo("stop")==0){
        	handleStopAcq();
        } else if(cmd.compareTo("get_list")==0){
        	handleSignalListReq();
        } else {
        	System.out.println("ERROR: Got unknown command " + cmd);
        }
    }


    @Override
    public void onWebSocketConnect(Session sess) {

        super.onWebSocketConnect(sess);
        handleSignalListReq();
    }


    @Override
    public void onWebSocketClose(int statusCode, String reason) {

        super.onWebSocketClose(statusCode, reason);
        handleStopAcq();
    }
    
    /**
     * Handle a user request to send information about the available signals
     */
    public void handleSignalListReq(){
    	/* Create the JSON transmission object */
    	JSONObject tx_obj = createSignalListTxArrayObj();
    	
    	/* Send the signal list data to the client */
    	try {
			getRemote().sendString(tx_obj.toJSONString());
		} catch (IOException e) {
            e.printStackTrace(System.err);
		}
    	
    }
    
    /**
     * Given a comma-separated list from a client for a set of signals to acquire, start the data capture
     * @param list
     */
    private void handleSignalAcqList(String list){
    	List<String> signal_names = new ArrayList<String>(Arrays.asList(list.split(",")));
    	signal_names.removeAll(Arrays.asList("", null));
    	if(signal_names.size() > 0){
    		CasseroleWebPlots.startAcq(signal_names);
            // On client signal broadcast request, begin new task to braodcast data at a given interval
        	updater = new java.util.Timer("Realtime Plot Webpage Update");
            updater.scheduleAtFixedRate(new dataBroadcastTask(), 0, updatePeriodMS);
    	}
 
    }
    
    /**
     * Handle the user's request to stop sending data.
     */
    private void handleStopAcq(){
    	if(CasseroleWebPlots.acqActive){
	    	CasseroleWebPlots.stopAcq();
	        // On client transmit stop request, close down broadcast task
	        updater.cancel();
    	}
    }
    
    
    private JSONObject createPlotDataTxArrayObj(){
        JSONObject tx_obj = new JSONObject();
        JSONArray signal_array = new JSONArray();
        
        // Build up JSON structure of samples recorded.
        for (PlotSignal sig : CasseroleWebPlots.activeSignalList) {
        	
        	JSONArray sample_arr = new JSONArray();
        	
        	PlotSample[] samples = sig.getAllSamples();
        	if(samples != null){
	        	for(PlotSample samp : samples){
	        		if(samp != null){
		        		JSONObject sample_obj = new JSONObject();
		        		sample_obj.put("time", samp.getTime_sec());
		        		sample_obj.put("val", samp.getVal());
		        		sample_arr.add(sample_obj);
	        		}
	        	}
        	}
        	
        	JSONObject signal_obj = new JSONObject();
        	signal_obj.put("name", sig.getName());
        	signal_obj.put("samples", sample_arr);
        	
        	signal_array.add(signal_obj);
        }
        
        // package array into object
        tx_obj.put("type", "daq_update");
        tx_obj.put("samples", signal_array);
        
    	return tx_obj;
    }
    
    /**
     * @return A JSON object describing all available signals which can be plotted.
     */
    private JSONObject createSignalListTxArrayObj(){
    	JSONObject tx_obj = new JSONObject();
    	JSONArray signal_array = new JSONArray();
    	
        // Package all data array elements into a JSON array
    	SortedSet<PlotSignal> sortedObjs = new TreeSet<PlotSignal>(CasseroleWebPlots.RTPlotSignals.values());
        for (PlotSignal sig : sortedObjs) {
        	JSONObject signal_info = new JSONObject();
        	signal_info.put("name", sig.getName());
        	signal_info.put("display_name", sig.getDisplayName());
        	signal_info.put("units", sig.getUnits());
        	signal_array.add(signal_info);
        }
        
        tx_obj.put("type", "signal_list");
        tx_obj.put("signals", signal_array);
        
    	return tx_obj;
    }


    /**
     * send socket data out to client
     */
    @SuppressWarnings("unchecked")
	public void broadcastData() {
        if (isConnected() & CasseroleWebPlots.acqActive) {
        	
        	JSONObject tx_obj = createPlotDataTxArrayObj();
        	
            try {
                getRemote().sendString(tx_obj.toJSONString());
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Timer task to periodically broadcast data to the client. Java multithreading magic here, do
     * not touch! If you touch this, you will face the wrath of Chitulu, god of data streaming
     * servers. May the oceans of 1's and 0's rise to praise him.
     * 
     * @author Chris Gerth
     *
     */
    private class dataBroadcastTask extends TimerTask {
        public void run() {
            broadcastData();
        }
    }

}
