package esg.harvest.publish.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import esg.harvest.publish.api.MetadataRepositoryCrawler;
import esg.harvest.publish.api.RecordConsumer;

/**
 * Subclass of {@link MetadataRepositoryCrawlerManagerImpl} configured for publishing.
 * 
 * @author luca.cinquini
 */
@Component
public class PublisherCrawlerManagerImpl extends MetadataRepositoryCrawlerManagerImpl{ // {
	
	private static final Log LOG = LogFactory.getLog(PublisherCrawlerManagerImpl.class);
	
	@Autowired
	public PublisherCrawlerManagerImpl( final MetadataRepositoryCrawler[] _crawlers,  //)//final MetadataRepositoryCrawler[] _crawlers) //, 
			                            final @Qualifier("indexer") RecordConsumer indexer) 
			{
		super(_crawlers);
		//LOG.debug("CONSTRUCTOR: PublisherCrawlerManagerImpl");
		this.subscribe(indexer);
	}

}
