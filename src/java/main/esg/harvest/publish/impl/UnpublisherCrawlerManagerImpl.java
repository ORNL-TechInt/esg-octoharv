package esg.harvest.publish.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import esg.harvest.publish.api.MetadataRepositoryCrawler;
import esg.harvest.publish.api.RecordConsumer;

/**
 * Subclass of {@link MetadataRepositoryCrawlerManagerImpl} configured for unpublishing.
 * 
 * @author luca.cinquini
 */
@Component
public class UnpublisherCrawlerManagerImpl extends MetadataRepositoryCrawlerManagerImpl {
	private static final Log LOG = LogFactory.getLog(UnpublisherCrawlerManagerImpl.class);
	
	@Autowired
	public UnpublisherCrawlerManagerImpl(final MetadataRepositoryCrawler[] _crawlers, 
			                            final @Qualifier("scrabber") RecordConsumer indexer) {
		super(_crawlers);
		//LOG.debug("CONSTRUCTOR: UnpublisherCrawlerManagerImpl");
		this.subscribe(indexer);
	}

}
