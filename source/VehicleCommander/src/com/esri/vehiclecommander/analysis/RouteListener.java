package com.esri.vehiclecommander.analysis;

import com.esri.core.map.Graphic;

/**
 * An interface for classes that listen for events from the route tools.
 */
public interface RouteListener {
    
    /**
     * Called when a waypoint is added.
     * @param graphic the added waypoint graphic, whose ID may or may not be populated.
     * @param graphicUid the added waypoint graphic's ID.
     */
    void waypointAdded(Graphic graphic, int graphicUid);
    
    /**
     * Called when a waypoint is removed.
     * @param graphicUid the removed waypoint graphic's ID.
     */
    void waypointRemoved(int graphicUid);
    
    /**
     * Called when a waypoint is selected.
     * @param graphic the selected waypoint graphic.
     */
    void waypointSelected(Graphic graphic);
    
}
