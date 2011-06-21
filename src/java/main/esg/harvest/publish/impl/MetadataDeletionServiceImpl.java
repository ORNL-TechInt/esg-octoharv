package esg.harvest.publish.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import esg.harvest.core.Record;
import esg.harvest.core.RecordImpl;
import esg.harvest.publish.api.MetadataDeletionService;
import esg.harvest.publish.api.RecordConsumer;

/**
 * Implementation of {@link MetadataDeletionService} that produces stub records 
 * and sends them to a collaborating {@link RecordConsumer} for removal from the system.
 * 
 * @author luca.cinquini
 *
 */
@Component
public class MetadataDeletionServiceImpl extends RecordProducerImpl implements MetadataDeletionService {
	
	@Autowired
	public MetadataDeletionServiceImpl(final @Qualifier("scrabber") RecordConsumer scrabber) {
		this.subscribe(scrabber);
	}

	@Override
	public void delete(List<String> ids) throws Exception {
		
		for (final String id : ids) {		
			final Record record = new RecordImpl(id);
			this.notify(record);
		}

	}

}
