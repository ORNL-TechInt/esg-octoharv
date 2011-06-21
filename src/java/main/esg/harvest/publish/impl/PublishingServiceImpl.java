package esg.harvest.publish.impl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import esg.harvest.publish.api.MetadataDeletionService;
import esg.harvest.publish.api.MetadataRepositoryCrawlerManager;
import esg.harvest.publish.api.MetadataRepositoryType;
import esg.harvest.publish.api.PublishingService;

/**
 * Implementation of {@link PublishingService} that delegates all functionality
 * to collaborating beans for crawling remote metadata repositories, producing
 * search records, and consuming search records for ingestion or removal.
 * 
 * @author luca.cinquini
 *
 */
@Service("publishingService")
public class PublishingServiceImpl implements PublishingService {
	private static final Log LOG = LogFactory.getLog(PublishingServiceImpl.class);
	
	/**
	 * Collaborator that crawls remote metadata repositories for the purpose of publishing records into the system.
	 */
	private final MetadataRepositoryCrawlerManager publisherCrawler;
	
	/**
	 * Collaborator that crawls remote metadata repositories for the purpose of unpublishing records from the system.
	 */
	private final MetadataRepositoryCrawlerManager unpublisherCrawler;
	
	/**
	 * Collaborator that deletes records with known identifiers.
	 */
	private final MetadataDeletionService recordRemover;

	@Autowired
	public PublishingServiceImpl(final PublisherCrawlerManagerImpl publisherCrawler,
			                     final UnpublisherCrawlerManagerImpl unpublisherCrawler,
			                     final MetadataDeletionService recordRemover
			) {
		//LOG.debug("CONSTRUCTOR: PublishingServiceImpl");
		
		this.publisherCrawler = publisherCrawler;
		this.unpublisherCrawler = unpublisherCrawler;
		this.recordRemover = recordRemover;
	}

	@Override
	public void publish(String uri, boolean recursive, MetadataRepositoryType metadataRepositoryType) throws Exception {
		//LOG.debug("In PublishingServiceImpl publish()");
		publisherCrawler.crawl(uri, recursive, metadataRepositoryType);
	}

	@Override
	public void unpublish(String uri, boolean recursive,MetadataRepositoryType metadataRepositoryType) throws Exception {
		//LOG.debug("In PublishingServiceImpl unpublish()");
		unpublisherCrawler.crawl(uri, recursive, metadataRepositoryType);

	}

	@Override
	public void unpublish(List<String> ids) throws Exception {
		
		recordRemover.delete(ids);

	}

}
