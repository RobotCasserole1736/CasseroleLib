package org.usfirst.frc.team1736.lib.WebServer;

import javax.servlet.annotation.WebServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@SuppressWarnings("serial")
@WebServlet(name = "Casserole Driver Data Streamer Servlet", urlPatterns = { "/driverviewstream" })
class CasseroleDriverViewStreamerServlet extends WebSocketServlet {
	 
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(10000);
        factory.register(CasseroleDriverViewStreamerSocket.class);
    }
}
