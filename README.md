# CasseroleLib
Common Libraries used year-to-year for FRC robot code

## Contents

### Java Libraries
Common libraries, drivers, and utilities used by team FRC 1736
  - [Javadoc](http://robotcasserole1736.github.io/CasseroleLib/index.html)
  
_Library Highlights:_
  - CasserolePID - Customized PID implementation.
  - Calibration - infrastructure for tunable parameters (setpoints, gains, etc.)
  - Battery Parameter estimation & overcurrent protection algorithms
  - Real-time filter & calculus operations
  - Onboard data-logging libraries
  - Sensor drivers
  - Jetty-based web server for customized calibration interfaces, debugging, and driver information display 

### Calculators
Physics simulations & calculations for design aid in FRC competitions. Requires Matlab or GNU Octave installed.

### logFileSnagger
Python utility to grab .csv files from the robot and transfer them to the local computer for analysis. Requires local install of Python 3.X, and FTP access over the network to the robot.

### Data Viewer
Matlab/Octave based data viewer. Takes .csv files generated from the datalogger library and generates matlab-style plots. Requires a local installation of Matlab or GNU Octave. Can get kinda slow for big .csv files, but kept around as a reference for data analysis.

### Data Viewer 2
Javascript & HTML5 based interactive viewer for .csv files generated from the datalogging library. Fairly quick answer for viewing plots. Special thanks to HighCharts for making this look so awesome!

### webServerContent
HTML, Javascript, and Ant build content required for supporting the Casserole Web Server libraries.


## Usage
  - Clone repo (if you haven't already)
  - Add content from java/lib folder into existing Eclipse project.
  - Install other software as needed (Eclipse, Python, Octave, etc.)
  - Reference Javadoc for class usage.

## Content Samples

Driver View (webserver):

![Casserole Driver View website sample](http://i.imgur.com/Jjtt5qY.gif)

Calibration (webserver):

![Casserole Calibartion website sample](http://i.imgur.com/u9I7qtQ.png)

Data Viewer (Local Data-logging + file snagger + data viewer 2):

![Casserole dataviewer 2 sasmple](http://i.imgur.com/Ii8zkLQ.png)
  
## Notes

This library is still under development - not all libraries are as-of-yet fully functional. We're working on improving it. Check back often! Let us know if there's anything you find useful, or broken!