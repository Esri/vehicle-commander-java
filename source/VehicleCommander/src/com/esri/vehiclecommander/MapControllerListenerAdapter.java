package com.esri.vehiclecommander;

/**
 * A convenience class that implements all methods in MapControllerListener with
 * empty method bodies. Extend this class and implement the methods you need.
 */
public abstract class MapControllerListenerAdapter implements MapControllerListener {

    /**
     * Called when map layers are added or removed via the MapController.
     * @param isOverlay true if and only if an overlay layer was added or removed.
     * @see MapControllerListener
     */
    public void layersChanged(boolean isOverlay) {}

    /**
     * Called when the map is ready. This event fires when the JMap mapReady
     * event fires.
     * @see MapControllerListener
     */
    public void mapReady() {}

}
