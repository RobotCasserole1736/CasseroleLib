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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


/**
 * DESCRIPTION: <br>
 * Simple class to receive UDP data from a coprocessor (such as a Beaglebone Black) <br><br>
 * The class is designed to be robust against spurious signals, connect/disconnect due to faulty wiring, etc. <br><br>
 * <br>
 * USAGE:
 * <ol>
 * <li>Instantiate the receiver with the listen address and port</li>
 * <li>Call the getPacket() method each time a new packet is desired. </li>
 * </ol>
 * 
 * 
 */


public class UDPReceiver{
    DatagramSocket recieveSocket = null;
    byte[] receiveData = new byte[2048]; 
    DatagramPacket recievePacket = null;

    /**
     * Constructor for the UDP reciever. Sets up internal memory structures in prep to start listening for packets.
     * 
     * @param listen_to_addr String of the IP address of the coprocessor (For example, "10.17.36.8")
     * @param listen_on_port integer port number to listen on. Often between 5800 and 5810 per FMS whitepaper. Must match whatever port the coprocessor is sending information to.
     */
    public UDPReceiver(String listen_from_addr_in, int listen_on_port_in) {
        try {
            recieveSocket = new DatagramSocket(listen_on_port_in);
            recievePacket = new DatagramPacket(receiveData, receiveData.length);
            recieveSocket.setSoTimeout(10);
        } catch (IOException e) {
            System.out.println("Error: Cannot set up UDP reciever socket: " + e.getMessage());
            recieveSocket = null;
        }

    }
    
    /**
     * Listens on the UPD connection for a packet. Casts it into a java string and returns it.
     * Note this method will block while <i> aggressively polling </i> until a full packet is received,
     * and drop all packets but the most recent. 
     * @return String of the data acquired from the UDP connection (if any data gotten)
     */
    public String getPacket(){
        boolean last_packet = false;
        String rx_string = "";
        if(recieveSocket != null){
            while(last_packet == false){
                try {
                    recieveSocket.receive(recievePacket);
                    rx_string = new String(recievePacket.getData(), 0, recievePacket.getLength());
                } catch (java.net.SocketTimeoutException e) {
                    /* timeout exception - this is OK, just means we don't see new complete packet. */
                    if(rx_string.length() != 0){
                        // We have a packet and there are no more in the recieve queue. Break and return the last packet one.
                        last_packet = true; 
                        //System.out.println(rx_string);
                    }
                } catch (IOException e) {
                    /* some other error we didn't think about... don't try to listen anymore */
                    System.out.println("Error: Cannot get data from UDP socket: " + e.getMessage());
                    recieveSocket = null;
                } 
            }
        }
        
        return rx_string;
            
    }
    
    /**
     * Here, the main function is used as a test which can be run on a user's PC. It should NOT be called by any robot code.
     */
    public static void main(String[] args) {
        System.out.println("Starting test for udp receiver...");
        UDPReceiver testReceiver = new UDPReceiver("127.0.0.1", 5800);
        
        while(true){
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Got Data: " + testReceiver.getPacket());
        }
    }

}
