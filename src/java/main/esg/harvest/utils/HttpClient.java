/*******************************************************************************
 * Copyright (c) 2010 Earth System Grid Federation
 * ALL RIGHTS RESERVED. 
 * U.S. Government sponsorship acknowledged.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of the <ORGANIZATION> nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package esg.harvest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import esg.harvest.publish.impl.MetadataRepositoryCrawlerManagerImpl;

/**
 * Simple class to execute an HTTP GET/POST request,
 * and return the HTTP response as a single string.
 */
public class HttpClient {
	private static final Log LOG = LogFactory.getLog(HttpClient.class);
	
	/**
	 * Method to execute an HTTP GET request.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public String doGet(final URL url) throws IOException {
						
		// prepare HTTP request
		final URLConnection connection = url.openConnection();		
		connection.setUseCaches(false);
				
	    // execute HTTP request
	    return getResponse(connection);
		
	}
	
	/**
	 * Method to send an XML document as a POST request.
	 * @param url
	 * @param xml
	 * @return
	 * @throws IOException
	 */
	public String doPostXml(final URL url, final String xml) throws IOException {
		
		// prepare HTTP request
	    final URLConnection connection = url.openConnection();
	    connection.setUseCaches(false);
	    connection.setDoOutput(true); // POST method
	    connection.setRequestProperty("Content-Type", "text/xml");
	    connection.setRequestProperty("Charset", "utf-8");
	    
	    // preemptive authentication
	    //final String userpassword = "<username>" + ":" + "<password>";   
	    //final byte[] authEncBytes = Base64.encodeBase64(userpassword.getBytes());
		//final String authStringEnc = new String(authEncBytes);
	    //connection.setRequestProperty("Authorization", "Basic "+ authStringEnc );   
	    	    
		
	    final OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
	    wr.write(xml);
	    wr.flush();
	    wr.close();

	    // execute HTTP request
	    return getResponse(connection);
		
	}
	
	/**
	 * Method to execute an HTTP request (GET/POST) and return the HTTP response.
	 * @param connection
	 * @return
	 * @throws IOException
	 */
	private String getResponse(final URLConnection connection) throws IOException {
		LOG.debug("In HttpClient getResponse");
	    final BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	    final StringBuilder sb = new StringBuilder();
	    String line = null;
	    while ((line = rd.readLine()) != null) {
	    	sb.append(line + "\n");
	    }
	    rd.close();
	    return sb.toString();
	    
	}

}
