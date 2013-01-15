package com.esri.vehiclecommander;

import com.esri.core.symbol.advanced.Message;
import com.esri.core.symbol.advanced.MessageHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A parser for MIL-STD-2525C messages in XML. The easiest thing to do is to call
 * parseMessages. But you can also use it as a handler with a SAXParser if desired.
 */
public class Mil2525CMessageParser extends DefaultHandler {
    private final SAXParser saxParser;

    private boolean readingId = false;
    private String elementName = null;
    private final ArrayList<Message> messages = new ArrayList<Message>();
    private Message message = null;
    private String version = null;
    private final boolean showLabels;

    /**
     * Creates a new Mil2525CMessageParser.
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public Mil2525CMessageParser(boolean showLabels) throws ParserConfigurationException, SAXException {
        saxParser = SAXParserFactory.newInstance().newSAXParser();
        this.showLabels = showLabels;
    }

    private static String translateElementName(String elementName) {
        /**
         * TODO these will have to change when message formats change. In particular,
         * element names should be all lowercase, so when that changes in Runtime,
         * we'll have to change it here.
         */
        if ("UniqueDesignation".equalsIgnoreCase(elementName) || "Unique_Designation".equalsIgnoreCase(elementName)) {
            elementName = "UniqueDesignation";
        } else if ("AdditionalInformation".equalsIgnoreCase(elementName)) {
            elementName = "AdditionalInformation";
        }
        return elementName;
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        qName = translateElementName(qName);
        if ("message".equals(qName) || "geomessage".equals(qName)) {
            if (null != message) {
                if (!showLabels) {
                    message.setProperty(translateElementName("uniquedesignation"), "");
                    message.setProperty(translateElementName("additionalinformation"), "");
                }
            }

            message = new Message();
            messages.add(message);
            version = attributes.getValue("v");
        } else if (MessageHelper.MESSAGE_ID_PROPERTY_NAME.equals(qName)) {
            readingId = true;
        }
        elementName = qName;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String charString = new String(ch, start, length);
        if (readingId) {
            message.setID(charString);
        } else if (null != message && null != elementName) {
            message.setProperty(elementName, charString);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        qName = translateElementName(qName);
        if (MessageHelper.MESSAGE_ID_PROPERTY_NAME.equals(qName)) {
            readingId = false;
        }
        elementName = null;
    }

    /**
     * Parses an XML file of messages and returns a list of messages.
     * @param xmlMessageFile the XML message file.
     * @return a list of messages.
     * @throws IOException
     * @throws SAXException
     */
    public synchronized ArrayList<Message> parseMessages(File xmlMessageFile) throws IOException, SAXException {
        messages.clear();
        saxParser.parse(new FileInputStream(xmlMessageFile), this);
        return messages;
    }

    /**
     * Parses an XML string of messages and returns a list of messages.
     * @param xmlMessages  the XML message string.
     * @return a list of messages.
     * @throws IOException
     * @throws SAXException
     */
    public synchronized ArrayList<Message> parseMessages(String xmlMessages) throws IOException, SAXException {
        messages.clear();
        saxParser.parse(new InputSource(new StringReader(xmlMessages)), this);
        return messages;
    }

}
