package com.esri.vehiclecommander;

import java.awt.event.MouseEvent;

/**
 * An abstract implementation of MapOverlayListener for convenience. Override the
 * methods you need. Any methods not overridden do nothing.
 * @see MapController#trackAsync(com.esri.vehiclecommander.MapOverlayListener, short)
 */
public abstract class MapOverlayAdapter implements MapOverlayListener {

    public void mouseClicked(MouseEvent event) {
        
    }

    public void mousePressed(MouseEvent event) {
        
    }

    public void mouseReleased(MouseEvent event) {
        
    }
    
    public void mouseDragged(MouseEvent event) {
        
    }

}
