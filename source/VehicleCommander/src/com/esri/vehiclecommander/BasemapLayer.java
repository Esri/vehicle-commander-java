package com.esri.vehiclecommander;

import com.esri.map.Layer;
import java.awt.MediaTracker;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * A bean to contain a Layer object and other information, such as a thumbnail image,
 * that would go with a basemap layer.
 */
public class BasemapLayer {
    
    private final Layer layer;
    private final Icon thumbnail;

    public BasemapLayer(Layer layer, Icon thumbnail) {
        this.layer = layer;
        this.thumbnail = thumbnail;
    }
    
    public BasemapLayer(Layer layer, String thumbnailFilename) {
        this.layer = layer;
        if (null == thumbnailFilename) {
            thumbnail = null;
        } else {
            ImageIcon imageIcon = new ImageIcon(thumbnailFilename);
            thumbnail = MediaTracker.COMPLETE == imageIcon.getImageLoadStatus() ? imageIcon : null;
        }
    }

    /**
     * @return the layer
     */
    public Layer getLayer() {
        return layer;
    }

    /**
     * @return the thumbnail
     */
    public Icon getThumbnail() {
        return thumbnail;
    }
    
}
