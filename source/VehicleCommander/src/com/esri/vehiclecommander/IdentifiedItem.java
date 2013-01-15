package com.esri.vehiclecommander;

import com.esri.core.geometry.Geometry;
import java.util.Map;

/**
 * An identified item that can be added to the IdentifyResultsJPanel.
 */
public class IdentifiedItem {
    
    private final Geometry geometry;
    private final int layerId;
    private final Map<String, Object> attributes;
    private final Object value;
    
    public IdentifiedItem(Geometry geometry, int layerId, Map<String, Object> attributes, Object value) {
        this.geometry = geometry;
        this.layerId = layerId;
        this.attributes = attributes;
        this.value = value;
    }

    /**
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * @return the layerId
     */
    public int getLayerId() {
        return layerId;
    }

    /**
     * @return the attributes
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }
    
}
