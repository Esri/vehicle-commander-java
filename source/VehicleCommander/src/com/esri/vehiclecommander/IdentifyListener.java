package com.esri.vehiclecommander;

import com.esri.core.geometry.Point;
import com.esri.map.Layer;
import java.util.Map;

/**
 * Listens for identify operations to complete. ArcGIS Runtime has something
 * similar for a single IdentifyTask. IdentifyListener is designed to work for a
 * set of concurrent identify tasks.
 */
public interface IdentifyListener {

    /**
     * Called when a complete identify operation finishes, which may include the
     * completion of more than one IdentifyTask.
     * @param identifyPoint the point that was used for the identify operation,
     *                      for display purposes. Can be null.
     * @param results the combined results from all identify tasks.
     * @param resultToLayer a map of results to the layer from which each result comes.
     */
    public void identifyComplete(Point identifyPoint, IdentifiedItem[] results, Map<IdentifiedItem, Layer> resultToLayer);

}
