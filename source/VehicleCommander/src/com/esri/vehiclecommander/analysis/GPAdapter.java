package com.esri.vehiclecommander.analysis;

import com.esri.map.ArcGISDynamicMapServiceLayer;

/**
 * An implementation of GPListener that by default does nothing. Override
 * the methods for the events you want to receive.
 */
public abstract class GPAdapter implements GPListener {

    public void gpDisbled() {
        
    }

    public void gpEnabled() {
        
    }
    
    public void gpStarted() {
        
    }

    public void gpEnded(ArcGISDynamicMapServiceLayer resultLayer) {
        
    }
    
}
