package com.esri.vehiclecommander;

import com.esri.map.Layer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author gary4620
 */
public class BasemapLayerList extends ArrayList<BasemapLayer> {
    
    public List<Layer> getLayers() {
        synchronized (this) {
            ArrayList<Layer> list = new ArrayList<Layer>(size());
            for (BasemapLayer layer : this) {
                list.add(layer.getLayer());
            }
            return list;
        }
    }
    
}
