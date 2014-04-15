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

import com.esri.vehiclecommander.model.SpotReport;
import java.io.IOException;

/**
 * Manages spot reports, including creating and sending them.
 */
public class SpotReportController extends GraphicsLayerController {

    private final UDPBroadcastController udpBroadcastController;

    /**
     * Creates a new SpotReportController.
     * @param mapController the application's map controller
     * @param messagingPort the UDP port through which spot reports should be broadcast.
     * @param advancedSymbolController the application's advanced symbol controller.
     */
    public SpotReportController(MapController mapController, int messagingPort,
            AdvancedSymbolController advancedSymbolController) {
        super(mapController, "Spot Reports");
        udpBroadcastController = UDPBroadcastController.getInstance(messagingPort);
    }

    /**
     * Sends a new spot report out to UDP clients. This method simply calls
     * submitSpotReport(spotReport, false).
     * @param spotReport the spot report to send.
     * @throws IOException
     */
    public void submitSpotReport(SpotReport spotReport) throws IOException {
        submitSpotReport(spotReport, false);
    }

    /**
     * Sends a spot report out to UDP clients.
     * @param spotReport the spot report to send.
     * @param isUpdate false if the spot report's ID should be regenerated so as to
     *                 be a new unique spot report; true otherwise.
     * @throws IOException
     */
    public void submitSpotReport(SpotReport spotReport, boolean isUpdate) throws IOException {
        if (null != spotReport) {
            if (!isUpdate) {
                spotReport.regenerateMessageId();
            }
            udpBroadcastController.sendUDPMessage(spotReport.toString().getBytes());
        }
    }

}
