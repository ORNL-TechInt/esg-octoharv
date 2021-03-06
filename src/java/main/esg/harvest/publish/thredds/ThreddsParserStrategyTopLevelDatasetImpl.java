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
package esg.harvest.publish.thredds;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


import thredds.catalog.InvAccess;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDocumentation;
import thredds.catalog.InvProperty;
import thredds.catalog.ThreddsMetadata.GeospatialCoverage;
import thredds.catalog.ThreddsMetadata.Variable;
import thredds.catalog.ThreddsMetadata.Variables;
import ucar.nc2.units.DateRange;
import esg.harvest.core.Record;
import esg.harvest.core.RecordImpl;
import esg.harvest.publish.impl.PublishingServiceMain;
import esg.harvest.query.impl.solr.SolrXmlPars;
import esg.harvest.publish.impl.RecordHelper;


/**
 * Implementation of {@link ThreddsParserStrategy} that produces a single {@link Record} of type "Dataset" for each top-level THREDDS dataset,
 * and one {@link Record} of type "File" for each file nested anywhere in the hierarchy.
 */
@Component
public class ThreddsParserStrategyTopLevelDatasetImpl implements ThreddsParserStrategy {
	
	private final Log LOG = LogFactory.getLog(this.getClass());
	
	/**
	 * Default URL builder
	 */
	private ThreddsDataseUrlBuilder urlBuilder = new ThreddsDatasetUrlBuilderCatalogUrlImpl();
		
	public ThreddsParserStrategyTopLevelDatasetImpl() {
		LOG.debug("CONSTRUCTOR: ThreddsParserStrategyTopLevelDatasetImpl");
		
	}
	
	/**
	 * Method to set the builder for the URL to be associated with each record
	 * (overriding the default strategy).
	 * 
	 * @param urlBuilder
	 */
	@Autowired
	public void setUrlBuilder(final @Qualifier("threddsDatasetUrlBuilderCatalogViewImpl") ThreddsDataseUrlBuilder urlBuilder) {
		this.urlBuilder = urlBuilder;
	}
	
	
	/**
	 * Method to parse the catalog top-level dataset.
	 */
	public List<Record> parseDataset(final InvDataset dataset) {
		LOG.debug("In ThreddsParserStrategyTopLevelDatasetImpl parseDataset");
		
	    if (LOG.isDebugEnabled()) LOG.debug("Parsing dataset: "+dataset.getID());
	    
		final List<Record> records = new ArrayList<Record>();
		
		// <dataset name="...." ID="..." restrictAccess="...">
		final String id = dataset.getID();
		Assert.notNull(id,"Dataset ID cannot be null");
		final Record record = new RecordImpl(id);
		final String name = dataset.getName();
		Assert.notNull(name, "Dataset name cannot be null");
		record.addField(SolrXmlPars.FIELD_TITLE, name);
	    // type
        record.addField(SolrXmlPars.FIELD_TYPE, SolrXmlPars.TYPE_DATASET);
        
		// IMPORTANT: add top-level dataset as first record in the list
		records.add(record);
        
		// catalog URL
		record.addField(SolrXmlPars.FIELD_URL, urlBuilder.buildUrl(dataset));
		
		// FIXME
		// metadata format
		record.addField(SolrXmlPars.FIELD_METADATA_FORMAT, "THREDDS");		
		// metadata file name
		record.addField(SolrXmlPars.FIELD_METADATA_URL, PublishingServiceMain.METADATA_URL);
		
		this.parseDocumentation(dataset, record);
		
		this.parseVariables(dataset, record);
		
		this.parseAccess(dataset, record);
		
		this.parseProperties(dataset, record);

		this.parseMetadataGroup(dataset,record);
		
		// recursion
		// NOTE: currently only files generate new records
		long size = parseSubDatasets(dataset, records);
		record.addField(SolrXmlPars.FIELD_SIZE, Long.toString(size));
		
		// debug
		if (LOG.isDebugEnabled()) {
    		for (final Record rec : records) LOG.debug(rec);
	    }
		
		return records;
	}
	
	
	/**
	 * Method to parse the children of a given dataset and store metadata information in the shared list of records.
	 * The very first record in the list corresponds to the root of the dataset hierarchy.
	 * @param dataset
	 * @param records
	 * @return
	 */
	private long parseSubDatasets(final InvDataset dataset, final List<Record> records) {
		LOG.debug("\tIn ThreddsParserStrategyTopLevelDatasetImpl parseSubdatasets");
		
	    if (LOG.isTraceEnabled()) LOG.trace("Crawling dataset: "+dataset.getID()+" for files");
	    
	    long dataset_size = 0L;
	    for (final InvDataset childDataset : dataset.getDatasets()) {
	        
	        if (StringUtils.hasText( childDataset.findProperty(ThreddsPars.FILE_ID) )) {
	            
	            // parse files into separate records
	            dataset_size += this.parseFile(childDataset, records);

	        } else if (StringUtils.hasText( childDataset.findProperty(ThreddsPars.AGGREGATION_ID) )) {
	            
	            // parse aggregation INTO TOP LEVEL DATASET
	            this.parseAggregation(childDataset, records.get(0) );
	            
	        }
	        
	        // recursion
	        dataset_size += parseSubDatasets(childDataset, records);
	        
	    }
	    
	    return dataset_size;
        
	}
	
	/**
	 * Method to parse the aggregation information into given record ( = top-level dataset)
	 * @param dataset
	 * @param record
	 */
	private void parseAggregation(final InvDataset dataset, final Record record) {
		LOG.debug("\tIn ThreddsParserStrategyTopLevelDatasetImpl parseAggregation");
		
	    this.parseAccess(dataset, record);
	    
	}
	
	
	/**
	 * Specific method to parse file information (into a new separate record)
	 * @param dataset
	 * @param records
	 * @return
	 */
	private long parseFile(final InvDataset file, final List<Record> records) {
		LOG.debug("In ThreddsParserStrategyTopLevelDatasetImpl parseFile");
		
	    // <dataset name="hus_AQUA_AIRS_L3_RetStd-v5_200209-201006.nc" 
        //          ID="obs4cmip5.NASA-JPL.AQUA.AIRS.mon.v1.hus_AQUA_AIRS_L3_RetStd-v5_200209-201006.nc" 
        //          urlPath="esg_dataroot/obs4cmip5/observations/atmos/hus/mon/grid/NASA-JPL/AQUA/AIRS/r1i1p1/hus_AQUA_AIRS_L3_RetStd-v5_200209-201006.nc" 
        //          restrictAccess="esg-user">

	    final String id = file.getID();
	    Assert.notNull(id,"File ID cannot be null");
	    if (LOG.isTraceEnabled()) LOG.trace("Parsing file id="+id);
        final Record record = new RecordImpl(id);
        // name -> title
        final String name = file.getName();
        Assert.notNull(name, "File name cannot be null");
        record.addField(SolrXmlPars.FIELD_TITLE, name);
        // type
        record.addField(SolrXmlPars.FIELD_TYPE, SolrXmlPars.TYPE_FILE);       
        // parent dataset
        record.addField(SolrXmlPars.FIELD_PARENT_ID, records.get(0).getId());

        long size = 0; // 0 file size by default
        this.parseProperties(file, record);
        // set size if found
        if (StringUtils.hasText( record.getField(SolrXmlPars.FIELD_SIZE)) ) {
            size = Long.parseLong(record.getField(SolrXmlPars.FIELD_SIZE));
        }
        
        this.parseVariables(file, record);
        
        this.parseAccess(file, record);
        
        this.parseDocumentation(file, record);
	    
        // add this record to the list
        records.add(record);
	    return size;
	    
	}
	
	/**
	 * Method to parse all (key,value) pair properties of a dataset into a search record fields.
	 * @param dataset
	 * @param record
	 */
	
	private void parseProperties(final InvDataset dataset, final Record record) {
		LOG.debug("\tIn ThreddsParserStrategyTopLevelDatasetImpl parseProperties");
		
	    // <property name="..." value="..." />
        for (final InvProperty property : dataset.getProperties()) {
            
            if (LOG.isTraceEnabled()) LOG.trace("Property: " + property.getName() + "=" + property.getValue());
            
            if (property.getName().equals(ThreddsPars.DATASET_ID)) {
                // note: override record ID to get rid of version
                // <dataset name="TES Level 3 Monthly Data (NetCDF)" ID="nasa.jpl.tes.monthly.v1" restrictAccess="esg-user">
                // <property name="dataset_id" value="nasa.jpl.tes.monthly" />
                record.setId(property.getValue());
            } else if (property.getName().equals(SolrXmlPars.FIELD_TITLE)) {
                // note: record title already set from dataset name
                record.addField(SolrXmlPars.FIELD_DESCRIPTION, property.getValue());
            } else if (property.getName().equals(ThreddsPars.DATASET_VERSION)) {
                record.addField(SolrXmlPars.FIELD_VERSION, property.getValue());
            } else if (property.getName().equals(ThreddsPars.SIZE)) {
                // FIXME: store THREDDS "size" as "file_size" ?
                record.addField(SolrXmlPars.FIELD_SIZE, property.getValue());
            } else {
                // index all other properties verbatim
                record.addField(property.getName(), property.getValue());
            }
        }
	    
	}
	
	/**
	 * Method to parse the variable information associated with a dataset into the metadata search record
	 * @param dataset
	 * @param record
	 */
	
	private void parseVariables(final InvDataset dataset, final Record record) {
		LOG.debug("\tIn ThreddsParserStrategyTopLevelDatasetImpl parseVariables");
		
	    // <variables vocabulary="CF-1.0">
        //   <variable name="hfss" vocabulary_name="surface_upward_sensible_heat_flux" units="W m-2">Surface Sensible Heat Flux</variable>
        // </variables>
        for (final Variables variables : dataset.getVariables()) {
            final String vocabulary = variables.getVocabulary();
            for (final Variable variable : variables.getVariableList()) {
                record.addField(SolrXmlPars.FIELD_VARIABLE, variable.getName());
                if (vocabulary.equals(ThreddsPars.CF)) record.addField(SolrXmlPars.FIELD_CF_VARIABLE, variable.getDescription());
            }
        }
	    
	}
	
	/**
	 * Method to parse the access information associated with a dataset into the metadata record search.
	 * FIXME: this method is only called for files - for datasets the access information is encoded differently
	 * @param dataset
	 * @param record
	 */
	
	private void parseAccess(final InvDataset dataset, final Record record) {
		LOG.debug("\tIn ThreddsParserStrategyTopLevelDatasetImpl parseAccess");
		
	    // <access urlPath="/ipcc/sresb1/atm/3h/hfss/miroc3_2_hires/run1/hfss_A3_2050.nc" serviceName="GRIDFTPatPCMDI" dataFormat="NetCDF" />
        for (final InvAccess access : dataset.getAccess()) {
            
            if (LOG.isTraceEnabled()) LOG.trace("Dataset="+dataset.getID()+" Service="+access.getService().getName()+" URL="+access.getStandardUri().toString());
           
            // FIXME: remove ?
            if (dataset.getID().indexOf("aggregation")<0) { // FIXME
                record.addField(SolrXmlPars.FIELD_URL, access.getStandardUri().toString());
                record.addField(SolrXmlPars.FIELD_SERVICE_TYPE, access.getService().getServiceType().toString());
            }
            
            // FIXME: or remove ?
            record.addField(SolrXmlPars.FIELD_SERVICE, 
                    RecordHelper.encodeServiceField(access.getService().getServiceType().toString(), 
                                                    access.getService().getDescription(),
                                                    access.getStandardUrlName()));

        }
	    
	}
	
	
	/**
	 * Method to extract metadata information from a thredds dataset
	 * Included in this metadata are the geospatial and temporal info contained
	 * in the xml tags:
	 * <>
	 * 
	 */
	
	private void parseMetadataGroup(final InvDataset dataset, final Record record) {
		LOG.debug("\tIn ThreddsParserStrategyTopLevelDatasetImpl parseMetadataGroup");
		
		
		this.parseGeoSpatialCoverage(dataset,record);
		this.parseTimeCoverage(dataset,record);
		
	}
	
	/**
     * Method to extract documentation information from a dataset into the search metadata record.
     */
	
    private void parseDocumentation(final InvDataset dataset, final Record record) {
    	LOG.debug("\tIn ThreddsParserStrategyTopLevelDatasetImpl parseDocumentation");
		
        // <documentation type="...">.......</documentation>
        for (final InvDocumentation documentation : dataset.getDocumentation()) {
            final String content = documentation.getInlineContent();
            if (StringUtils.hasText(content)) {
                record.addField(SolrXmlPars.FIELD_DESCRIPTION, content);
            }
        }
        
    }
	
	/**
	 * Helper method to extract Geospatial metadata from a thredds dataset
	 * <geospatialCoverage zpositive="down">
	 *		<northsouth>
	 *			<start>36.6058</start>
	 *			<size>0.0</size>
	 *			<units>degrees_north</units>
	 *		</northsouth>
	 *		<eastwest>
	 *			<start>-97.4888</start>
	 *			<size>0.0</size>
	 *			<units>degrees_west</units>
	 *		</eastwest>
	 *		<updown>
	 *			<start>314.0</start>
	 *			<size>0.0</size>
	 *			<units>m</units>
	 *		</updown>
	 *	</geospatialCoverage>
	 * 
	 */
	
	private void parseGeoSpatialCoverage(final InvDataset dataset, final Record record) {
		LOG.debug("\tIn ThreddsParserStrategyTopLevelDatasetImpl parseGeoSpatialCoverage");
		
		final GeospatialCoverage gsc = dataset.getGeospatialCoverage();
		
		if (gsc!=null) {
		    
			record.addField(SolrXmlPars.FIELD_SOUTH, Double.toString(gsc.getNorthSouthRange().getStart()));
		
			record.addField(SolrXmlPars.FIELD_NORTH, Double.toString(gsc.getNorthSouthRange().getStart()+gsc.getNorthSouthRange().getSize()));
			
			record.addField(SolrXmlPars.FIELD_WEST, Double.toString(gsc.getEastWestRange().getStart()));
			
			record.addField(SolrXmlPars.FIELD_EAST, Double.toString(gsc.getEastWestRange().getStart()+gsc.getEastWestRange().getSize()));
			
		}
	}
	
	
	/**
	 * Helper method to extract temporal metadata from a thredds dataset
	 * Note: not all representations are covered, just the following
	 * <timeCoverage zpositive="down">
	 *		<start>1999-11-16T12:00</start>
	 *		<end>2009-11-16T12:00</end>
	 *	</timeCoverage>
	 * 
	 */
	
	private void parseTimeCoverage(final InvDataset dataset,Record record) {
		LOG.debug("\tIn ThreddsParserStrategyTopLevelDatasetImpl parseTimeCoverage");
		
		final DateRange daterange = dataset.getTimeCoverage();
		
		if (daterange!=null) {
			record.addField(SolrXmlPars.FIELD_DATETIME_START, daterange.getStart().toDateTimeStringISO());	
			record.addField(SolrXmlPars.FIELD_DATETIME_STOP, daterange.getEnd().toDateTimeStringISO());
		}
		
	}
	
}
