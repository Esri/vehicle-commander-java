package com.esri.vehiclecommander.analysis;

import com.esri.map.ArcGISDynamicMapServiceLayer;

/**
 * A listener for geoprocessing tools.
 */
public interface GPListener {
    
    /**
     * Called when GP functionality becomes available.
     */
    public void gpEnabled();
    
    /**
     * Called when GP functionality becomes unavailable.
     */
    public void gpDisbled();
    
    /**
     * Called when GP calculation starts.
     */
    public void gpStarted();
    
    /**
     * Called when GP calculation ends, whether successful or not.
     * @param resultLayer the GP result layer, or null if the GP failed.
     */
    public void gpEnded(ArcGISDynamicMapServiceLayer resultLayer);
    
}
