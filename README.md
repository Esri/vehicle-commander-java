# vehicle-commander

The Vehicle Commander template demonstrates best practices for building in-vehicle military applications with ArcGIS.  The Vehicle Commander template contains source code for an in-vehicle application and directions for building the application from source.  To download a precompiled distribution of the application, visit: 
[ArcGIS for Defense and Intelligence](http://www.arcgis.com/home/group.html?owner=Arcgisonline_defense&title=ArcGIS%20for%20Defense%20and%20Intelligence).

![Image of Vehicle Commander](ScreenShot.png "vehicle-commander")

## The Vehicle Commander

* Displays high-performance touchscreen maps, including provisioned basemaps and operational data
* Displays a GPS location (simulated or serial GPS)
* Communicates with other machines running Vehicle Commander to display their GPS locations
* Allows users to open map packages as layers on the map
* Allows users to run viewshed analysis using a geoprocessing package

## About the Readme documentation
The documentation is divided between Quick Start, Detailed Usage, and other sections. These sections include: 

* [Hardware and software requirements](#hardware-and-software-requirements)
* [Quick Start and Build Instructions](#quick-start-instructions)
* [Release Notes](#release-notes--known-issues)
* [Detailed Usage](#detailed-instructions)
* [Conclusion](#conclusion)

## General Help

* [New to Github? Get started here.](http://htmlpreview.github.com/?https://github.com/Esri/esri.github.com/blob/master/help/esri-getting-to-know-github.html)

## Hardware and Software Requirements

### Hardware Requirements
Hardware requirements for this template are the same as those for ArcGIS Runtime SDK for Java.  See the Runtime SDK documentation for more information. 

### Software Requirements
* Building Requirements
    * ArcGIS Runtime SDK for Java (10.1.1 or later).
    * [Java SE Development Kit (JDK)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 6 or higher.
    * [Apache Ant](http://ant.apache.org/).
    * Optionally, an integrated development environment (IDE). This template’s source code includes project files for the following IDEs:
        * [NetBeans](http://netbeans.org/) 6.9.1 or higher
        * [Eclipse](http://eclipse.org/) Indigo (3.7.1) or higher 
* Deployed Application Requirements
    * Software requirements for this template are the same as those for ArcGIS Runtime SDK for Java.  See the Runtime SDK documentation for more information. 
    * Some important items to note: 
        * The need for Java 6 or 7 using the Java Runtime Environment (JRE) from Oracle.
        * The ArcGIS Runtime does not run in a Remote Desktop session or as a remote X client.
        * The appropriate driver for display adapter should be installed.

## Quick Start Instructions

This section is for developers who just need to quickly build and run the application.
 
### Verify your development environment
* Ensure the JavaSE SDK is installed
    * Java is installed and added to your path and the environment variable `JAVA_HOME` to be set to this location
    * To verify your Java Installation: Open Command Prompt> `java -version` and verify it runs and returns the version correctly 
* Ensure Apache Ant is installed and configured 
    * Download Ant from the [Apache Ant Project](http://ant.apache.org/bindownload.cgi) and unzip to a location on your machine
    * Set environment variable `ANT_HOME` to Ant Install Location
    * Add Ant\bin to your path: `%ANT_HOME%\bin`
    * To verify your Ant Installation: Open Command Prompt> `ant -version` and verify it runs and returns the help correctly 

### Configure/copy the required files
* Some files will need manually updated/copied on your system
    * Copy new Message Types 
        * In a file browser or shell, navigate to source/VehicleCommander. Copy all files named afm*.json to <RuntimeSDKJava>/ArcGISRuntime{Version}/Resources/Symbols/Mil2525c/MessageTypes. 
        * This enables the application to display certain message types that other instances of the application send.
* Check the contents of  source/VehicleCommander/mapconfig.xml
    * On non-windows systems, you may need to edit the default locations in this file to locations on your system 

### Build and run the application
* (Linux only) Initialize runtime. From a Command Prompt> `> . {RuntimeSDKHome}/init_sdk_java.sh`
* To build or run the application with ant
    * Open Command Prompt>
    * `> cd vehicle-commander\source\VehicleCommander`
    * To Build: `> ant`
    * To Run: `> ant run`
    * Verify “Build Succeeded” 
* To run the application
    * From a Command Prompt> `> java -jar VehicleCommander.jar`
    * See [Running the application](#running) for more detailed command line options

## Release Notes / Known Issues

### 10.2

#### What’s New in Vehicle Commander 10.2
* Uses ArcGIS Runtime 10.2
    * Works with OpenGL 1.4 and higher and includes Intel GPU.
* Bug fixes and performance improvements.

### 10.1.1

#### What’s New in Vehicle Commander 10.1.1
* Uses ArcGIS Runtime 10.1.1.
    * Works with OpenGL 2.1 and higher.
* Uses new directory structure.
* Bug fixes and performance improvements.

#### Release Notes 
* To deploy this application, you need to rebuild with your own license string or provide your own license strings via the -license and -exts command line switches. 
* The Buddies and Observations buttons in the main menu currently do nothing. In a future release, each of these buttons will be either implemented or removed.
* Onboard GPS works only on Windows, not on Linux. This is a limitation of ArcGIS Runtime 10.1.1.
* (Linux) Rotating by holding the V and B keys rotates only one step at a time. This happens because of a [documented JRE bug](http://bugs.sun.com/view_bug.do?bug_id=4153069) on Linux. This issue might be worked around in a future Vehicle Commander release or a future JRE release.
* Attempting to navigate to an invalid MGRS location can crash Vehicle Commander. This is a behavior of ArcGIS Runtime. Vehicle Commander attempts to ignore or repair invalid MGRS strings. Even if the string matches the basic MGRS patterns or can be made to match, that does not guarantee that the combination of grid zone identifier, 100,000-meter square identification, and easting/northing is valid in the MGRS. A future release of ArcGIS Runtime will address this issue.
* (Linux) When running in undecorated mode (no title bar), application dialogs can pop under the main application and become difficult to access. Since the checkbox for changing between decorated and undecorated mode is itself on a dialog, decorated mode is now the default. You can use undecorated mode on Linux, but dialogs will be difficult to use.
* (Linux) If you run the application from a USB drive on a Linux machine, the Runtime deployment (ArcGISRuntime10.1.1 directory) will no longer work on other Linux machines. This limitation is documented in the [ArcGIS Runtime 10.1.1 release notes](http://resources.arcgis.com/en/help/runtime-java/concepts/index.html#//01qv00000036000000). One solution is to copy the application to the hard drive before running. Another solution is to make a backup copy of the ArcGISRuntime10.1.1 directory, especially the ClientLx and LocalServerLx subdirectories.

## Detailed Instructions 

This section contains more detailed instructions that will help you learn more about the Vehicle Commander application. This part is divided into the following sections:

* [Configuring the  the Vehicle Commander Build](#configuring-the-build)
* [Deploying the Vehicle Commander application](#deploying-the-application)
* [Running the Vehicle Commander application](#running)
* [Using the Vehicle Commander application](#using-the-application)

### Configuring the Build

1. (Optional) If you want to open map packages and/or run viewshed analysis in the compiled application, you need to use an ArcGIS Runtime license string. Copy your license string from the ArcGIS Runtime SDK License Viewer. Open the class com.esri.vehiclecommander.VehicleCommanderJFrame Java source file. Look for the BUILT_IN_LICENSE_STRING static field, and paste your license string as the value of this field. (Alternatively, you can pass a license string as a command line argument to the application.)
2. (Optional) If you want to run viewshed analysis in the compiled application, you need to use an ArcGIS Runtime Spatial Analyst license string. Copy your license string from the ArcGIS Runtime SDK License Viewer. Open the class com.esri.vehiclecommander.VehicleCommanderJFrame Java source file. Look for the BUILT_IN_EXTS_STRING static field, and paste your license string as the value of this field.
3. Build the application for deployment using Ant with the build.xml file in source/VehicleCommander:

    `C:\vehicle-commander\source\VehicleCommander>ant deploy`

If you wish to use an IDE, configure it to use the included Ant build.xml script for best results. For Eclipse, you may wish to follow the documentation on [installing the ArcGIS Runtime SDK Eclipse plugin](http://resources.arcgis.com/en/help/runtime-java/concepts/index.html#/Installing_the_Eclipse_Plugin/01qv00000007000000/). For NetBeans, you must mount all the JARs in <RuntimeSDKJava>/SDK/jars, using a NetBeans Ant library or adding JARs directly to your project.

Note: if you wish to run the application from your IDE on Linux, you must run the initialization shell script found in <RuntimeSDKJava>, and then run your IDE from the same shell. If desired, you can automate running this script using /etc/profile or ~/.bash_profile.

### Deploying the Application

1. Using the directions in [Quick Start Instructions](#quick-start-instructions), build the Vehicle Commander application using  ant deploy. This will build the application.
2. Add an ArcGIS Runtime deployment to the application/VehicleCommander directory. The simplest way is to use the ArcGISRuntime{Version} directory that came with the prebuilt template from ArcGIS Online in the application/VehicleCommander. You can also create an ArcGIS Runtime deployment with the the [ArcGIS Runtime SDK Deployment Builder](http://resources.arcgis.com/en/help/runtime-java/concepts/index.html#/Creating_a_runtime_deployment/01qv00000013000000/). If you create your own:

   * Include at least Local Server, GPS, and Military Message Processing (MIL2525C).
   * If you want to run viewshed analysis, include Geoprocessing and Spatial Analyst.
   * In a file browser or shell, navigate to source/VehicleCommander and copy all the afm*.json files to your deployment’s ArcGISRuntime{Version}/resources/symbols/mil2525c/messagetypes directory. The Ant build script will do this for you if your ArcGIS Runtime deployment is in applications/VehicleCommander/ArcGISRuntime{Version}.
3. Your application deployment should contain at least the following:
   * ArcGISRuntime{Version} directory (ArcGIS Runtime deployment)
   * lib directory (populated by Ant build script)
   * ArcGIS Runtime SDK JAR files from <RuntimeSDKJava>/SDK/jars
   * beansbinding-1.2.1.jar from source/VehicleCommander/lib
   * mapconfig.xml file from source/VehicleCommander (populated by Ant build script)
   * VehicleCommander.jar file compiled using directions in [Quick Start](#quick-start-instructions) 
4. Edit your copy of mapconfig.xml as instructed in [Running the Vehicle Commander application](#running).
5. (Optional) If you want to set initial user settings using appconfig.xml, copy it from source\VehicleCommander\src\com\esri\vehiclecommander\resources to your application directory, alongside VehicleCommander.jar. Open appconfig.xml in your application directory. You can set the following:
   * User name: a display name for position updates the Vehicle Commander application sends to other applications. This name does not need to be unique. Set the value of the <user> element’s name attribute to the desired user name.
   * User ID: an ID that uniquely identifies the user in position updates that the Vehicle Commander application sends to other applications. The ID should be unique among other machines that will be able to send and receive position updates with this machine. It is recommended, though not required, that the value be a GUID. Set the value of the &lt;user&gt; element’s id attribute to the desired unique ID.
   * User code: a MIL-STD-2525C 15-character SIC code that the Vehicle Commander application will include in the position reports it sends. Set the value of &lt;code&gt;to the desired SIC.
   * Messaging port: the UDP port on which the Vehicle Commander sends and receives position reports. Note that the Message Simulator always sends messages on port 45678, so the <port> setting only applies to the Vehicle Commander. Set the value of <port> to the desired port number. Note that this port needs to be open in your firewall in order to see military messages on the map.
   * Messaging interval: the number of milliseconds the Vehicle Commander will wait between sending out its position reports. Set the value of <interval> to the desired number of milliseconds.
   * GPS type: “simulated” for simulated GPS, or “onboard” for serial GPS. The default is “simulated.” Set the value of the <gps> element’s type attribute to the desired GPS type. [Is this installed in the Dev versions? needs a link to where to get it, unless it is in the GIT pakage, in which case we need to include it in the Template Contents section]
   * GPS GPX file: the filename of a GPX file to use when GPS type is “simulated.” Set the value of the <gps> element’s gpx attribute to the filename. If absent and the GPS type is “simulated,” the application uses a built-in GPS file over Jalalabad, Afghanistan.
   * GPS speed multiplier: a multiplier for simulated GPS speed. The default is 1 (actual speed). 0.5 means half-speed, and 2 means double-speed. Set the value of the <gps> element’s speedMultiplier attribute to the desired GPS speed multiplier.
If appconfig.xml is absent the first time the application runs, the application will use default values. The user can change these values in the running application, as described in [Running the Vehicle Commander application.](#running)

### Running

This section assumes that you have deployed the Vehicle Commander application, or you are running it from the application/VehicleCommander directory.

1. Review the [software requirements](#software-requirements).
2. In your application directory, open mapconfig.xml in a text editor. If mapconfig.xml is absent, the application will open with a blank map. You can define the following:
   * The initial extent, using a point in the map’s spatial reference and a scale.
   * The spatial reference of the map, using a well-known ID (WKID). Web Mercator (WKID 3857) is used by ArcGIS Online services and is normally recommended.
   * The map layers. You can add the following types of layers:
      * TiledCacheLayer: a compact map cache, or a tile package (.tpk) enabled for ArcGIS Runtime. Set the value of `<datasetpath>` to the .tpk filename or cache directory, using a relative or absolute path. The cache directory is the one that contains conf.xml and conf.cdi.
      * TiledMapServiceLayer: an ArcGIS Server cached map service or image service. Set the value of `<url>` to the service REST URL.
      * LocalDynamicMapLayer: an ArcGIS map package (.mpk) enabled for ArcGIS Runtime. Set the value of `<datasetpath>` to the .mpk filename, using a relative or absolute path.
      * DynamicMapServiceLayer: an ArcGIS Server dynamic map service or image service. Set the value of `<url>` to the service REST URL.
      * Mil2525CMessageLayer: an XML file of military messages with MIL-STD-2525C symbol codes. Use one `<geomessages>` tag containing one or more `<geomessage>` tags formatted as follows. Lines and polygons can be used (with appropriate symbol codes) by creating semicolon-separated lists of points in the `<_control_points>` element.

     &lt;geomessages v="1.1"> <br>
     &lt;geomessage>  <br>
     &lt;_type&gt;position_report&lt;/_type&gt; <br>
     &lt;_action&gt;UPDATE&lt;/_action&gt; <br>
     &lt;_id&gt;{3bf3e432-94c5-4db8-b9a1-42318708af74}&lt;/esri_id&gt; <br>
     &lt;_control_points&gt;7843104.64,4087771.88&lt;/_control_points&gt; <br>
     &lt;_wkid&gt;3857&lt;/_wkid&gt; <br>
     &lt;sic&gt;SFGPUCII---F---&lt;/sic&gt; <br>
     &lt;/geomessage&gt; <br>
     &lt;/geomessages&gt; <br>
          
3. (Optional) If you want to run viewshed analysis in the application, add a `<viewshed>` element to mapconfig.xml. It should be a child of the `<mapconfig>` element and should contain the following child elements:

   * `<servicepath>`: the path to a GPK, or the URL to a geoprocessing service.
   * `<taskname>`: the name of the viewshed task in the GPK or service.
   * `<observerheight>`: the observer height (in the units of the elevation dataset being used by the GPK or service) to use for viewshed analysis.
   * `<observerparamname>`: the name of the observer feature set parameter.
   * `<observerheightparamname>`: the name of the observer height parameter.
   * `<radiusparamname>`: the name of the radius parameter.

4. Run the Vehicle Commander application:
   * Open a command prompt and navigate to application/VehicleCommander folder
   * Enter:  `java -jar VehicleCommander.jar`
   * At the end of the command line, you can pass the following parameters:
   * -mapconfig `<XML file>`: use a map configuration file other than the mapconfig.xml file located in the application directory.
   * -license `<license string or file>`: use a license other than the one compiled into the application. This can be either the license string itself or the name of a file containing only the license string. 
   * -exts <extension license strings or file>: use a set of extension license strings other than the ones compiled into the application. This can be either a semicolon-separated list of extension license strings or the name of a file containing only a semicolon-separated list of extension license strings. 
   * -version: print the application version and exit.

The application opens as shown in [Using the Application.](#using-the-application)

* If the application crashes, it is possible the machine does not have the proper OpenGL capabilities. Refer to [Hardware Requirements](#hardware-requirements) to learn how to verify OpenGL.
*  If the map opens blank, verify the paths in mapconfig.xml.
5.If you want to change user settings, click the Main Menu button and go to Options > About Me. A dialog box lets you change the user settings described in [Deploying the Application.](#deploying-the-application)

### Using the Application

![Image of Vehicle Commander](ScreenShotLabels.png "vehicle-commander")

Vehicle Commander provides high performance mapping, situational awareness, and reporting for mounted units. It is intended for touchscreen use, though it also works properly with a mouse; in that sense, the words “click” and “tap” are interchangeable in this section.

#### Mapping

To pan the map, press the mouse and drag, or use the navigation buttons. To zoom in or out, use the mouse wheel or the navigation buttons. To navigate to an MGRS coordinate, go to Main Menu > Navigation and type a valid MGRS string, then type Enter or tap the Go button.

To change the basemap, tap the Basemaps button to open the basemap gallery and tap one of the basemap icons.

To add or remove an MGRS grid, tap the Grid button.

To display the vehicle’s simulated GPS location and broadcast it to other applications, click the Main Menu button and go to Options > Show Me. A green icon indicates the current GPS location and heading, and the map enters Follow Me mode, following the current GPS location. To exit Follow Me mode, zoom or pan the map. To reenter Follow Me mode at any time, click the Follow Me button, located in the lower right corner with the navigation buttons.

While in Follow Me mode, three navigation modes are available and are selected with navigation buttons:

* North Up: the map stays rotated so that north is up, until the user manually rotates the map with the V and B keys.
* Track Up: the map rotates so that the current GPS direction is up.
* Waypoint Up: the map rotates so that the direction from the current GPS location to the selected waypoint is up. To select a waypoint, go to Main Menu > Waypoints. You can read about creating waypoints elsewhere in this document.

To rotate the map clockwise, press and hold the V key. To rotate the map counterclockwise, press and hold the B key. To clear the rotation and orient the map with north up, press the N key.

To add a map overlay, go to Main Menu > Overlays. You can add an ArcGIS map package (.mpk) enabled for ArcGIS Runtime. Clicking Map File opens a file chooser dialog. Navigate to the .mpk of your choice.

You can click the map to identify features from map packages, as well as MIL-STD-2525C symbols on the map. Identified items are shown in a panel. Click the previous and back buttons to view the attributes of identified items. Click the X button to close the identify panel.

A panel at the bottom of the application displays the current position, heading, and time. To change the units used to display the heading, go to Main Menu > Options > About Me. For Heading Units, choose Degrees or Mils and click OK.

To close the Main Menu, click the back arrow button at the top of the menu.

#### Situational awareness and reporting

The map displays moving locations of friendly forces if you run the Vehicle Commander on multiple machines that are connected to the same network router. These machines must have the messaging port (default 45678; see [Deploying the Application](#deploying-the-application) ) open for UDP sending and receiving in the machine’s firewall settings. They must also have unique IDs set under Main Menu > Options > About Me > Unique ID.

Toggle the 911 button to indicate to friendly forces that you need immediate assistance. Your position marker will flash on the display of other vehicles that receive your position reports. Toggle the 911 button off to clear your emergency status.

Use the Chem Light buttons to create digital chem lights. Click a color, and then click the map to place a chem light. The chem light appears on your map, as well as the maps in vehicles that receive your position reports. In the field, different colors of chem lights would have different predetermined meanings to all friendly forces.

To create a spot report:

* Go to Main Menu > Reports > Spot Report.
* Click the top button (Size) to get started. The spot report wizard helps you enter a value for each spot report field in the SALUTE format (Size, Affiliation, Location, Unit, Time, Equipment). Rather than entering text, most fields offer preset fat buttons ideal for quick touchscreen use.
* Go through the fields and choose values.

      1. For Location, choose From Map and click the location for the spot report.
      2. For Equipment, you can choose one of the preset symbols, or you can search for any MIL-STD-2525C symbol by name, tags, or symbol code.
		
* After completing the final field, Equipment, you can click any field to change its value before sending.
* When you are satisfied with your spot report values, click Send. The spot report displays on your map, as well as the maps in vehicles that receive your position reports.

#### Analysis

When properly configured, the application provides advanced geospatial analysis. Tap the Tools button to open the toolbar.
To calculate a viewshed, tap the Viewshed button and follow the dialog’s instructions:

1. Tap a point on the map.
2. Select a viewshed radius in one of various ways:
   * Tap a second point.
   * Type a radius.
   * Choose a preset viewshed radius.
3. Tap the Go button. The viewshed is calculated and displayed on the map.

Calculating a new viewshed removes the previous viewshed from the map. To hide the viewshed, go to Main Menu > Overlays and turn off the Viewshed overlay.

You can use the Route panel to create a route with waypoints. To create a route, tap the Route button and follow the Route panel’s instructions:

* Tap the map to add a waypoint.
* Drag the map to draw a route segment.
* Toggle the Draw Route button:
* Select the button to add waypoints and route segments.
* Deselect the button to activate panning and identifying. This is useful if the route you want to draw will go off the edge of the map. Turn off drawing, pan the map, and turn on drawing to continue placing the route.
* Tap the Undo button to remove the last route segment or waypoint created.
* Tap the Clear button to delete all routes and waypoints. The Clear operation cannot be undone.

To hide the route, go to Main Menu > Overlays and turn off the Route overlay.

## Conclusion

You can use the Vehicle Commander application as-is for in-vehicle situations. You can also use the Vehicle Commander application as a starting point for your own application development. Refer to the [ArcGIS Runtime SDK for Java documentation](http://resources.arcgis.com/en/help/runtime-java/concepts/index.html#/ArcGIS_Runtime_SDK_for_Java/01qv0000001n000000/), as well as the [ArcGIS Runtime SDK for Java Resource Center](http://resources.arcgis.com/en/communities/runtime-java/index.html), in order to get the most out of ArcGIS Runtime.

## Resources

* Learn more about Esri's [ArcGIS for the Military solution](http://solutions.arcgis.com/military/).
* This application uses [Esri's ArcGIS Runtime SDK for Java](http://resources.arcgis.com/en/communities/runtime-java/);
see the site for concepts, samples, and references for using the API to create mapping applications.
* A deployment release and a more detailed description of this template, including operating instructions, are included with the current release of this template available at [ArcGIS Online](http://www.arcgis.com/home/item.html?id=ae30551d12f443cb903f4829b03de315)

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing).

## Licensing

Copyright 2012-2013 Esri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

A copy of the license is available in the repository's
[license.txt](license.txt) file.

Note: Portions of this code use Beans Binding (JSR-295) which is licensed under 
GNU Lesser General Public License 2.1. See [license-ThirdParty.txt](license-ThirdParty.txt) for the details 
of this license or visit the [Beans Binding](http://java.net/projects/beansbinding/) project for more details 

[](Esri Tags: ArcGIS Defense and Intelligence Situational Awareness ArcGIS Runtime JavaSE Military)
[](Esri Language: Java)
