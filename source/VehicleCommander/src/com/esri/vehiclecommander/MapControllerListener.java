package com.esri.vehiclecommander;

/**
 * A listener for MapController events. Add a MapControllerListener to a MapController
 * by calling MapController.addListener(MapControllerListener).
 */
public interface MapControllerListener {

    /**
     * Called when map layers are added or removed via the MapController.
     * @param isOverlay true if and only if an overlay layer was added or removed.
     */
    public void layersChanged(boolean isOverlay);

    /**
     * Called when the map is ready. This event fires when the JMap mapReady
     * event fires.
     */
    public void mapReady();

}
