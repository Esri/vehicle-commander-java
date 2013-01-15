package com.esri.vehiclecommander;

import com.esri.core.symbol.advanced.Message;
import com.esri.core.symbol.advanced.MessageProcessor;
import com.esri.core.symbol.advanced.SymbolDictionary.DictionaryType;
import com.esri.map.MessageGroupLayer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * A layer that displays MIL-STD-2525C messages from an XML string.
 */
public class Mil2525CMessageLayer extends MessageGroupLayer {

    private final AppConfigController appConfig;

    /**
     * Constructs a Mil2525CMessageLayer, assuming that the ArcGIS Runtime deployment
     * is located in the application directory. Use the other constructor if the
     * ArcGIS Runtime deployment is elsewhere.
     * @param xmlMessageFilename the XML file on which this layer is based.
     * @param name the layer name.
     * @param mapController the MapController.
     * @param appConfig the application configuration's controller.
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public Mil2525CMessageLayer(String xmlMessageFilename, String name, MapController mapController, AppConfigController appConfig)
            throws IOException, ParserConfigurationException, SAXException {
        super(DictionaryType.Mil2525C);
        this.appConfig = appConfig;
        init(xmlMessageFilename, name, mapController, null);
    }

    /**
     * Constructs a Mil2525CMessageLayer.
     * @param xmlMessageFilename the XML file on which this layer is based.
     * @param name the layer name.
     * @param mapController the MapController.
     * @param appConfig the application configuration's controller.
     * @param symbolDictionaryPath the directory containing the ArcGIS Runtime deployment.
     *        This must not be null! If it's null, call the other constructor.
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public Mil2525CMessageLayer(String xmlMessageFilename, String name, MapController mapController, AppConfigController appConfig, String symbolDictionaryPath)
            throws IOException, ParserConfigurationException, SAXException {
        super(DictionaryType.Mil2525C, symbolDictionaryPath);
        this.appConfig = appConfig;
        init(xmlMessageFilename, name, mapController, symbolDictionaryPath);
    }

    private void init(String xmlMessageFilename, String name, MapController mapController, String symbolDictionaryPath) throws ParserConfigurationException, SAXException, IOException {
        this.setName(name);
        final MessageProcessor processor = null == symbolDictionaryPath ?
            new MessageProcessor(DictionaryType.Mil2525C, this) :
            new MessageProcessor(DictionaryType.Mil2525C, this, symbolDictionaryPath);
        Mil2525CMessageParser parser = new Mil2525CMessageParser(appConfig.isShowMessageLabels());
        final ArrayList<Message> messages = parser.parseMessages(new File(xmlMessageFilename));
        mapController.addListener(new MapControllerListenerAdapter() {

            @Override
            public void mapReady() {
                synchronized (messages) {
                    for (Message message : messages) {
                        if (!appConfig.isShowMessageLabels()) {
                            message.setProperty("AdditionalInformation", "");
                            message.setProperty("UniqueDesignation", "");
                        }
                        try {
                            //Any other problem simply throws a RuntimeException, but a missing
                            //message ID crashes the JVM. Therefore, we test for that case and
                            //throw our own RuntimeException.
                            if (null == message.getID()) {
                                throw new RuntimeException("Message ID is null");
                            } else {
                                processor.processMessage(message);
                            }
                        } catch (RuntimeException re) {
                            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Bad message in layer\n\tMessage: " + message + "\n\tError: " + re.getMessage());
                        }
                    }
                }
            }
        });
    }

}
