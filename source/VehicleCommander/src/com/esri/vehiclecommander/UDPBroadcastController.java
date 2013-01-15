package com.esri.vehiclecommander;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller that sends UDP broadcasts.
 */
public class UDPBroadcastController {

    private static final HashMap<Integer, UDPBroadcastController> portToController
            = new HashMap<Integer, UDPBroadcastController>();

    private final DatagramSocket udpSocket;
    private final DatagramPacket packet;

    /**
     * Returns the UDPBroadcastController singleton instance for the specified
     * messaging port.
     * @param messagingPort the UDP port through which the desired UDPBroadcastController
     *                      sends UDP messages to clients.
     * @return the UDPBroadcastController singleton instance for the specified
     * messaging port.
     */
    public static UDPBroadcastController getInstance(int messagingPort) {
        UDPBroadcastController instance = portToController.get(messagingPort);
        if (null == instance) {
            instance = new UDPBroadcastController(messagingPort);
            portToController.put(messagingPort, instance);
        }
        return instance;
    }

    private UDPBroadcastController(int messagingPort) {
        DatagramSocket theSocket = null;
        DatagramPacket thePacket = null;
        try {
            theSocket = new DatagramSocket();
            thePacket = new DatagramPacket(new byte[0], 0, InetAddress.getByName("255.255.255.255"), messagingPort);
        } catch (IOException ex) {
            Logger.getLogger(UDPBroadcastController.class.getName()).log(Level.SEVERE, null, ex);
        }
        udpSocket = theSocket;
        packet = thePacket;
    }

    /**
     * Sends a UDP broadcast.
     * @param bytes the message.
     * @throws IOException if the message cannot be sent.
     */
    public void sendUDPMessage(byte[] bytes) throws IOException {
        synchronized (packet) {
            packet.setData(bytes);
            packet.setLength(bytes.length);
            udpSocket.send(packet);
        }
    }
    
}
