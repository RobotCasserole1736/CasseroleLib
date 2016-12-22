package org.usfirst.frc.team1736.lib.CoProcessor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

///////////////////////////////////////////////////////////////////////////////
// Copyright (c) FRC Team 1736 2016. See the License file.
//
// Can you use this code? Sure! We're releasing this under GNUV3, which
// basically says you can take, modify, share, publish this as much as you
// want, as long as you don't make it closed source.
//
// If you do find it useful, we'd love to hear about it! Check us out at
// http://robotcasserole.org/ and leave us a message!
///////////////////////////////////////////////////////////////////////////////

/**
 * DESCRIPTION: <br>
 * Simple class to receive UDP data from a coprocessor (such as a Beaglebone Black) <br>
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
     * Here, the main function is used as a test which can be run on a user's PC
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
