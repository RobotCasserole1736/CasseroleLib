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
import java.util.TimerTask;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

/**
 * DESCRIPTION: <br>
 * Private socket definition class that Jetty wants me to make public even though it doesn't
 * actually have to be. Don't use this for anything unless you know preciisely what you are doing.
 */

public class CasseroleStateStreamerSocket extends WebSocketAdapter {
    private java.util.Timer updater = new java.util.Timer("State Webpage Update");
    private int updatePeriodMS = 250; // default update rate of 0.25s
    volatile int test_data;


    /**
     * Set the time between server broadcasts of current state. Default is 1 second. Faster update
     * rates bog down both server and network.
     * 
     * @param in_period_ms Broadcast period in milliseconds.
     */
    public void setUpdatePeriod(int in_period_ms) {
        updatePeriodMS = in_period_ms;
    }


    @Override
    public void onWebSocketText(String message) {
        if (isConnected()) {
            System.out.printf("Got client's message: [%s]%n", message);
        }
    }


    @Override
    public void onWebSocketConnect(Session sess) {

        super.onWebSocketConnect(sess);
        // On client connect, begin new task to braodcast data at 1 second intervals
        test_data = 0;
        updater.scheduleAtFixedRate(new dataBroadcastTask(), 0, updatePeriodMS);
    }


    @Override
    public void onWebSocketClose(int statusCode, String reason) {

        super.onWebSocketClose(statusCode, reason);
        // On client disconnect, close down broadcast task
        updater.cancel();
    }


    /**
     * send socket data out to client
     */
    @SuppressWarnings("unchecked")
	public void broadcastData() {
        if (isConnected()) {
            try {
                JSONObject full_obj = new JSONObject();
                JSONArray data_array = new JSONArray();

                // Package all data array elements into a JSON array
                for (String name : CassesroleWebStates.ordered_state_name_list) {
                    data_array.add(CassesroleWebStates.data_array_elements.get(name));
                }

                // package array into object
                full_obj.put("state_array", data_array);
                getRemote().sendString(full_obj.toJSONString());
                test_data += 1;

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
