/*******************************************************************************
 * Copyright 2012 Esri
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.esri.vehiclecommander;

import com.esri.core.geometry.Point;
import java.util.Calendar;

/**
 * A bean that holds a Point and a timestamp (Calendar). GPSTrackPoint objects
 * are Comparable and are ordered by timestamp ascending when compared or sorted.
 */
public class GPSTrackPoint implements Comparable<GPSTrackPoint> {

    private Point point;
    private Calendar timestamp;
    private double speed;

    /**
     * Creates a new GPSTrackPoint with no data.
     */
    public GPSTrackPoint() {
    }

    /**
     * Creates a new GPSTrackPoint with data.
     * @param point the point.
     * @param timestamp the timestamp.
     * @param speed the speed.
     */
    public GPSTrackPoint(Point point, Calendar timestamp, double speed) {
        this.point = point;
        this.timestamp = timestamp;
        this.speed = speed;
    }

    /**
     * @return the point
     */
    public Point getPoint() {
        return point;
    }

    /**
     * @param point the point to set
     */
    public void setPoint(Point point) {
        this.point = point;
    }

    /**
     * @return the timestamp
     */
    public Calendar getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(Calendar timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Compares the timestamp of this GPSTrackPoint to another, using Calendar.compareTo.
     * @see Comparable
     * @see Calendar
     * @param trackPoint the other GPSTrackPoint.
     * @return a negative number if this GPSTrackPoint's timestamp is earlier than
     *         the other point's timestamp; 0 if the timestamps are the same; and
     *         a positive number otherwise.
     */
    public int compareTo(GPSTrackPoint trackPoint) {
        return this.timestamp.compareTo(trackPoint.getTimestamp());
    }

    /**
     * @return the speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @param speed the speed to set
     */
    public void setSpeed(double speed) {
        this.speed = speed;
    }

}
