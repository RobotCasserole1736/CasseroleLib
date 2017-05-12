package org.usfirst.frc.team1736.lib.WebServer;

/*
 *******************************************************************************************
 * Copyright (C) 2017 FRC Team 1736 Robot Casserole - www.robotcasserole.org
 *******************************************************************************************
 *
 * This software is released under the MIT Licence - see the license.txt
 *  file in the root of this repo.
 *
 * Non-legally-binding statement from Team 1736:
 *  Thank you for taking the time to read through our software! We hope you
 *   find it educational and informative! 
 *  Please feel free to snag our software for your own use in whatever project
 *   you have going on right now! We'd love to be able to help out! Shoot us 
 *   any questions you may have, all our contact info should be on our website
 *   (listed above).
 *  If you happen to end up using our software to make money, that is wonderful!
 *   Robot Casserole is always looking for more sponsors, so we'd be very appreciative
 *   if you would consider donating to our club to help further STEM education.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
class CasseroleWebpageFromFileServlet extends HttpServlet {

    private final int FILE_CHUNK_CHARS = 100;
    public String filename = null;


    public void setFile(String file_in) {
        filename = file_in;
    }


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        char[] webpageFileContents = new char[FILE_CHUNK_CHARS];
        int chars_read = 0;

        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);

        // open the html file and feed it as the response, chunk by chunk
        FileReader fstream = new FileReader(filename);
        BufferedReader webpageFile = new BufferedReader(fstream);
        System.out.println("Feeding client the data-viewing page...");

        chars_read = webpageFile.read(webpageFileContents, 0, FILE_CHUNK_CHARS);
        while (chars_read != -1) {
            response.getWriter().write(webpageFileContents, 0, chars_read);
            chars_read = webpageFile.read(webpageFileContents, 0, FILE_CHUNK_CHARS);
        }

        webpageFile.close();

    }

}
