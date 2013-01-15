package com.esri.gps;

import com.esri.core.geometry.Point;
import com.esri.core.gps.FixStatus;
import com.esri.core.gps.GPSEventListener;
import com.esri.core.gps.GPSStatus;
import com.esri.core.gps.GeoPosition;
import com.esri.core.gps.GpsGeoCoordinate;
import com.esri.core.gps.IGPSWatcher;
import com.esri.core.gps.PositionChangeType;
import com.esri.vehiclecommander.Utilities;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * A component that provides GPS locations. Extend this class and use your
 * class to fill in the details, whether you're using a real GPS device or building
 * a simulator. Add one or more GPSEventListener objects to listen to the GPSProvider's
 * GPS fixes.<br/>
 * <br/>
 * In your child class, you must call sendGPSLocation whenever you want to send
 * a GPS fix to all the listeners.
 */
public abstract class GPSProvider implements IGPSWatcher {

    private final Set<GPSEventListener> listeners;

    /**
     * Creates a new GPSProvider with an empty list of listeners.
     */
    public GPSProvider() {
        listeners = Collections.synchronizedSet(new HashSet<GPSEventListener>());
    }

    /**
     * Sends a GPS location to all of this GPSProvider's listeners.
     * @param latLonPoint the GPS location to send.
     * @param previousLatLonPoint the previous GPS location, to calculate heading.
     * @param speed the speed to send.
     */
    protected final void sendGPSLocation(
            final Point latLonPoint,
            final Point previousLatLonPoint,
            final double speed) {
        double rise = latLonPoint.getY() - previousLatLonPoint.getY();
        double run = latLonPoint.getX() - previousLatLonPoint.getX();
        double trigHeadingRadians = Math.atan(rise / run);
        if (0 > run) {
            trigHeadingRadians += Math.PI;
        }
        double compassHeadingRadians = Utilities.toCompassHeadingRadians(trigHeadingRadians);
        final double course = Math.toDegrees(compassHeadingRadians);
        synchronized (listeners) {
            for (final GPSEventListener listener : listeners) {
                GpsGeoCoordinate coord = new GpsGeoCoordinate();
                coord.setPositionChangeType(PositionChangeType.POSITION.getCode());
                coord.setCourse(course);
                coord.setLatitude(latLonPoint.getY());
                coord.setLongitude(latLonPoint.getX());
                coord.setFixStatus(FixStatus.GPS_FIX);
                coord.setSpeed(speed);
                GeoPosition geoPosition = new GeoPosition(coord, new Date());
                listener.onPositionChanged(geoPosition);
            }
        }
    }

    /**
     * Sends a GPS status to all of this GPSProvider's listeners.
     * @param status the GPS status to send.
     */
    protected final void sendGPSStatus(final GPSStatus status) {
        synchronized (listeners) {
            for (final GPSEventListener listener : listeners) {
                new Thread() {
                    @Override
                    public void run() {
                        listener.onStatusChanged(status);
                    }
                }.start();
            }
        }
    }

    /**
     * Adds a GPSEventListener.
     * @param gpsEventListener the GPSEventListener.
     */
    public final void addListener(GPSEventListener gpsEventListener) {
        listeners.add(gpsEventListener);
    }

    /**
     * Removes a GPSEventListener if it has been added to this GPSProvider. If the specified
     * GPSEventListener has not been added to this GPSProvider, this method has no effect.
     * @param gpsEventListener the GPSEventListener to remove.
     */
    public final void removeListener(GPSEventListener gpsEventListener) {
        listeners.remove(gpsEventListener);
    }

}
