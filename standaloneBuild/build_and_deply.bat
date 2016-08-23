@echo OFF
::Standalone build and deploy batch script
:: Requires-
::  1) Apache Ant is installed on local pc, with appropriate version
::  2) JAVA_HOME environment variable setup
::  3) Java & Ant on system PATH environment variable correctly

call ant -noinput -nice 1 -nouserlib deploy
pause
