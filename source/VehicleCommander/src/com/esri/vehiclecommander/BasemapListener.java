package com.esri.vehiclecommander;

import com.esri.map.Layer;

/**
 * Fired for certain basemap events, such as becoming visible.
 */
public interface BasemapListener {
    
    public void basemapBecameVisible(Layer layer);
    
}
