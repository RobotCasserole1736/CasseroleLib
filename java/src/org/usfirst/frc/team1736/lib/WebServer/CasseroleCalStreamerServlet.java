package org.usfirst.frc.team1736.lib.WebServer;

import javax.servlet.annotation.WebServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

@SuppressWarnings("serial")
@WebServlet(name = "Casserole Calibration Streamer Servlet", urlPatterns = { "/calstream" })
class CasseroleCalStreamerServlet extends WebSocketServlet {
	 
    @Override
    public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(999999999); //I really don't want a timeout, and dont care if it stays open indefinitely...
        factory.register(CasseroleCalStreamerSocket.class);
    }
}
