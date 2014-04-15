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
package com.esri.vehiclecommander.model;

import com.esri.core.geometry.Point;
import com.esri.core.gps.GPSStatus;
import com.esri.core.gps.IGPSWatcher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import javax.swing.Timer;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A GPS simulator for the Vehicle Commander application.
 */
public class GPSSimulator extends GPSProvider {

	public void dispose() { 
    	// required by IGPSWatcher at 10.2 (note: can't use @Override or it won't build with pre-10.2)
    }
	 
    private class GPXHandler extends DefaultHandler {

        private List<GPSTrackPoint> gpsTrackPoints = new ArrayList<GPSTrackPoint>();

        private Double lat = null;
        private Double lon = null;
        private Calendar time = null;
        private double speed = 0;

        private boolean readingTrkpt = false;
        private boolean readingTime = false;
        private boolean readingSpeed = false;

        @Override
        public void startDocument() throws SAXException {
            gpsTrackPoints = new ArrayList<GPSTrackPoint>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ("trkpt".equalsIgnoreCase(qName)) {
                readingTrkpt = true;
                String latString = attributes.getValue("lat");
                String lonString = attributes.getValue("lon");
                try {
                    //Do these both in one try block. We could use two try blocks,
                    //but one value is no good without the other, so don't bother.
                    lat = Double.parseDouble(latString);
                    lon = Double.parseDouble(lonString);
                } catch (Exception e) {
                    //Do nothing
                }
            } else if (readingTrkpt && "time".equalsIgnoreCase(qName)) {
                readingTime = true;
            } else if (readingTrkpt && "speed".equalsIgnoreCase(qName)) {
                readingSpeed = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (readingTime) {
                try {
                    time = DatatypeConverter.parseDateTime(new String(ch, start, length));
                } catch (IllegalArgumentException iae) {
                    //Do nothing
                }
            } else if (readingSpeed) {
                try {
                    speed = Double.parseDouble(new String(ch, start, length));
                } catch (NumberFormatException nfe) {
                    //Do nothing
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (readingTrkpt && "trkpt".equalsIgnoreCase(qName)) {
                readingTrkpt = false;

                GPSTrackPoint trackPoint = new GPSTrackPoint(new Point(lon, lat), time, speed);
                gpsTrackPoints.add(trackPoint);

                lat = null;
                lon = null;
                time = null;
                speed = 0;
            } else if (readingTime && "time".equalsIgnoreCase(qName)) {
                readingTime = false;
            } else if (readingSpeed && "speed".equalsIgnoreCase(qName)) {
                readingSpeed = false;
            }
        }

        @Override
        public void endDocument() throws SAXException {
            Collections.sort(gpsTrackPoints);
        }

    }
    
    private final Timer timer;
    private final List<GPSTrackPoint> gpsPoints;
    private int gpsPointsIndex = 0;
    private double speedMultiplier = 1.0;
    private int timeout = 0;

    /**
     * Creates a GPSSimulator using built-in GPX file.
     */
    public GPSSimulator() throws ParserConfigurationException, SAXException, IOException {
        this(getSimulatedGPXInputStream());
    }

    /**
     * Creates a new GPSSimulator based on a GPX file.
     * @param gpxFile the GPX file.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public GPSSimulator(File gpxFile) throws ParserConfigurationException, SAXException, IOException {
        this(
                null == gpxFile || !gpxFile.exists() || !gpxFile.isFile()
                ? getSimulatedGPXInputStream()
                : new FileInputStream(gpxFile)
                );
    }

    private GPSSimulator(InputStream gpxInputStream) throws ParserConfigurationException, SAXException, IOException {
        final GPXHandler handler = new GPXHandler();
        SAXParserFactory.newInstance().newSAXParser().parse(gpxInputStream, handler);
        gpsPoints = handler.gpsTrackPoints;
        
        timer = new Timer((int) getNextDelay(), new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                GPSTrackPoint currentTrackPoint = getTrackPoint(0, true);
                sendGPSLocation(
                        currentTrackPoint.getPoint(),
                        getTrackPoint(-1, true).getPoint(),
                        currentTrackPoint.getSpeed());
                gpsPointsIndex++;
                gpsPointsIndex %= gpsPoints.size();
            }
        });
        timer.setInitialDelay(0);
    }

    private static InputStream getSimulatedGPXInputStream() {
        return GPSSimulator.class.getResourceAsStream("/com/esri/vehiclecommander/resources/Route_Archer.gpx");
    }

    private long getNextDelay() {
        if (1 >= gpsPoints.size()) {
            return 1000;
        }
        else {
            long theDelay = 0;
            while (0 >= theDelay) {
                theDelay = getTrackPoint(1, true).getTimestamp().getTimeInMillis()
                        - getTrackPoint(0, true).getTimestamp().getTimeInMillis();
                theDelay = (int) Math.round(((double) theDelay) / speedMultiplier);
            }
            return theDelay;
        }
    }

    private GPSTrackPoint getTrackPoint(int index) {
        return getTrackPoint(index, false);
    }

    /**
     * Gets a track point. If isRelativeIndex is false, the index is used as is.
     * If isRelativeIndex is true, the index provided is relative to the current index.
     * For example, if there are
     * 10 points, numbered 0 through 9, and the simulator is currently on point 6,
     * then:
     * <ul>
     *     <li>getTrackPoint(-2) returns point 4</li>
     *     <li>getTrackPoint(-1) returns point 5</li>
     *     <li>getTrackPoint(0) returns point 6</li>
     *     <li>getTrackPoint(1) returns point 7</li>
     *     <li>getTrackPoint(2) returns point 8</li>
     *     <li>getTrackPoint(3) returns point 0</li>
     * </ul>
     * @param index
     * @param isRelativeIndex
     * @return
     */
    private GPSTrackPoint getTrackPoint(int index, boolean isRelativeIndex) {
        if (0 == gpsPoints.size()) {
            return null;
        } else {
            if (isRelativeIndex) {
                while (index < 0) {
                    index += gpsPoints.size();
                }
                index = (gpsPointsIndex + index) % gpsPoints.size();
            }
            return gpsPoints.get(index);
        }
    }

    /**
     * Moves the simulator back to the beginning of the GPX file, and then starts
     * the simulation (or lets it continue running).
     */
    public void restart() {
        gpsPointsIndex = 0;
        start();
    }

    /**
     * Starts the simulator from its current point.
     */
    public void start() {
        timer.start();
        sendGPSStatus(GPSStatus.RUNNING);
    }

    /**
     * Pauses the simulator. Next time start() is called, it will start from the
     * point at which it was paused.
     */
    public void pause() {
        timer.stop();
        sendGPSStatus(GPSStatus.STOPPED);
    }

    /**
     * Stops the simulator and moves it back to the beginning of the GPX file.
     */
    public void stop() {
        timer.stop();
        sendGPSStatus(GPSStatus.STOPPED);
        gpsPointsIndex = 0;
    }

    /**
     * Returns the speed multiplier.
     * @return the speed multiplier.
     */
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    /**
     * Sets a speed multiplier, which increases or decreases the speed of GPS updates
     * compared to the actual speed specified in the GPX file.
     * @param speedMultiplier the speed multiplier to set.
     */
    public void setSpeedMultiplier(double speedMultiplier) {
        if (0 < speedMultiplier) {
            this.speedMultiplier = speedMultiplier;
            timer.setDelay((int) getNextDelay());
        }
    }

    /**
     * @return the timeout
     * @see IGPSWatcher
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     * @see IGPSWatcher
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns the status of the simulator.
     * @return the status of the simulator.
     * @see IGPSWatcher
     */
    public GPSStatus getStatus() {
        if (null == timer) {
            return GPSStatus.NOT_CONNECTED;
        } else {
            return timer.isRunning() ? GPSStatus.RUNNING : GPSStatus.STOPPED;
        }
    }

}
