package com.esri.vehiclecommander;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A GraphicsLayerController that receives UDP messages and displays them on the map.
 */
public class UDPMessageGraphicsLayerController extends GraphicsLayerController {

    private final DatagramSocket udpSocket;
    private final AdvancedSymbolController symbolController;

    /**
     * Creates a new UDPMessageGraphicsLayerController.
     * @param mapController the application's map controller.
     * @param symbolController the application's advanced symbol controller.
     * @param messagingPort the application's messaging port.
     */
    public UDPMessageGraphicsLayerController(
            MapController mapController,
            AdvancedSymbolController symbolController,
            int messagingPort) {
        super(mapController, "UDPMessageGraphicsLayerController");
        this.symbolController = symbolController;
        DatagramSocket theSocket = null;
        try {
            theSocket = new DatagramSocket(messagingPort);
        } catch (IOException ex) {
            Logger.getLogger(UDPMessageGraphicsLayerController.class.getName()).log(Level.SEVERE, null, ex);
        }
        udpSocket = theSocket;
        startReceiving();
    }

    private void startReceiving() {
        new Thread() {

            @Override
            public void run() {
                while (true) {
                    final DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
                    try {
                        udpSocket.receive(packet);
                        new Thread() {

                            @Override
                            public void run() {
                                //Parse message(s) and display
                                symbolController.addMessagesToMap(new String(packet.getData(), packet.getOffset(), packet.getLength()));
                                if (8192 == packet.getLength()) {
                                    System.out.println("*** WARNING *** packet of maximum length received! Change max length in class " + UDPMessageGraphicsLayerController.class.getSimpleName());
                                }
                            }
                        }.start();
                    } catch (IOException ex) {
                        Logger.getLogger(UDPMessageGraphicsLayerController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }.start();

    }

}
