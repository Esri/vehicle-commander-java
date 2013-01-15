package com.esri.vehiclecommander;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.Timer;

/**
 * A KeyListener for application-wide key events. This could be added to the application
 * frame or the JMap, for example.
 */
public class ApplicationKeyListener extends KeyAdapter {

    private final Frame frame;
    private final MapController mapController;
    private final GPSController gpsController;
    private Timer rotateTimer = null;

    /**
     * Constructor that takes the application frame as a parameter.
     * @param frame The application frame
     */
    public ApplicationKeyListener(Frame frame, MapController mapController,
            GPSController gpsController) {
        this.frame = frame;
        this.mapController = mapController;
        this.gpsController = gpsController;
    }

    /**
     * Performs certain actions for certain keys:
     * <ul>
     *     <li>Escape: close application</li>
     *     <li>V and B: cancel rotation</li>
     *     <li>N: clear rotation</li>
     * </ul>
     * @param e The key event
     */
    @Override
    public void keyReleased(KeyEvent e) {
        if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
            //Close application on escape
            Utilities.closeApplication(frame);
        } else if (KeyEvent.VK_V == e.getKeyCode() || KeyEvent.VK_B == e.getKeyCode()) {
            //Cancel rotation
            rotateTimer.stop();
        } else if (KeyEvent.VK_N == e.getKeyCode()) {
            mapController.setRotation(0);
            gpsController.setNavigationMode(GPSNavigationMode.NORTH_UP);
        }
    }

    /**
     * Performs certain actions for certain keys:
     * <ul>
     *     <li>V and B: rotate the map</li>
     * </ul>
     * @param e The key event
     */
    @Override
    public void keyPressed(final KeyEvent e) {
        if (KeyEvent.VK_V == e.getKeyCode() || KeyEvent.VK_B == e.getKeyCode()) {
            gpsController.setNavigationMode(GPSNavigationMode.NORTH_UP);
            if (null == rotateTimer || !rotateTimer.isRunning()) {
                //Start rotation
                rotateTimer = new Timer(1000 / 24, new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        double rotation = (KeyEvent.VK_V == e.getKeyCode() ? -360 : 360) / 12;
                        mapController.rotate(rotation);
                    }
                });
                rotateTimer.start();
            }
        }
    }

}
