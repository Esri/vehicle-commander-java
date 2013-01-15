package com.esri.vehiclecommander;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A bean containing map configuration details, typically read from XML by MapConfigReader.
 */
public class MapConfig {

    private List<BasemapLayer> layers = new ArrayList<BasemapLayer>();
    private final List<Map<String, String>> toolbarItems;
    
    /**
     * Instantiates a MapConfig with an empty list of toolbar items.
     */
    public MapConfig() {
        this.toolbarItems = new ArrayList<Map<String, String>>();
    }

    /**
     * Instantiates a MapConfig with a list of toolbar items.
     * @param toolbarItems the toolbar items. Each item in the list is a map of
     *                     key-value pairs used to instantiate a toolbar item.
     */
    public MapConfig(List<Map<String, String>> toolbarItems) {
        this.toolbarItems = toolbarItems;
    }

    /**
     * Returns the basemap layers contained by this MapConfig.
     * @return The basemap layers contained by this MapConfig.
     */
    public List<BasemapLayer> getBasemapLayers() {
        return layers;
    }

    /**
     * Sets this MapConfig's basemap layers.
     * @param layers the basemap layers to be stored by this MapConfig
     */
    public void setBasemapLayers(List<BasemapLayer> layers) {
        this.layers = layers;
    }
    
    /**
     * Returns the toolbar items as a list of key-value pairs.
     * @return the toolbar items.
     */
    public List<Map<String, String>> getToolbarItems() {
        return toolbarItems;
    }

}
