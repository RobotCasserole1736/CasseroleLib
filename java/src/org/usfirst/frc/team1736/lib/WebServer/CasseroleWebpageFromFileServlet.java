package org.usfirst.frc.team1736.lib.WebServer;

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
	
	public void setFile(String file_in){
		filename = file_in;
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		char[] webpageFileContents = new char[FILE_CHUNK_CHARS];
		int chars_read = 0;
		
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		
		//open the html file and feed it as the response, chunk by chunk
		FileReader fstream = new FileReader(filename);
		BufferedReader webpageFile = new BufferedReader(fstream);
		System.out.println("Feeding client the data-viewing page...");
		
		chars_read = webpageFile.read(webpageFileContents,0,FILE_CHUNK_CHARS);
		while(chars_read != -1){		
			response.getWriter().write(webpageFileContents,0,chars_read);
			chars_read = webpageFile.read(webpageFileContents,0,FILE_CHUNK_CHARS);
		}
		
		webpageFile.close(); 
		
	}

}
