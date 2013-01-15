package com.esri.vehiclecommander;

import java.awt.event.MouseEvent;

/**
 * An interface for classes that listen for MapOverlay events.
 * @see MapController
 * @see com.esri.map.MapOverlay
 */
public interface MapOverlayListener {

    /**
     * Called when the mouse is clicked on the map.
     * @param event the mouse click event.
     */
    public void mouseClicked(MouseEvent event);
    
    /**
     * Called when the mouse is pressed down on the map.
     * @param event the mouse press event.
     */
    public void mousePressed(MouseEvent event);
    
    /**
     * Called when the mouse is released on the map.
     * @param event the mouse release event.
     */
    public void mouseReleased(MouseEvent event);
    
    /**
     * Called when the mouse is dragged on the map.
     * @param event the mouse drag event.
     */
    public void mouseDragged(MouseEvent event);

}
