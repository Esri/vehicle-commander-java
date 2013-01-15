package com.esri.vehiclecommander;

import com.esri.core.geometry.AngularUnit;
import com.esri.core.gps.GPSException;
import com.esri.core.gps.GPSUncheckedException;
import com.esri.core.gps.IGPSWatcher;
import com.esri.core.gps.SerialPortGPSWatcher;
import com.esri.gps.GPSType;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A controller for application configuration settings. For example, this class
 * manages settings used for sending position reports to other users.
 */
public class AppConfigController {

    private static final String KEY_USERNAME = AppConfigController.class.getName() + "username";
    private static final String KEY_UNIQUE_ID = AppConfigController.class.getName() + "uniqueId";
    private static final String KEY_SIC = AppConfigController.class.getName() + "sic";
    private static final String KEY_PORT = AppConfigController.class.getName() + "port";
    private static final String KEY_POSITION_MESSAGE_INTERVAL = AppConfigController.class.getName() + "positionMessageInterval";
    private static final String KEY_VEHICLE_STATUS_MESSAGE_INTERVAL = AppConfigController.class.getName() + "vehicleStatusMessageInterval";
    private static final String KEY_GPS_TYPE = AppConfigController.class.getName() + "gpsType";
    private static final String KEY_GPX = AppConfigController.class.getName() + "gpx";
    private static final String KEY_SPEED_MULTIPLIER = AppConfigController.class.getName() + "speedMultiplier";
    private static final String KEY_MPK_CHOOSER_DIR = AppConfigController.class.getName() + "mpkFileChooserDirectory";
    private static final String KEY_GPX_CHOOSER_DIR = AppConfigController.class.getName() + "gpxFileChooserDirectory";
    private static final String KEY_SHOW_MESSAGE_LABELS = AppConfigController.class.getName() + "showMessageLabels";
    private static final String KEY_DECORATED = AppConfigController.class.getName() + "decorated";
    private static final String KEY_SHOW_MGRS_GRID = AppConfigController.class.getName() + "showMgrsGrid";
    private static final String KEY_SHOW_LOCAL_TIME_ZONE = AppConfigController.class.getName() + "showLocalTimeZone";
    private static final String KEY_HEADING_UNITS = AppConfigController.class.getName() + "headingUnits";
    private static final String KEY_GEOMESSAGE_VERSION = AppConfigController.class.getName() + "geomessageVersion";

    private boolean gpsTypeDirty = false;

    private class AppConfigHandler extends DefaultHandler {

        private String username = null;
        private String uniqueId = null;
        private String sic = null;
        private int port = -1;
        private int positionMessageInterval = -1;
        private int vehicleStatusMessageInterval = -1;
        private GPSType gpsType = GPSType.SIMULATED;
        private String gpx = null;
        private double speedMultiplier = -1;
        private int headingUnits = AngularUnit.Code.DEGREE;
        private String geomessageVersion = "1.1";
        
        private boolean readingUser = false;
        private boolean readingCode = false;
        private boolean readingMessaging = false;
        private boolean readingPort = false;
        private boolean readingPositionMessageInterval = false;
        private boolean readingVehicleStatusMessageInterval = false;
        private boolean readingGps = false;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ("user".equalsIgnoreCase(qName)) {
                readingUser = true;
                username = attributes.getValue("name");
                uniqueId = attributes.getValue("id");
            } else if (readingUser) {
                if ("code".equalsIgnoreCase(qName)) {
                    readingCode = true;
                }
            } else if ("messaging".equalsIgnoreCase(qName)) {
                readingMessaging = true;
            } else if (readingMessaging) {
                if ("port".equalsIgnoreCase(qName)) {
                    readingPort = true;
                } else if ("interval".equalsIgnoreCase(qName) || "positionmessageinterval".equalsIgnoreCase(qName)) {
                    //Vehicle Commander 1.0 used "interval" instead of "positionmessageinterval"; accept either one
                    readingPositionMessageInterval = true;
                } else if ("vehiclestatusmessageinterval".equalsIgnoreCase(qName)) {
                    readingVehicleStatusMessageInterval = true;
                }
            } else if ("gps".equalsIgnoreCase(qName)) {
                readingGps = true;
                gpsType = "onboard".equalsIgnoreCase(attributes.getValue("type"))
                        ? GPSType.ONBOARD : GPSType.SIMULATED;
                gpx = attributes.getValue("gpx");
                String speedMultiplierString = attributes.getValue("speedMultiplier");
                if (null != speedMultiplierString) {
                    try {
                        speedMultiplier = Double.parseDouble(speedMultiplierString);
                    } catch (Throwable t) {}
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String value = new String(ch, start, length);
            if (readingCode) {
                sic = value;
            } else {
                try {
                    int intValue = Integer.parseInt(value);
                    if (readingPort) {
                        port = intValue;
                    } else if (readingPositionMessageInterval) {
                        positionMessageInterval = intValue;
                    } else if (readingVehicleStatusMessageInterval) {
                        vehicleStatusMessageInterval = intValue;
                    }
                } catch (NumberFormatException nfe) {

                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("user".equalsIgnoreCase(qName)) {
                readingUser = false;
            } else if ("code".equalsIgnoreCase(qName)) {
                readingCode = false;
            } else if ("messaging".equalsIgnoreCase(qName)) {
                readingMessaging = false;
            } else if ("port".equalsIgnoreCase(qName)) {
                readingPort = false;
            } else if ("interval".equalsIgnoreCase(qName) || "positionmessageinterval".equalsIgnoreCase(qName)) {
                //Vehicle Commander 1.0 used "interval" instead of "positionmessageinterval"; accept either one
                readingPositionMessageInterval = false;
            } else if ("vehiclestatusmessageinterval".equalsIgnoreCase(qName)) {
                readingVehicleStatusMessageInterval = false;
            } else if ("gps".equalsIgnoreCase(qName)) {
                readingGps = false;
            }
        }
        
    }

    private final Preferences preferences;
    private final Set<AppConfigListener> listeners = new HashSet<AppConfigListener>();

    private GPSController gpsController;

    /**
     * Creates a new AppConfigController. This constructor first reads the user's
     * settings from the system. Then, if appconfig.xml is present in the working
     * directory, any settings not present in the user profile will be read from
     * appconfig.xml.
     */
    public AppConfigController() {
        preferences = Preferences.userRoot();
        
        try {
            resetFromAppConfigFile(false);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(AppConfigController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(AppConfigController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AppConfigController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(AppConfigController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void addListener(AppConfigListener listener) {
        listeners.add(listener);
    }

    /**
     * Returns the file that will be read when resetting application configuration
     * settings. This file may or may not actually exist.
     * @return the file that will be read when resetting application configuration
     *         settings.
     */
    private URI getAppConfigFileUri() throws URISyntaxException {
        File configFile = new File("./appconfig.xml");
        if (configFile.exists()) {
            return configFile.toURI();
        } else {
            return getClass().getResource("resources/appconfig.xml").toURI();
        }
    }

    /**
     * Resets application configuration settings by reading appconfig.xml found
     * in the working directory.
     * @param overwriteExistingSettings true if settings that are present should
     *                                  be overwritten; false if settings that
     *                                  are present should not be overwritten
     */
    public final void resetFromAppConfigFile(boolean overwriteExistingSettings) throws ParserConfigurationException, SAXException, IOException, URISyntaxException {
        if (overwriteExistingSettings || null == getUsername() || null == getUniqueId()
                || null == getGpsType() || null == getGpx() || 0 >= getSpeedMultiplier()
                || null == getSic() || -1 == getPort() || -1 == getPositionMessageInterval()
                || -1 == getVehicleStatusMessageInterval()) {
            AppConfigHandler handler = new AppConfigHandler();
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(getAppConfigFileUri().toString(), handler);
            if (overwriteExistingSettings || null == getUsername()) {
                setUsername(handler.username);
            }
            if (overwriteExistingSettings || null == getUniqueId()) {
                setUniqueId(handler.uniqueId);
            }
            if (overwriteExistingSettings || null == getSic()) {
                setSic(handler.sic);
            }
            if (overwriteExistingSettings || -1 == getPort()) {
                setPort(handler.port);
            }
            if (overwriteExistingSettings || -1 == getPositionMessageInterval()) {
                setPositionMessageInterval(handler.positionMessageInterval);
            }
            if (overwriteExistingSettings || -1 == getVehicleStatusMessageInterval()) {
                setVehicleStatusMessageInterval(handler.vehicleStatusMessageInterval);
            }
            if (overwriteExistingSettings || null == getGpsType()) {
                setGpsType(handler.gpsType, false);
            }
            if (overwriteExistingSettings || null == getGpx()) {
                setGpx(handler.gpx, false);
            }
            if (overwriteExistingSettings || -1 == getSpeedMultiplier()) {
                setSpeedMultiplier(handler.speedMultiplier);
            }
            if (overwriteExistingSettings || null == getGeomessageVersion()) {
                setGeomessageVersion(handler.geomessageVersion);
            }
            resetGps();
        }
    }

    private void setPreference(String key, String value) {
        if (null != key) {
            if (null == value) {
                preferences.remove(key);
            } else {
                preferences.put(key, value);
            }
        }
    }

    private void setPreference(String key, int value) {
        preferences.putInt(key, value);
    }

    private void setPreference(String key, double value) {
        preferences.putDouble(key, value);
    }

    private void setPreference(String key, boolean value) {
        preferences.putBoolean(key, value);
    }

    /**
     * Saves the specified username to the application configuration settings.
     * @param username the username, or null to erase the setting.
     */
    public void setUsername(String username) {
        setPreference(KEY_USERNAME, username);
    }

    /**
     * Returns the stored username, or null if no username has been set.
     * @return the stored username, or null if no username has been set.
     */
    public final String getUsername() {
        return preferences.get(KEY_USERNAME, null);
    }

    /**
     * Saves the specified unique ID to the application configuration settings.
     * Normally this is a UUID/GUID.
     * @param uniqueId the unique ID. Unique ID cannot be null, so if uniqueId parameter
     *                 is null, the unique ID will be set to a new random GUID.
     */
    public void setUniqueId(String uniqueId) {
        if (null == uniqueId) {
            uniqueId = UUID.randomUUID().toString();
        }
        setPreference(KEY_UNIQUE_ID, uniqueId);

    }

    /**
     * Returns the stored unique ID. If no ID has been set, a new ID will be generated.
     * @return the stored unique ID. If no ID has been set, a new ID will be generated.
     */
    public final String getUniqueId() {
        String uniqueId = preferences.get(KEY_UNIQUE_ID, null);
        if (null == uniqueId) {
            uniqueId = UUID.randomUUID().toString();
            setUniqueId(uniqueId);
        }
        return uniqueId;
    }

    /**
     * Saves the specified symbol ID code to the application configuration settings.
     * @param sic the symbol ID code, or null to erase the setting.
     */
    public void setSic(String sic) {
        setPreference(KEY_SIC, sic);
    }

    /**
     * Returns the stored symbol ID code, or null if no symbol ID code has been set.
     * @return the stored symbol ID code, or null if no symbol ID code has been set.
     */
    public final String getSic() {
        return preferences.get(KEY_SIC, null);
    }

    /**
     * Saves the specified UDP port number for messaging to the application configuration settings.
     * @param port the messaging port number.
     */
    public void setPort(int port) {
        setPreference(KEY_PORT, port);
    }

    /**
     * Returns the stored UDP port number for messaging, or -1 if no port number has been set.
     * @return the stored UDP port number for messaging, or -1 if no port number has been set.
     */
    public final int getPort() {
        return preferences.getInt(KEY_PORT, -1);
    }

    /**
     * Saves the specified position message interval, in milliseconds, to the application configuration settings.
     * @param positionMessageInterval the position message interval, in milliseconds.
     */
    public void setPositionMessageInterval(int positionMessageInterval) {
        setPreference(KEY_POSITION_MESSAGE_INTERVAL, positionMessageInterval);
    }

    /**
     * Returns the stored position message interval, in milliseconds, or -1 if no messaging interval has been set.
     * @return the stored position message interval, in milliseconds, or -1 if no messaging interval has been set.
     */
    public final int getPositionMessageInterval() {
        return preferences.getInt(KEY_POSITION_MESSAGE_INTERVAL, -1);
    }

    /**
     * Saves the specified vehicle status message interval, in milliseconds, to the application configuration settings.
     * @param vehicleStatusMessageInterval the vehicle status message interval, in milliseconds.
     */
    public void setVehicleStatusMessageInterval(int vehicleStatusMessageInterval) {
        setPreference(KEY_VEHICLE_STATUS_MESSAGE_INTERVAL, vehicleStatusMessageInterval);
    }

    /**
     * Returns the stored vehicle status message interval, in milliseconds, or -1 if no messaging interval has been set.
     * @return the stored vehicle status message interval, in milliseconds, or -1 if no messaging interval has been set.
     */
    public final int getVehicleStatusMessageInterval() {
        return preferences.getInt(KEY_VEHICLE_STATUS_MESSAGE_INTERVAL, -1);
    }

    /**
     * Saves the GPS type to the application configuration settings.
     * @param gpsType the GPS type.
     */
    public void setGpsType(GPSType gpsType) {
        setGpsType(gpsType, true);
    }

    private void setGpsType(GPSType gpsType, boolean resetNow) {
        GPSType oldType = getGpsType();
        setPreference(KEY_GPS_TYPE, gpsType.toString());

        if (null == oldType || !oldType.equals(gpsType)) {
            gpsTypeDirty = true;
            if (resetNow) {
                resetGps();
            }
        }
    }

    private void resetGps() {
        if (null != gpsController) {
            try {
                gpsController.stop();
            } catch (GPSException ex) {
                Logger.getLogger(AppConfigController.class.getName()).log(Level.SEVERE, null, ex);
            }
            switch (getGpsType()) {
                case ONBOARD: {
                    try {
                        gpsController.setGpsWatcher(new SerialPortGPSWatcher());
                    } catch (GPSUncheckedException gpsue) {
                        gpsController.setGpsWatcher(null);
                        Utilities.showGPSErrorMessage(gpsue.getMessage());
                    }
                    break;
                }

                case SIMULATED:
                default: {
                    try {
                        gpsController.setGpsWatcher(new GPSSimulator(null == getGpx() ? null : new File(getGpx())));
                    } catch (Exception ex) {
                        gpsController.setGpsWatcher(null);
                        Logger.getLogger(AppConfigController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            gpsTypeDirty = false;
            try {
                gpsController.start();
            } catch (GPSException ex) {
                Logger.getLogger(AppConfigController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Returns the GPS type.
     * @return the GPS type.
     */
    public GPSType getGpsType() {
        String name = preferences.get(KEY_GPS_TYPE, null);
        if (null == name) {
            return null;
        } else {
            try {
                return GPSType.valueOf(name);
            } catch (Throwable t) {
                return null;
            }
        }
    }

    /**
     * Saves the GPX file to be used for simulated GPS to the application configuration settings.
     * @param gpx the GPX file to be used for simulated GPS.
     */
    public void setGpx(String gpx) {
        setGpx(gpx, true);
    }

    private void setGpx(String gpx, boolean resetNow) {
        String oldGpx = getGpx();
        setPreference(KEY_GPX, gpx);
        if (resetNow) {
            if (gpsTypeDirty) {
                resetGps();
            } else if (null == oldGpx) {
                if (null != gpx) {
                    resetGps();
                }
            } else if (!oldGpx.equals(gpx)) {
                resetGps();
            }
        }
    }

    /**
     * Returns the GPX file to be used for simulated GPS.
     * @return the GPX file to be used for simulated GPS.
     */
    public String getGpx() {
        return preferences.get(KEY_GPX, null);
    }

    /**
     * Saves the simulated GPS speed multiplier to the application configuration settings.
     * @param multiplier the simulated GPS speed multiplier.
     */
    public void setSpeedMultiplier(double multiplier) {
        setPreference(KEY_SPEED_MULTIPLIER, multiplier);
        if (null != gpsController) {
            IGPSWatcher gpsWatcher = gpsController.getGpsWatcher();
            if (gpsWatcher instanceof GPSSimulator) {
                ((GPSSimulator) gpsWatcher).setSpeedMultiplier(multiplier);
            }
        }
    }

    /**
     * Returns the simulated GPS speed multiplier, or -1 if no speed multiplier has been set.
     * @return the simulated GPS speed multiplier, or -1 if no speed multiplier has been set.
     */
    public final double getSpeedMultiplier() {
        return preferences.getDouble(KEY_SPEED_MULTIPLIER, -1);
    }

    /**
     * Saves the current directory for the map package file chooser to the application
     * configuration settings.
     * @param the current directory for the map package file chooser.
     */
    public void setMPKFileChooserCurrentDirectory(String dir) {
        setPreference(KEY_MPK_CHOOSER_DIR, dir);
    }

    /**
     * Returns the stored current directory for the map package file chooser, or
     * null if no directory has been set.
     * @return the stored current directory for the map package file chooser, or
     *         null if no directory has been set.
     */
    public String getMPKFileChooserCurrentDirectory() {
        return preferences.get(KEY_MPK_CHOOSER_DIR, null);
    }

    /**
     * Saves the current directory for the GPX file chooser to the application
     * configuration settings.
     * @param the current directory for the GPX file chooser.
     */
    public void setGPXFileChooserCurrentDirectory(String dir) {
        setPreference(KEY_GPX_CHOOSER_DIR, dir);
    }

    /**
     * Returns the stored current directory for the GPX file chooser, or
     * null if no directory has been set.
     * @return the stored current directory for the GPX file chooser, or
     *         null if no directory has been set.
     */
    public String getGPXFileChooserCurrentDirectory() {
        return preferences.get(KEY_GPX_CHOOSER_DIR, null);
    }

    /**
     * Gets the application's GPSController.
     * @return the gpsController
     */
    public GPSController getGpsController() {
        return gpsController;
    }

    /**
     * Gives this AppConfigController a reference to the application's GPSController.
     * @param gpsController the gpsController to set
     */
    public void setGpsController(GPSController gpsController) {
        this.gpsController = gpsController;
    }

    /**
     * Returns true if the application should show labels for new message features.
     * @return true if the application should show labels for new message features.
     */
    public boolean isShowMessageLabels() {
        return preferences.getBoolean(KEY_SHOW_MESSAGE_LABELS, true);
    }

    /**
     * Tells the application whether it should show labels for new message features.
     * @param showMessageLabels true if the application should show labels for new message features.
     */
    public void setShowMessageLabels(boolean showMessageLabels) {
        setPreference(KEY_SHOW_MESSAGE_LABELS, showMessageLabels);
    }

    /**
     * Returns true if the application should be decorated (title bar, resizable, etc.).
     * @return true if the application should be decorated (title bar, resizable, etc.).
     */
    public boolean isDecorated() {
        return preferences.getBoolean(KEY_DECORATED, false);
    }

    /**
     * Tells the application whether it should be decorated (title bar, resizable, etc.).
     * @param decorated true if the application should be decorated (title bar, resizable, etc.).
     */
    public void setDecorated(final boolean decorated) {
        boolean oldDecorated = isDecorated();
        setPreference(KEY_DECORATED, decorated);
        if (decorated != oldDecorated) {
            for (final AppConfigListener listener : listeners) {
                listener.decoratedChanged(decorated);
            }
        }
    }
    
    /**
     * Returns true if the application should show an MGRS grid on the map.
     * @return true if the application should show an MGRS grid on the map.
     */
    public boolean isShowMgrsGrid() {
        return preferences.getBoolean(KEY_SHOW_MGRS_GRID, false);
    }

    /**
     * Tells the application whether it should show an MGRS grid on the map.
     * @param showMessageLabels true if the application should show an MGRS grid on the map.
     */
    public void setShowMgrsGrid(final boolean showMgrsGrid) {
        setPreference(KEY_SHOW_MGRS_GRID, showMgrsGrid);
    }
    
    /**
     * Returns true if the application should display the time in the machine's
     * time zone.
     * @return true if the application should display the time in the machine's
     *         time zone.
     */
    public boolean isShowLocalTimeZone() {
        return preferences.getBoolean(KEY_SHOW_LOCAL_TIME_ZONE, false);
    }

    /**
     * Tells the application whether it should display the time in the machine's
     * time zone.
     * @param showLocalTimeZone true if the application should display the time
     *                          in the machine's time zone.
     */
    public void setShowLocalTimeZone(boolean showLocalTimeZone) {
        setPreference(KEY_SHOW_LOCAL_TIME_ZONE, showLocalTimeZone);
    }
    
    /**
     * Tells the application the units in which to display the GPS heading.
     * @param an AngularUnit.Code constant representing the units in which to display
     *        the GPS heading.
     * @see AngularUnit.Code
     */
    public void setHeadingUnits(int headingUnits) {
        setPreference(KEY_HEADING_UNITS, headingUnits);
    }

    /**
     * Returns the units in which to display the GPS heading.
     * @return an AngularUnit.Code constant representing the units in which to display
     *         the GPS heading. The default is degrees.
     */
    public int getHeadingUnits() {
        return preferences.getInt(KEY_HEADING_UNITS, AngularUnit.Code.DEGREE);
    }
    
    /**
     * Tells the application the Geomessage version to use for outgoing messages.
     * @param geomessageVersion the Geomessage version.
     */
    public void setGeomessageVersion(String geomessageVersion) {
        setPreference(KEY_GEOMESSAGE_VERSION, geomessageVersion);
    }
    
    /**
     * Returns the Geomessage version that the application is using for outgoing messages.
     * @return the Geomessage version that the application is using for outgoing messages.
     */
    public String getGeomessageVersion() {
        return preferences.get(KEY_GEOMESSAGE_VERSION, "1.1");
    }

}
