package com.esri.vehiclecommander;

/**
 * An enum that distinguishes between various GPS navigation modes.
 */
public enum GPSNavigationMode {
    /**
     * Rotate the map so that north is up.
     */
    NORTH_UP,
    
    /**
     * Rotate the map so that the current GPS heading is up.
     */
    TRACK_UP,
    
    /**
     * Rotate the map so that the direction from the current GPS location to the
     * selected waypoint is up.
     */
    WAYPOINT_UP
}
