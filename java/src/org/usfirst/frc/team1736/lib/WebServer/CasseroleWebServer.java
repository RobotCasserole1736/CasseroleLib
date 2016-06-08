package org.usfirst.frc.team1736.lib.WebServer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class CasseroleWebServer {
	

	static Server server;
	
	public void startServer(){
		
		//New server will be on the robot's address plus port 8080 (http://127.0.0.1:8080)
		server = new Server(8080);
		
		//Set up classes which will handle web requests
		//I'm not entirely certain how we'll make this work, but here's my first pass:
		//The build process has been modified to also copy the web resource files to the RIO
		//Since we're not really concerned about security, the files are all accessible.
		//the resource_handler makes the .html/.css files on the RIO available to a client to access freely.
		//index.html is served by default if no other specific file is requested.
		//The ServletContextHandler holds more specific types of content that can be served up directly.
		//Mostly this is the JSON data streams for displaying state data and config and stuff.
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);
		
		ResourceHandler resource_handler = new ResourceHandler();
	    resource_handler.setDirectoriesListed(true);
	    resource_handler.setWelcomeFiles(new String[]{ "index.html" });
	    resource_handler.setResourceBase(".");
		server.insertHandler(resource_handler);
		
//		ServletHolder basicHolder = new ServletHolder("basic", new CasseroleBasicServlet());
//		context.addServlet(basicHolder, "/basic");
		
//		ServletHolder pingHolder = new ServletHolder("ping", new CasserolePingServlet());
//		context.addServlet(pingHolder, "/ping");
		
//		CasseroleWebpageFromFileServlet dataWebpage = new CasseroleWebpageFromFileServlet();
//		dataWebpage.setFile("./testData.html");
//		ServletHolder dataWebpageHolder = new ServletHolder("data", dataWebpage);
//		context.addServlet(dataWebpageHolder, "/data");
		
		//StateStreamer - broadcasts present state of robot. RPM's, voltages, etc.
		ServletHolder statestreamHolder = new ServletHolder("statestream", new CasseroleStateStreamerServlet());
		context.addServlet(statestreamHolder, "/statestream");
		
		//CalStreamer - Handles calibration viewing and updating 
		ServletHolder calstreamHolder = new ServletHolder("calstream", new CasseroleCalStreamerServlet());
		context.addServlet(calstreamHolder, "/calstream");
		
		// Kick off server in brand new thread.
		// Thanks to Team 254 for an example of how to do this!
		Thread serverThread = new Thread(new Runnable() {
			@Override
			public void run(){
				try {
					server.start();
					server.join();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		});
		serverThread.setName("CasseroleWebServerThread");
		serverThread.setPriority(Thread.MIN_PRIORITY);
		serverThread.start();

	}	
	
}
