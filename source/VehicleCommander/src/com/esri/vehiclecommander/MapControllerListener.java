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
package com.esri.vehiclecommander;

/**
 * A listener for MapController events. Add a MapControllerListener to a MapController
 * by calling MapController.addListener(MapControllerListener).
 */
public interface MapControllerListener {

    /**
     * Called when map layers are added or removed via the MapController.
     * @param isOverlay true if and only if an overlay layer was added or removed.
     */
    public void layersChanged(boolean isOverlay);

    /**
     * Called when the map is ready. This event fires when the JMap mapReady
     * event fires.
     */
    public void mapReady();

}
