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
import com.esri.core.symbol.advanced.MessageHelper;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A bean that holds the details of a spot report. The spot report is in the standard
 * SALUTE format:
 * <ul>
 *   <li>Size</li>
 *   <li>Activity</li>
 *   <li>Location</li>
 *   <li>Unit (or Uniform)</li>
 *   <li>Time</li>
 *   <li>Equipment</li>
 * </ul>
 */
public class SpotReport {

    /**
     * A value for the size field that indicates that the
     * user selected "Other" as the size.
     */
    public static final int SIZE_OTHER = -1;

    /**
     * A value for the size field that indicates that the size is not yet set.
     */
    public static final int SIZE_NOT_SET = -2;
    
    private int size = SIZE_NOT_SET;
    private String activity;
    private Point location;
    private int locationWkid;
    private String unit;
    private Calendar time;
    private String equipment;
    private String messageId = null;
    private final AppConfigController appConfig;
    private final GPSController gpsController;
    private final MapController mapController;
    private final AdvancedSymbolController mil2525CSymbolController;

    /**
     * Creates a new SpotReport object.
     * @param appConfig the application configuration's controller.
     * @param gpsController the application's GPS controller.
     * @param mapController the application's map controller.
     * @param mil2525CSymbolController the application's MIL-STD-2525C symbol controller.
     */
    public SpotReport(AppConfigController appConfig, GPSController gpsController,
            MapController mapController, AdvancedSymbolController mil2525CSymbolController) {
        this.appConfig = appConfig;
        this.gpsController = gpsController;
        this.mapController = mapController;
        this.mil2525CSymbolController = mil2525CSymbolController;
        regenerateMessageId();
    }

    /**
     * Creates a SpotReport with the specified field values.
     * @param size
     * @param activity
     * @param location
     * @param unit
     * @param time
     * @param equipment
     */
    public SpotReport(AppConfigController appConfig,
            GPSController gpsController, MapController mapController, AdvancedSymbolController mil2525CSymbolController,
            int size, String activity, Point location, int locationWkid, String unit, Calendar time, String equipment) {
        this(appConfig, gpsController, mapController, mil2525CSymbolController);
        this.size = size;
        this.activity = activity;
        this.location = location;
        this.locationWkid = locationWkid;
        this.unit = unit;
        setTime(time);
        this.equipment = equipment;
    }

    /**
     * Resets the message ID to a new random GUID.
     */
    public final void regenerateMessageId() {
        messageId = UUID.randomUUID().toString();
    }

    /**
     * Returns the message ID.
     * @return the message ID.
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Returns the size as a number of troops, or SIZE_OTHER to indicate that the
     * user selected "Other" as the size.
     * @return the size, or SIZE_OTHER
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the size as a number of troops, converted to a String. SIZE_NOT_SET
     * is returned as "N/A". SIZE_OTHER is returned as "Other".
     * @return the size as a number of troops, converted to a String.
     */
    public String getSizeString() {
        switch (size) {
            case SIZE_NOT_SET: {
                return "N/A";
            }

            case SIZE_OTHER: {
                return "Other";
            }

            default: {
                return Integer.toString(size);
            }
        }
    }

    /**
     * Sets the size as a number of troops, or SIZE_OTHER to indicate that the
     * user selected "Other" as the size.
     * @param size the size to set, or SIZE_OTHER
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * @return the activity
     */
    public String getActivity() {
        return activity;
    }

    /**
     * @param activity the activity to set
     */
    public void setActivity(String activity) {
        this.activity = activity;
    }

    /**
     * @return the location
     */
    public Point getLocation() {
        return location;
    }

    public int getLocationWkid() {
        return locationWkid;
    }

    /**
     * @param location the location to set.
     * @param wkid the WKID of the location's spatial reference.
     */
    public void setLocation(Point location, int wkid) {
        this.location = location;
        this.locationWkid = wkid;
    }

    /**
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @param unit the unit to set
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return the time
     */
    public Calendar getTime() {
        return time;
    }

    /**
     * Returns the SpotReport's time, formatted as a standard date-time string.
     * @return the SpotReport's time, formatted as a standard date-time string.
     */
    public String getTimeString() {
        if (null == time) {
            return null;
        } else {
            return Utilities.DATE_FORMAT_GEOMESSAGE.format(time.getTime()).toUpperCase();
        }
    }

    /**
     * @param time the time to set
     */
    public final void setTime(Calendar time) {
        this.time = time;
    }

    /**
     * @return the equipment
     */
    public String getEquipment() {
        return equipment;
    }

    /**
     * @param equipment the equipment to set
     */
    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    /**
     * Returns this spot report as an XML "geomessages" string.
     * @return this spot report as an XML "geomessages" string.
     */
    @Override
    public String toString() {
        try {
            Date theTime;
            if (null != time) {
                theTime = time.getTime();
            } else {
                theTime = new Date();
            }
            StringWriter sw = new StringWriter();
            XMLStreamWriter spotReport = XMLOutputFactory.newInstance().createXMLStreamWriter(sw);
            spotReport.writeStartDocument();
            spotReport.writeStartElement("geomessages");
            spotReport.writeStartElement("geomessage");
            spotReport.writeAttribute("v", Utilities.GEOMESSAGE_VERSION);

            Utilities.writeTextElement(spotReport,
                    MessageHelper.MESSAGE_2525C_TYPE_PROPERTY_NAME, "spotrep");
            Utilities.writeTextElement(spotReport,
                    MessageHelper.MESSAGE_ID_PROPERTY_NAME, messageId);
            Utilities.writeTextElement(spotReport,
                    MessageHelper.MESSAGE_WKID_PROPERTY_NAME, Integer.toString(getLocationWkid()));
            Utilities.writeTextElement(spotReport,
                    MessageHelper.MESSAGE_2525C_CONTROL_POINTS_PROPERTY_NAME,
                    getLocation().getX() + "," + getLocation().getY());
            Utilities.writeTextElement(spotReport,
                    MessageHelper.MESSAGE_ACTION_PROPERTY_NAME, "UPDATE");
            Utilities.writeTextElement(spotReport, "uniquedesignation", appConfig.getUsername());
            try {
                // symbol and text modifiers
                Utilities.writeTextElement(spotReport, "sic", mil2525CSymbolController.getSic(getEquipment()));
            } catch (IOException ex) {
                Logger.getLogger(SpotReport.class.getName()).log(Level.SEVERE, null, ex);
            }

            // salute format attributes
            Utilities.writeTextElement(spotReport, "size", getSizeString());
            Utilities.writeTextElement(spotReport, "activity", getActivity());
            Utilities.writeTextElement(spotReport, "location", mapController.toMilitaryGrid(new Point[] { getLocation() })[0]);
            Utilities.writeTextElement(spotReport, "unit", getUnit());
            Utilities.writeTextElement(spotReport, "equipment", getEquipment());

            Utilities.writeTextElement(spotReport, "activity_cat", getActivity());
            Utilities.writeTextElement(spotReport, "unit_cat", getUnit());
            Utilities.writeTextElement(spotReport, "equip_cat", getEquipment());
            Utilities.writeTextElement(spotReport, "timeobserved", Utilities.DATE_FORMAT_GEOMESSAGE.format(theTime));
            Utilities.writeTextElement(spotReport, "datetimesubmitted", Utilities.DATE_FORMAT_GEOMESSAGE.format(new Date()));

            spotReport.writeEndElement(); // geomessage
            spotReport.writeEndElement(); // geomessages
            spotReport.writeEndDocument();
            spotReport.flush();
            return sw.toString();
        } catch (XMLStreamException ex) {
            Logger.getLogger(SpotReport.class.getName()).log(Level.SEVERE, null, ex);
            return super.toString();
        }
    }
    
}
