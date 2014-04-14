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
package com.esri.vehiclecommander.messaging;

import com.esri.core.geometry.Point;
import com.esri.core.gps.GPSEventListener;
import com.esri.core.gps.GPSStatus;
import com.esri.core.gps.GeoPosition;
import com.esri.core.gps.GpsGeoCoordinate;
import com.esri.core.gps.Satellite;
import com.esri.core.symbol.advanced.MessageHelper;
import com.esri.vehiclecommander.AppConfigController;
import com.esri.vehiclecommander.ChemLightController;
import com.esri.vehiclecommander.GPSController;
import com.esri.vehiclecommander.UDPBroadcastController;
import com.esri.vehiclecommander.Utilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A controller that manages vehicle status, including sending vehicle status reports.
 */
public class VehicleStatusController implements GPSEventListener {
    
    private final AppConfigController appConfig;
    private final Timer timer;

    private UDPBroadcastController udpBroadcastController;
    private int messagingPort;
    private final Point currentPoint = new Point();
    
    /**
     * Instantiates the controller and starts sending vehicle status updates, assuming
     * the app config's GPSController is providing location information.
     * @param appConfig the application configuration.
     */
    public VehicleStatusController(AppConfigController appConfig) {
        this.appConfig = appConfig;
        messagingPort = appConfig.getPort();
        appConfig.getGpsController().addGPSEventListener(this);
        this.udpBroadcastController = UDPBroadcastController.getInstance(messagingPort);
        timer = new Timer(appConfig.getVehicleStatusMessageInterval(), new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                timer.setDelay(VehicleStatusController.this.appConfig.getVehicleStatusMessageInterval());
                if (VehicleStatusController.this.appConfig.getPort() != messagingPort) {
                    messagingPort = VehicleStatusController.this.appConfig.getPort();
                    udpBroadcastController = UDPBroadcastController.getInstance(messagingPort);
                }
                try {
                    sendVehicleStatusReport();
                } catch (XMLStreamException ex) {
                    Logger.getLogger(VehicleStatusController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        timer.start();
    }

    @Override
    protected void finalize() throws Throwable {
        timer.stop();
    }
    
    private void sendVehicleStatusReport() throws XMLStreamException {
        boolean sendMessage = false;
        double x = 0;
        double y = 0;
        synchronized (currentPoint) {
            if (null != currentPoint && !currentPoint.isEmpty()) {
                sendMessage = true;
                x = currentPoint.getX();
                y = currentPoint.getY();
            }
        }
        if (sendMessage) {
            StringWriter xmlStringWriter = new StringWriter();
            XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(xmlStringWriter);
            xmlStreamWriter.writeStartDocument();
            xmlStreamWriter.writeStartElement("geomessages");
            xmlStreamWriter.writeStartElement("geomessage");
            xmlStreamWriter.writeAttribute("v", Utilities.GEOMESSAGE_VERSION);

            Utilities.writeTextElement(xmlStreamWriter,
                    MessageHelper.MESSAGE_2525C_TYPE_PROPERTY_NAME, "sysmsg");
            Utilities.writeTextElement(xmlStreamWriter,
                    MessageHelper.MESSAGE_ACTION_PROPERTY_NAME, "UPDATE");
            Utilities.writeTextElement(xmlStreamWriter,
                    MessageHelper.MESSAGE_ID_PROPERTY_NAME, UUID.randomUUID().toString());
            Utilities.writeTextElement(xmlStreamWriter,
                    MessageHelper.MESSAGE_WKID_PROPERTY_NAME, Integer.toString(Utilities.WGS84.getID()));
            Utilities.writeTextElement(xmlStreamWriter,
                    MessageHelper.MESSAGE_2525C_CONTROL_POINTS_PROPERTY_NAME,
                    x + "," + y);
            Utilities.writeTextElement(xmlStreamWriter, "uniquedesignation", appConfig.getUsername());
            Utilities.writeTextElement(xmlStreamWriter, "type", appConfig.getVehicleType());
            String dateString = Utilities.DATE_FORMAT_GEOMESSAGE.format(new Date());
            Utilities.writeTextElement(xmlStreamWriter, "datetimevalid", dateString);
            Utilities.writeTextElement(xmlStreamWriter, "fuel_state", "100");
            Utilities.writeTextElement(xmlStreamWriter, 
                    "1.0".equals(appConfig.getGeomessageVersion()) ? "sys_msg" : "system_msgs",
                    "Operational");
            for (int i = 1; i <= 4; i++) {
                /**
                 * Status coded values:
                 * 1 = Operational
                 * 2 = Advisory
                 * 3 = Critical
                 * 4 = Inoperable
                 */
                Utilities.writeTextElement(xmlStreamWriter, "sys_status_" + i, "1");
            }

            xmlStreamWriter.writeEndElement(); // geomessage
            xmlStreamWriter.writeEndElement(); // geomessages
            xmlStreamWriter.writeEndDocument();
            xmlStreamWriter.flush();
            String messageText = xmlStringWriter.toString();
            try {
                udpBroadcastController.sendUDPMessage(messageText.getBytes());
            } catch (IOException ex) {
                Logger.getLogger(ChemLightController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void onStatusChanged(GPSStatus newStatus) {
        //Do nothing
    }

    public void onPositionChanged(GeoPosition newPosition) {
        GpsGeoCoordinate location = newPosition.getLocation();
        synchronized (currentPoint) {
            currentPoint.setXY(location.getLongitude(), location.getLatitude());
        }
    }

    public void onNMEASentenceReceived(String newSentence) {
        //Do nothing
    }

    public void onSatellitesInViewChanged(Map<Integer, Satellite> sattellitesInView) {
        //Do nothing
    }
    
}
