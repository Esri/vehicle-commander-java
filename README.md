# vehicle-commander

The Vehicle Commander template demonstrates best practices for building in-vehicle military applications with ArcGIS Runtime.  The Vehicle Commander template contains source code for creating a sample in-vehicle, touchscreen application.

![Image of Vehicle Commander]( ScreenShot.png "vehicle-commander")

## Features

* Displays high-performance touchscreen maps, including provisioned basemaps and operational data
* Displays a GPS location (simulated or serial GPS)
* Communicates with other machines running Vehicle Commander to display their GPS locations
* Allows users to open map packages as layers on the map
* Allows users to run viewshed analysis using a geoprocessing package

## Instructions

### General Help

* [New to Github? Get started here.](http://htmlpreview.github.com/?https://github.com/Esri/esri.github.com/blob/master/help/esri-getting-to-know-github.html)

### Getting Started with the Vehicle Commander
* Copy required message resources.  
    * In a file browser or shell, navigate to source/VehicleCommander. Copy all files named afm*.json to <RuntimeSDKJava>/ArcGISRuntime10.1.1/Resources/Symbols/Mil2525c/MessageTypes. This enables the application to display certain message types that other instances of the application send.
* Install and configure Apache Ant
    * Download Ant from the [Apache Ant Project](http://ant.apache.org/bindownload.cgi) and unzip to a location on your machine
    * Set environment variable `ANT_HOME` to Ant Install Location
    * Add Ant\bin to your path: `%ANT_HOME%\bin`
    * NOTE: Ant requires the [Java Developer Kit (JDK)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) to be installed and the environment variable `JAVA_HOME` to be set to this location
    * To verify your Ant Installation: Open Command Prompt> `ant -v` and verify it runs and returns the version correctly 
* To build or run the Vehicle Commander application
    * Open Command Prompt>
    * > cd vehicle-commander\source\VehicleCommander
    * To Build: > ant
    * To Run: > ant run
    * Verify “Build Succeeded” 
* \(**Optional**\) To build or run the Message Simulator application
    * Open Command Prompt>
    * > cd vehicle-commander\source\MessageSimulator
    * To Build: > ant
    * To Run: > ant run
    * Verify “Build Succeeded” 

## Requirements

* ArcGIS Runtime SDK for Java 10.1.1
* Java Development Environment (this template has been tested with Ant and Eclipse)
* Apache Ant - used to compile and run source
* See the [ArcGIS Runtime SDK for Java](http://resources.arcgis.com/en/help/system-requirements/10.1/index.html#/ArcGIS_Runtime_SDK_10_1_1_for_Java/015100000093000000/) page for detailed system requirements

### Services

* The default application uses ArcGIS Online services to display basemaps. See the configuration file mapconfig.xml for the services used.

## Resources

* Learn more about Esri's [ArcGIS for Defense maps and apps](http://resources.arcgis.com/en/communities/defense-and-intelligence/).
* This application uses [Esri's ArcGIS Runtime SDK for Java 10.1.1](http://resources.arcgis.com/en/communities/runtime-java/);
see the site for concepts, samples, and references for using the API to create mapping applications.
* A deployment release and a more detailed description of this template, including operating instructions, are included with the current release of this template available at [ArcGIS Online](http://www.arcgis.com/home/item.html?id=ae30551d12f443cb903f4829b03de315)

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Contributing

Anyone and everyone is welcome to contribute.

## Licensing

Copyright 2012 Esri

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

[](Esri Tags: ArcGIS Defense and Intelligence Situational Awareness ArcGIS Runtime JavaSE 10.1.1)
[](Esri Language: Java)
