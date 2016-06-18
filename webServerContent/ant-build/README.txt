In order to properly build and deploy the web content to the RIO, there are changes needed to the ant build .xml's. The details of these changess are dependant upon exactly how the build works year-to-year. Hopefully it's fairly stable. For reference, the modified files for 2016 are provided.

The basic modifications expected:

Ensure all jetty .jar's are on the classpsath for compilation and loading onto the RIO.
Add build target to move all .html and .css from local resources folder to RIO's resources folder