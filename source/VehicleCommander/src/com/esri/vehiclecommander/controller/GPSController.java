/*******************************************************************************
 * Copyright 2012-2014 Esri
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
package com.esri.vehiclecommander.controller;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.gps.GPSEventListener;
import com.esri.core.gps.GPSException;
import com.esri.core.gps.GPSStatus;
import com.esri.core.gps.GeoPosition;
import com.esri.core.gps.GpsGeoCoordinate;
import com.esri.core.gps.IGPSWatcher;
import com.esri.core.gps.Satellite;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.advanced.MessageHelper;
import com.esri.map.GPSLayer;
import com.esri.vehiclecommander.model.GPSNavigationMode;
import com.esri.vehiclecommander.util.Utilities;
import com.esri.vehiclecommander.view.VehicleCommanderJFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A GPSEventListener for the Vehicle Commander application.
 */
public class GPSController implements GPSEventListener {

    private IGPSWatcher gpsWatcher;
    private final MapController mapController;
    private final UDPBroadcastController udpBroadcastController;
    private final VehicleCommanderJFrame app;
    private final AppConfigController appConfig;
    private final Timer positionReportTimer;
    private GPSLayer gpsLayer;
    private final Set<GPSEventListener> listeners = Collections.synchronizedSet(new HashSet<GPSEventListener>());

    private final Object positionUpdateLock = new Object();
    private Point previousMapPoint = null;
    private int previousMapPointWkid = 0;
    private double longitude = 0;
    private double latitude = 0;
    private Double heading = null;
    private boolean sendPositionReports = true;
    private boolean followGps = false;
    private final Object followGpsLock = new Object();
    private GPSNavigationMode navigationMode = GPSNavigationMode.NORTH_UP;
    private final Object navigationModeLock = new Object();
    private boolean isHighlight = false;
    private double speed = 0;
    private Graphic selectedWaypoint = null;
    private final Object selectedWaypointLock = new Object();

    /**
     * Creates a new GPSController.
     * @param gpsWatcher the IGPSWatcher to be controlled by this GPSController.
     */
    public GPSController(IGPSWatcher gpsWatcher, MapController mapController, AppConfigController appConfig, VehicleCommanderJFrame app) throws IOException {
        this.appConfig = appConfig;
        this.gpsWatcher = gpsWatcher;
        this.udpBroadcastController = UDPBroadcastController.getInstance(appConfig.getPort());
        this.mapController = mapController;
        this.app = app;

        checkAndAddGPSLayer();

        positionReportTimer = new Timer(appConfig.getPositionMessageInterval(), new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    sendPositionReport();
                } catch (XMLStreamException ex) {
                    Logger.getLogger(GPSController.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(GPSController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        positionReportTimer.start();
    }

    /**
     * Stops the simulator, and then calls super.finalize().
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        positionReportTimer.stop();
        stop();
        super.finalize();
    }

    /**
     * Returns the most recent GPS location as an MGRS string. If a GPS location
     * has not been received, the method returns null.
     * @return the most recent GPS location as an MGRS string, or null if no GPS
     *         location has been received.
     */
    public String getPositionMGRS() {
        synchronized (positionUpdateLock) {
            if (null != previousMapPoint) {
                return mapController.toMilitaryGrid(new Point[] {previousMapPoint})[0];
            } else {
                return null;
            }
        }
    }

    /**
     * Sends a highlight/911 position report out to clients.
     * @throws XMLStreamException
     * @throws IOException
     */
    public void sendHighlightReport() throws XMLStreamException, IOException {
        isHighlight = true;
        sendPositionReport("UPDATE");
    }

    /**
     * Sends a position report out to clients indicating that the sender (i.e. this
     * vehicle) should be un-highlighted.
     * @throws XMLStreamException
     * @throws IOException
     */
    public void sendUnHighlightReport() throws XMLStreamException, IOException {
        isHighlight = false;
        sendPositionReport("UPDATE");
    }

    private void sendPositionReport() throws XMLStreamException, IOException {
        sendPositionReport("UPDATE");
    }

    private void sendPositionReport(String action) throws XMLStreamException, IOException {
        if (sendPositionReports) {
            synchronized (positionUpdateLock) {
                if (null != previousMapPoint) {
                    StringWriter xmlStringWriter = new StringWriter();
                    XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(xmlStringWriter);
                    xmlStreamWriter.writeStartDocument();
                    xmlStreamWriter.writeStartElement("geomessages");
                    xmlStreamWriter.writeStartElement("geomessage");
                    xmlStreamWriter.writeAttribute("v", Utilities.GEOMESSAGE_VERSION);

                    Utilities.writeTextElement(xmlStreamWriter,
                            MessageHelper.MESSAGE_2525C_TYPE_PROPERTY_NAME,
                            "trackrep");
                    Utilities.writeTextElement(xmlStreamWriter,
                            MessageHelper.MESSAGE_ACTION_PROPERTY_NAME, action);
                    Utilities.writeTextElement(xmlStreamWriter,
                            MessageHelper.MESSAGE_ID_PROPERTY_NAME, appConfig.getUniqueId());
                    Utilities.writeTextElement(xmlStreamWriter,
                            MessageHelper.MESSAGE_2525C_CONTROL_POINTS_PROPERTY_NAME,
                            previousMapPoint.getX() + "," + previousMapPoint.getY());
                    Utilities.writeTextElement(xmlStreamWriter,
                            MessageHelper.MESSAGE_WKID_PROPERTY_NAME, Integer.toString(previousMapPointWkid));
                    Utilities.writeTextElement(xmlStreamWriter, "sic", appConfig.getSic());
                    Utilities.writeTextElement(xmlStreamWriter, "uniquedesignation", appConfig.getUsername());
                    Utilities.writeTextElement(xmlStreamWriter, "type", appConfig.getVehicleType());
                    Date currentTime = new Date();
                    Utilities.writeTextElement(xmlStreamWriter, "datetimevalid", Utilities.DATE_FORMAT_GEOMESSAGE.format(currentTime));
                    Utilities.writeTextElement(xmlStreamWriter, "speed", 
                            "1.0".equals(appConfig.getGeomessageVersion()) ? Long.toString(Math.round(speed))
                            : Double.toString(speed));
                    if (null != heading) {
                        Utilities.writeTextElement(xmlStreamWriter, "direction", Long.toString(Math.round(heading)));
                    }
                    Utilities.writeTextElement(xmlStreamWriter, "status911", isHighlight ? "1" : "0");
                    
                    xmlStreamWriter.writeEndElement(); // geomessage
                    xmlStreamWriter.writeEndElement(); // geomessages
                    xmlStreamWriter.writeEndDocument();
                    xmlStreamWriter.flush();
                    String messageText = xmlStringWriter.toString();
                    udpBroadcastController.sendUDPMessage(messageText.getBytes());
                }
            }
        }
    }

    /**
     * Starts the GPS watcher.
     */
    public void start() throws GPSException {
        if (null != gpsWatcher) {
            gpsWatcher.addListener(this);
            gpsWatcher.start();
        }
    }

    /**
     * Stops the GPS watcher.
     */
    public void stop() throws GPSException {
        if (null != gpsWatcher) {
            gpsWatcher.stop();
        }
    }

    /**
     * Displays or hides the GPS layer.
     * @param show true if the GPS layer should be shown and false otherwise.
     */
    public void showGPSLayer(boolean show) {
        gpsLayer.setVisible(show);
    }

    /**
     * Checks to see if the GPS layer is in the map. If not, this method adds the
     * GPS layer to the map. The intent of this method is for resetting the map.
     */
    public final void checkAndAddGPSLayer() {
        if (!(null == gpsWatcher || mapController.hasLayer(gpsLayer))) {
            gpsLayer = new GPSLayer(gpsWatcher);
            gpsLayer.setShowTrail(false);
            gpsLayer.setShowTrackPoints(false);
            /**
             * TODO leverage new GPSLayer.setMode instead of doing GPS navigation
             * ourselves. For now, just set it to OFF.
             */
            gpsLayer.setMode(GPSLayer.Mode.OFF);
            mapController.addLayer(gpsLayer, false);
        }
    }

    /**
     * Returns true if the GPSController is sending position reports and false otherwise.
     * @return true if the GPSController is sending position reports and false otherwise.
     */
    public boolean isSendPositionReports() {
        return sendPositionReports;
    }

    /**
     * Tells the GPSController whether to send position reports to listening applications.
     * @param sendPositionReports true if the GPSController should send position
     *                            reports and false otherwise.
     */
    public void setSendPositionReports(boolean sendPositionReports) {
        this.sendPositionReports = sendPositionReports;
    }

    /**
     * Returns true if the map should follow the current GPS location.
     * @return true if the map should follow the current GPS location.
     */
    public boolean isFollowGps() {
        synchronized (followGpsLock) {
            return followGps;
        }
    }

    /**
     * Sets whether the map should follow the current GPS location.
     * @param followGps true if the map should follow the current GPS location.
     */
    public void setFollowGps(boolean followGps) {
        synchronized (followGpsLock) {
            this.followGps = followGps;
        }
    }

    /**
     * Returns the current navigation mode.
     * @return the current navigation mode.
     */
    public GPSNavigationMode getNavigationMode() {
        synchronized (navigationModeLock) {
            return navigationMode;
        }
    }

    /**
     * Sets the navigation mode.
     * @param navigationMode the new navigation mode.
     */
    public void setNavigationMode(GPSNavigationMode navigationMode) {
        synchronized (navigationModeLock) {
            this.navigationMode = navigationMode;
        }
    }
    
    /**
     * Sets the selected waypoint.
     * @param selectedWaypoint the selected waypoint graphic.
     */
    public void setSelectedWaypoint(Graphic selectedWaypoint) {
        synchronized (selectedWaypointLock) {
            this.selectedWaypoint = selectedWaypoint;
        }
    }

    /**
     * Called when the GPS status changes.
     * @see GPSEventListener
     * @param newStatus the new status.
     */
    public void onStatusChanged(final GPSStatus newStatus) {
        synchronized (listeners) {
            new Thread() {

                @Override
                public void run() {

                    for (final GPSEventListener listener : listeners) {
                        new Thread() {

                            @Override
                            public void run() {
                                listener.onStatusChanged(newStatus);
                            }

                        }.start();
                    }
                }
                
            }.start();
        }
    }

    /**
     * Called when the GPS position changes.
     * @see GPSEventListener
     * @param newPosition the new GPS position.
     */
    public void onPositionChanged(final GeoPosition newPosition) {
        synchronized (listeners) {
            for (final GPSEventListener listener : listeners) {
                new Thread() {

                    @Override
                    public void run() {
                        listener.onPositionChanged(newPosition);
                    }

                }.start();
            }
        }

        GpsGeoCoordinate location = newPosition.getLocation();
        Point latLonPoint = new Point(location.getLongitude(), location.getLatitude(), location.getAltitude());
        Point mapPoint = (Point) GeometryEngine.project(latLonPoint, Utilities.WGS84, mapController.getSpatialReference());

        Double theHeading = location.getCourse();

        if (isFollowGps()) {
            mapController.panTo(mapPoint);
            switch (navigationMode) {
                case TRACK_UP: {
                    if (null != theHeading) {
                        mapController.setRotation(theHeading);
                    }
                    break;
                }
                case WAYPOINT_UP: {
                    synchronized (selectedWaypointLock) {
                        if (null != selectedWaypoint) {
                            //Calculate heading from current location to selected waypoint
                            Point selectedWaypointLatLon = (Point) GeometryEngine.project(
                                    selectedWaypoint.getGeometry(),
                                    mapController.getSpatialReference(),
                                    Utilities.WGS84);
                            double headingToWaypoint = Utilities.calculateBearingDegrees(latLonPoint, selectedWaypointLatLon);
                            mapController.setRotation(headingToWaypoint);
                        }
                    }
                    break;
                }
            }
        }

        app.updatePosition(mapPoint, theHeading);

        synchronized (positionUpdateLock) {        	
            previousMapPoint = mapPoint;
            if ((mapController != null) && (mapController.getSpatialReference() != null))
            	previousMapPointWkid = mapController.getSpatialReference().getID();
            
            longitude = latLonPoint.getX();
            latitude = latLonPoint.getY();
            heading = theHeading;
            speed = location.getSpeed();
        }
    }

    /**
     * Called when an NMEA message is received from GPS.
     * @see GPSEventListener
     * @param newSentence the new NMEA sentence.
     */
    public void onNMEASentenceReceived(final String newSentence) {
        synchronized (listeners) {
            for (final GPSEventListener listener : listeners) {
                new Thread() {

                    @Override
                    public void run() {
                        listener.onNMEASentenceReceived(newSentence);
                    }

                }.start();
            }
        }
    }

    /**
     * Called when the GPS satellites in view change.
     * @see GPSEventListener
     * @param satellitesInView the satellites now in view.
     */
    public void onSatellitesInViewChanged(final Map<Integer, Satellite> satellitesInView) {
        synchronized (listeners) {
            for (final GPSEventListener listener : listeners) {
                new Thread() {

                    @Override
                    public void run() {
                        listener.onSatellitesInViewChanged(satellitesInView);
                    }

                }.start();
            }
        }
    }

    /**
     * Returns this GPSController's GPS watcher.
     * @return the gpsWatcher
     */
    public IGPSWatcher getGpsWatcher() {
        return gpsWatcher;
    }

    /**
     * Sets the GPSWatcher and recreates the GPSLayer in the map.
     * @param gpsWatcher the gpsWatcher to set
     */
    public void setGpsWatcher(IGPSWatcher gpsWatcher) {
        if (null != this.gpsWatcher) {
            try {
                this.gpsWatcher.stop();
            } catch (GPSException ex) {
                Logger.getLogger(GPSController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.gpsWatcher = gpsWatcher;
        if (null != gpsWatcher) {
            try {
                gpsWatcher.addListener(this);
            } catch (GPSException ex) {
                Logger.getLogger(GPSController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (null != gpsLayer) {
            mapController.removeLayer(gpsLayer);
        }
        checkAndAddGPSLayer();
    }

    /**
     * Adds a GPS event listener to this GPSController.
     * @param listener the new listener.
     */
    public void addGPSEventListener(GPSEventListener listener) {
        listeners.add(listener);
    }

}
