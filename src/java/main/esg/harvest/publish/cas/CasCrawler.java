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
package esg.harvest.publish.cas;

import java.net.URI;
import java.util.List;

import org.jdom.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import esg.harvest.core.Record;
import esg.harvest.publish.api.MetadataRepositoryCrawler;
import esg.harvest.publish.api.MetadataRepositoryType;
import esg.harvest.publish.api.RecordProducer;
import esg.harvest.publish.xml.MetadataHandler;
import esg.harvest.utils.HttpClient;
import esg.harvest.utils.XmlParser;

/**
 * Class to harvest metadata from a remote CAS server.
 */
@Service
public class CasCrawler implements MetadataRepositoryCrawler {
		
	private final MetadataHandler metadataHandler;
	
	@Autowired
	public CasCrawler(final @Qualifier("metadataHandlerCasRdfImpl") MetadataHandler metadataHandler) {
		this.metadataHandler = metadataHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	public void crawl(final URI uri, final boolean recursive, final RecordProducer callback) throws Exception {
		
		// parse XML document
		final String xml = (new HttpClient()).doGet( uri.toURL() );
		final XmlParser xmlParser = new XmlParser(false);
		final Document doc = xmlParser.parseString(xml);
		
		// process XML
		final List<Record> records = metadataHandler.parse(doc.getRootElement());
		
		// index records
		for (final Record record : records) callback.notify(record);

	}
	
	/**
	 * {@inheritDoc}
	 */
	public MetadataRepositoryType supports() {
		return MetadataRepositoryType.CAS;
	}

}
