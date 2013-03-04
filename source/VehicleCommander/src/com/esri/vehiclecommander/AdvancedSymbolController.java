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

import com.esri.core.map.Graphic;
import com.esri.core.symbol.advanced.Message;
import com.esri.core.symbol.advanced.MessageHelper;
import com.esri.core.symbol.advanced.MessageProcessor;
import com.esri.core.symbol.advanced.SymbolDictionary;
import com.esri.core.symbol.advanced.SymbolDictionary.DictionaryType;
import com.esri.core.symbol.advanced.SymbolProperties;
import com.esri.map.GraphicsLayer;
import com.esri.map.Layer;
import com.esri.map.MessageGroupLayer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * A controller to handle the usage of MessageProcessor and SymbolDictionary by
 * the application.
 */
public class AdvancedSymbolController {
    
    /**
     * Set <unique ID>
     */
    private final Set<String> uniqueIds = new HashSet<String>();
    
    /**
     * Map <message type, Map <unique designation, unique ID> >
     */
    private final Map<String, String> uniqueDesignationToId = new HashMap<String, String>();

    private final SymbolDictionary symbolDictionary;
    private MessageProcessor messageProcessor;
    private MessageGroupLayer symbolLayer;
    private final String layerName;
    private final String symbolDictionaryPath;
    private final DictionaryType dictionaryType;
    private final MapController mapController;
    private final Mil2525CMessageParser messageParser;
    private final AppConfigController appConfig;
    
    /**
     * Instantiates a symbol controller.
     * @param type the type (2525C, etc.).
     * @param layerName the name of the layer to which this controller will add symbols.
     * @param appConfig the application configuration object.
     * @param mapController the MapController.
     * @throws ParserConfigurationException if SAX parsing won't work.
     * @throws SAXException if SAX parsing won't work.
     */
    public AdvancedSymbolController(
            DictionaryType type,
            String layerName,
            AppConfigController appConfig,
            MapController mapController) throws ParserConfigurationException, SAXException {
        this(type, layerName, appConfig, null, mapController);
    }

    /**
     * Instantiates a symbol controller.
     * @param type the type (2525C, etc.).
     * @param layerName the name of the layer to which this controller will add symbols.
     * @param appConfig the application configuration object.
     * @param symbolDictionaryPath the path where the symbol dictionary file is found.
     * @param mapController the MapController.
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public AdvancedSymbolController(
            DictionaryType type,
            String layerName,
            AppConfigController appConfig,
            String symbolDictionaryPath,
            MapController mapController) throws ParserConfigurationException, SAXException {
        this.dictionaryType = type;
        this.layerName = layerName;
        this.symbolDictionaryPath = symbolDictionaryPath;
        if (null == symbolDictionaryPath) {
            symbolDictionary = new SymbolDictionary(type);
        } else {
            symbolDictionary = new SymbolDictionary(type, symbolDictionaryPath);
        }
        this.mapController = mapController;
        if (null != mapController) {
            mapController.setAdvancedSymbolController(this);
        }
        messageParser = new Mil2525CMessageParser();
        this.appConfig = appConfig;
        initializeMessageProcessor();
    }

    private void initializeMessageProcessor() {
        if (null == symbolDictionaryPath) {
            symbolLayer = new MessageGroupLayer(dictionaryType);
        } else {
            symbolLayer = new MessageGroupLayer(dictionaryType, symbolDictionaryPath);
        }
        symbolLayer.setName(layerName);
        if (null == symbolDictionaryPath) {
            messageProcessor = new MessageProcessor(dictionaryType, symbolLayer);
        } else {
            messageProcessor = new MessageProcessor(dictionaryType, symbolLayer, symbolDictionaryPath);
        }
        mapController.addLayer(symbolLayer, false);
    }

    /**
     * Gets the SIC for the symbol specified by the symbolName parameter.
     * @param symbolName the name of the symbol,
     * @return the SIC for the symbol specified by the symbolName parameter.
     */
    public String getSic(String symbolName) throws IOException {
        List<SymbolProperties> symbols = findSymbols(symbolName);
        for (SymbolProperties sym : symbols) {
            if (sym.getName().equalsIgnoreCase(symbolName)) {
                return sym.getValues().get("SymbolID");
            }
        }
        return null;
    }

    /**
     * Gets the symbol image for the specified symbol name or SIC.
     * @param symbolNameOrId a symbol name or SIC.
     * @return the symbol image for the specified symbol name or SIC.
     */
    public BufferedImage getSymbolImage(String symbolNameOrId) {
        return symbolDictionary.getSymbolImage(symbolNameOrId, 100, 100);
    }

    /**
     * Searches for symbols having a keyword matching (fully or partially) the provided
     * search string.
     * @param searchString The search string.
     * @return A list of symbols matching the provided search string.
     */
    public List<SymbolProperties> findSymbols(String searchString) throws IOException {
        ArrayList<String> keywords = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(searchString, Utilities.MIL_2525C_WHITESPACE_CHARS);
        while (tok.hasMoreTokens()) {
            keywords.add(tok.nextToken());
        }
        return symbolDictionary.findSymbols(keywords);
    }

    /**
     * Returns a list of symbol categories.
     * @return a list of symbol categories.
     */
    public List<String> getCategories() {
        return symbolDictionary.getFilters().get("Category");
    }

    /**
     * Returns a list of symbols in a category.
     * @param category the category name.
     * @return a list of symbols in a category.
     */
    public List<SymbolProperties> getSymbolsInCategory(String category) throws IOException {
        HashMap<String, List<String>> filters = new HashMap<String, List<String>>(1); 
        ArrayList<String> categories = new ArrayList<String>(1);
        categories.add(category);
        filters.put("Category", categories);
        return symbolDictionary.findSymbols(filters);
    }

    /**
     * Parses the specified XML string and adds any messages to the map if symbols
     * can be found for them.
     * @param xmlMessages an XML string containing messages.
     */
    public void addMessagesToMap(String xmlMessages) {
        if (!mapController.hasLayer(symbolLayer)) {
            initializeMessageProcessor();
        }
        //Parse messages and add to map
        try {
            final List<Message> messages = Collections.synchronizedList(messageParser.parseMessages(xmlMessages));
            synchronized (messages) {
                for (int i = 0; i < messages.size(); i++) {
                    Message message = messages.get(i);
                    String messageType = (String) message.getProperty(MessageHelper.MESSAGE_2525C_TYPE_PROPERTY_NAME);
                    if ("chemlight".equals(messageType)) {
                        messageType = "afmchemlight";
                        message.setProperty(MessageHelper.MESSAGE_2525C_TYPE_PROPERTY_NAME, messageType);
                    } else if ("position_report".equals(messageType)) {
                        messageType = "trackrep";
                        message.setProperty(MessageHelper.MESSAGE_2525C_TYPE_PROPERTY_NAME, messageType);
                    }
                    if (null != message.getID() && !appConfig.getUniqueId().equals(message.getID())
                            /**
                             * If we do something with other report types,
                             * we can remove this next check.
                             */
                            && ("trackrep".equals(messageType) || "spotrep".equals(messageType) || "afmchemlight".equals(messageType))) {

                        String sic = (String) message.getProperty("sic");
                        if (null == sic || 15 != sic.length()) {
                            //Try to get the SIC from the equip_cat
                            String equipCat = (String) message.getProperty("equip_cat");
                            if (null != equipCat) {
                                sic = getSic(equipCat);
                                message.setProperty("sic", sic);
                            }
                        }
                        //Workaround for odd chem light behavior
                        if ("afmchemlight".equals(messageType) && (null == sic || 0 == sic.length())) {
                            sic = " ";
                            message.setProperty("sic", sic);
                        }

                        /**
                         * There are up to two different unique IDs to check. One is
                         * the one in the message. The other is the one pointed to
                         * by the message's unique designation. If we have mapped
                         * either of those, we need to remove it.
                         */
                        synchronized (uniqueIds) {
                            if (uniqueIds.contains(message.getID())) {
                                messageProcessor.processMessage(MessageHelper.createRemoveMessage(dictionaryType, message.getID(), messageType));
                                uniqueIds.remove(message.getID());
                            }
                        }
                        String uniqueDesignation = (String) message.getProperty("UniqueDesignation");
                        if (null != uniqueDesignation && "".equals(uniqueDesignation.trim())) {
                            uniqueDesignation = null;
                        }
                        synchronized (uniqueDesignationToId) {
                            if (null != uniqueDesignation && uniqueDesignationToId.containsKey(uniqueDesignation)) {
                                String uniqueId = uniqueDesignationToId.get(uniqueDesignation);
                                synchronized (uniqueIds) {
                                    if (uniqueIds.contains(uniqueId)) {
                                        messageProcessor.processMessage(MessageHelper.createRemoveMessage(dictionaryType, uniqueId, messageType));
                                        uniqueIds.remove(uniqueId);
                                    }
                                }
                                uniqueDesignationToId.remove(uniqueDesignation);
                            }
                        }

                        if (!appConfig.isShowMessageLabels()) {
                            message.setProperty("AdditionalInformation", "");
                            message.setProperty("UniqueDesignation", "");
                            message.setProperty("speed", "");
                        }

                        try {
                            boolean processed = messageProcessor.processMessage(message);
                            if (!processed) {
                                Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not display message: {0}", message);
                            } else {
                                synchronized (uniqueIds) {
                                    uniqueIds.add(message.getID());
                                }
                                if (null != uniqueDesignation) {
                                    synchronized (uniqueDesignationToId) {
                                        uniqueDesignationToId.put(uniqueDesignation, message.getID());
                                    }
                                }
                            }
                            if ("trackrep".equals(messageType)) {
                                boolean status911 = null != message.getProperty("status911") && !"0".equals(message.getProperty("status911"));
                                message.setProperty(MessageHelper.MESSAGE_ACTION_PROPERTY_NAME, (status911 ? "" : "un-") + "select");
                                processed = messageProcessor.processMessage(message);
                                if (!processed) {
                                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Could not display message: {0}", message);
                                }
                            }
                        } catch (Throwable t) {
                            //Swallow this Throwable so we can continue processing the other messages
                            Logger.getLogger(AdvancedSymbolController.class.getName()).log(Level.SEVERE, null, t);
                        }
                        
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AdvancedSymbolController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(AdvancedSymbolController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public IdentifyResultList identify(float screenX, float screenY, int tolerance) {
        IdentifyResultList results = new IdentifyResultList();
        Layer[] layers = symbolLayer.getLayers();
        for (Layer layer : layers) {
            if (layer instanceof GraphicsLayer) {
                GraphicsLayer gl = (GraphicsLayer) layer;
                int[] graphicIds = gl.getGraphicIDs(screenX, screenY, tolerance);
                for (int id : graphicIds) {
                    Graphic graphic = gl.getGraphic(id);
                    IdentifiedItem item = new IdentifiedItem(
                            graphic.getGeometry(),
                            -1,
                            graphic.getAttributes(),
                            layer.getName() + " " + graphic.getUid());
                    results.add(item, layer);
                }
            }
        }
        return results;
    }

}
