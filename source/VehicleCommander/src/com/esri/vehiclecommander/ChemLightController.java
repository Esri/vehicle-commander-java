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
import java.awt.Color;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Manages chem lights, including creating, displaying, and storing them.
 */
public class ChemLightController extends GraphicsLayerController {

    private final UDPBroadcastController udpBroadcastController;
    private final AppConfigController appConfig;

    /**
     * Creates a new ChemLightController for the application.
     * @param mapController the application's MapController.
     * @param appConfig the application's AppConfigController.
     */
    public ChemLightController(MapController mapController, AppConfigController appConfig) {
        super(mapController, "Chem Lights");
        udpBroadcastController = UDPBroadcastController.getInstance(appConfig.getPort());
        this.appConfig = appConfig;
    }

    /**
     * Adds a chem light and sends it out to clients. If this application has a
     * UDPMessageGraphicsLayerController or other mechanism to receive UDP messages,
     * the chem light should appear on the map soon after this method returns.
     * @param point the chem light's location.
     * @param wkid the WKID of the location's spatial reference.
     * @param color the chem light's color. ArcGIS Runtime 1.0 only supports Color.RED,
     *              Color.GREEN, Color.BLUE, and Color.YELLOW. Other colors will
     *              render as a gray square and will be sent out to clients as a
     *              hex string.
     * @throws XMLStreamException
     */
    public void addChemLight(Point point, int wkid, Color color) throws XMLStreamException {
        String id = UUID.randomUUID().toString();
        StringWriter xmlStringWriter = new StringWriter();
        XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(xmlStringWriter);
        xmlStreamWriter.writeStartDocument();
        xmlStreamWriter.writeStartElement("geomessages");
        xmlStreamWriter.writeStartElement("geomessage");
        xmlStreamWriter.writeAttribute("v", Utilities.GEOMESSAGE_VERSION);

        Utilities.writeTextElement(xmlStreamWriter,
                MessageHelper.MESSAGE_2525C_TYPE_PROPERTY_NAME, "chemlight");
        Utilities.writeTextElement(xmlStreamWriter,
                MessageHelper.MESSAGE_ID_PROPERTY_NAME, id);
        Utilities.writeTextElement(xmlStreamWriter,
                MessageHelper.MESSAGE_WKID_PROPERTY_NAME, Integer.toString(wkid));
        Utilities.writeTextElement(xmlStreamWriter,
                MessageHelper.MESSAGE_2525C_CONTROL_POINTS_PROPERTY_NAME,
                point.getX() + "," + point.getY());
        Utilities.writeTextElement(xmlStreamWriter,
                MessageHelper.MESSAGE_ACTION_PROPERTY_NAME, "UPDATE");
        Utilities.writeTextElement(xmlStreamWriter, "uniquedesignation", appConfig.getUsername());
        Utilities.writeTextElement(xmlStreamWriter, "color", getChemLightColorString(color));
        String dateString = Utilities.DATE_FORMAT_GEOMESSAGE.format(new Date());
        Utilities.writeTextElement(xmlStreamWriter, "datetimesubmitted", dateString);
        Utilities.writeTextElement(xmlStreamWriter, "datetimemodified", dateString);

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

    private static String getChemLightColorString(Color color) {
        if (Color.RED.equals(color)) {
            return "1";
        } else if (Color.YELLOW.equals(color)) {
            return "4";
        } else if (Color.GREEN.equals(color)) {
            return "2";
        } else if (Color.BLUE.equals(color)) {
            return "3";
        } else {
            /**
             * ArcGIS Runtime does not currently support custom chem light colors.
             * But we can send a hex string in case some client can use it.
             */
            return "#" + Integer.toHexString(color.getRed()) + Integer.toHexString(color.getGreen()) + Integer.toHexString(color.getBlue());
        }
    }
}
