package com.esri.vehiclecommander;

import com.esri.map.Layer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * An ordered list of IdentifyResult objects with references to the layers from which they
 * came.
 */
public class IdentifyResultList {
    
    private final ArrayList<IdentifiedItem> results = new ArrayList<IdentifiedItem>();
    private final HashMap<IdentifiedItem, Layer> resultToLayer = new HashMap<IdentifiedItem, Layer>();
    
    public void add(IdentifiedItem result, Layer layer) {
        results.add(result);
        resultToLayer.put(result, layer);
    }
    
    public int size() {
        return results.size();
    }
    
    public IdentifiedItem get(int index) {
        return results.get(index);
    }
    
    public Layer getLayer(IdentifiedItem result) {
        return resultToLayer.get(result);
    }
    
    public void clear() {
        results.clear();
        resultToLayer.clear();
    }
    
}
