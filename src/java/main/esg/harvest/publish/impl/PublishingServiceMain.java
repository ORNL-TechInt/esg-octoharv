package esg.harvest.publish.impl;

import java.util.Arrays;

import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import esg.harvest.publish.api.MetadataRepositoryType;
import esg.harvest.publish.api.PublishingService;




/**
 * Main class to start the indexing or scrabbing of search records from a remote metadata repository,
 * or deletion of a single record with known identifier.
 * 
 * @author luca.cinquini
 *
 */
public class PublishingServiceMain {

	private static String[] configLocations = new String[] { "classpath:esg\\harvest\\config\\application-context.xml" };
    
    public static String METADATA_URL = "";
    
    private static final Log LOG = LogFactory.getLog(PublishingServiceMain.class);
	
    /**
     * Main method loads the proper web service to invoke from the Spring context.
     * 
     * @param args
     * @throws Exception
     */
    
    public static void main(String[] args) throws Exception {
    	
    	//LOG.debug("***INITIALIZATION***");
		
    	final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configLocations);
    	
    	//LOG.debug("***END INITIALIZATION***\n");
		
    	//LOG.debug("***PROGRAM START***");
		
    	
    	final PublishingService publishingService = (PublishingService)context.getBean("publishingService");
    	
	    final PublishingServiceMain self = new PublishingServiceMain();
	    self.run(publishingService, args);
	    
	    //LOG.debug("***PROGRAM STOP***");
		
    }
    
    /**
	 * Method to execute the web service invocation.
	 * 
	 * @param publishingService
	 * @param args
	 * @throws Exception
	 */
    protected void run(final PublishingService publishingService, final String[] args) throws Exception {
    	//LOG.debug("In main.run()");
	    if (args.length!=1 && args.length!=3) {
	    	exit();
	    }
	    
	    
	    // unpublish single record
	    if (args.length==1) {
	    	
	    	
	    	final String id = args[0];
	    	publishingService.unpublish(Arrays.asList(new String[] {id}));
	    	
	    	
	    	
	    // publish/unpublish full repository
	    } else if (args.length==3) {
	    	
	    	
		    final String uri = args[0];
		    final MetadataRepositoryType type = MetadataRepositoryType.valueOf(args[1]);
		    
		    //change the Metadata file's URL
		    PublishingServiceMain.METADATA_URL = uri;
		    
		    
		    final boolean publish = Boolean.parseBoolean(args[2]);
		    
		    if (publish) {
		    	publishingService.publish(uri, true, type);
		    } else {
		    	publishingService.unpublish(uri, true, type);
		    }
	    }
		
	}
    
    /**
	 * Method to indicate usage and exit the program.
	 */
	protected void exit() {
		
    	System.out.println("Usage #1: to unpublish a single record:");
    	System.out.println("          java esg.search.publish.impl."+this.getClass().getName()+" <id>");
    	System.out.println("Usage #2: to publish or unpublish a remote metadata repository: ");
    	System.out.println("          java esg.search.publish.impl."+this.getClass().getName()+" <Metadata Repository URL> <Metadata repository Type> true|false");
    	System.out.println("          where true:publish, false:unpublish");
    	System.out.println("Example: java esg.search.publish.impl."+this.getClass().getName()+" nasa.jpl.tes.monthly");
    	System.out.println("Example: java esg.search.publish.impl."+this.getClass().getName()+" file:///Users/cinquini/Documents/workspace/esg-search/resources/pcmdi.ipcc4.GFDL.gfdl_cm2_0.picntrl.mon.land.run1.v1.xml THREDDS true|false");
    	System.out.println("Example: java esg.search.publish.impl."+this.getClass().getName()+" http://pcmdi3.llnl.gov/thredds/esgcet/catalog.xml THREDDS true|false");
    	System.out.println("Example: java esg.search.publish.impl."+this.getClass().getName()+" http://esg-datanode.jpl.nasa.gov/thredds/esgcet/catalog.xml THREDDS true|false");
    	System.out.println("Example: java esg.search.publish.impl."+this.getClass().getName()+" file:///Users/cinquini/Documents/workspace/esg-search/resources/ORNL-oai_dif.xml OAI true|false");
    	System.out.println("Example: java esg.search.publish.impl."+this.getClass().getName()+" file:///Users/cinquini/Documents/workspace/esg-search/resources/cas_rdf.xml CAS true|false");
    	System.exit(-1);

	}
    
}
