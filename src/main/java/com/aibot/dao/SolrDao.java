package com.aibot.dao;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.StringJoiner;

public class SolrDao <T>{

    SolrClient server = null;

    public SolrDao (String solrURL)
    {
        server = SolrServerFactory.getInstance().createServer(solrURL);
        configureSolr (server);
    }

    /*
    * put a single POJO document into solr
    * */
    public void put (T dao)
    {
        put (createSingletonSet (dao));
    }

    /*
    * put a collection of POJO documents into solr
    * */
    public void put (Collection<T> dao)
    {
        try
        {
            long startTime = System.currentTimeMillis();
            UpdateResponse rsp = server.addBeans (dao);
            System.out.print ("Added documents to solr. Time taken = " + rsp.getElapsedTime() + ". " + rsp.toString());
            long endTime = System.currentTimeMillis();
            System.out.println (" , time-taken=" + ((double)(endTime-startTime))/1000.00 + " seconds");
            server.commit();
        }
        catch (SolrServerException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /*
    * put a single SolrInputDocument into solr
    * */
    public void putDoc (SolrInputDocument doc)
    {
        putDoc (createSingletonSet(doc));
    }

    /*
    * put a collection of SolrInputDocuments into solr
    * */
    public void putDoc (Collection<SolrInputDocument> docs)
    {
        try
        {
            long startTime = System.currentTimeMillis();
            UpdateRequest req = new UpdateRequest();
            req.setAction( UpdateRequest.ACTION.COMMIT, false, false );
            req.add (docs);
            UpdateResponse rsp = req.process( server );
            System.out.print ("Added documents to solr. Time taken = " + rsp.getElapsedTime() + ". " + rsp.toString());
            long endTime = System.currentTimeMillis();
            System.out.println (" , time-taken=" + ((double)(endTime-startTime))/1000.00 + " seconds");
        }
        catch (SolrServerException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    /*
    * search using a given Solr query and return the query response
    * */
    public QueryResponse search(SolrQuery squery){
        QueryResponse rsp = null;
        try
        {
            rsp = server.query( squery, SolrRequest.METHOD.POST );
        }
        catch (SolrServerException e)
        {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
        return rsp;
    }

    /*
    * search using a given query and return result solr documents
    * */
    public SolrDocumentList searchDoc(SolrQuery squery)
    {
        QueryResponse rsp = null;
        try {
            rsp = server.query( squery, SolrRequest.METHOD.POST );
        } catch (SolrServerException e)
        {
            e.printStackTrace();
            return null;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
        SolrDocumentList docs = rsp.getResults();
        return docs;
    }

    public UpdateResponse deteleAll(){
        SolrQuery query = new SolrQuery();
        query.setQuery( "*:*" );
        //query.addSortField( "price", SolrQuery.ORDER.asc );
        UpdateResponse rsp = null;
        try
        {
            rsp = server.deleteByQuery("*:*", 5);
        }catch (SolrServerException e)
        {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
        return rsp;
    }

    /*
    * utility function for reading all and get QueryResponses
    * */
    public QueryResponse readAll ()
    {
        SolrQuery query = new SolrQuery();
        query.setQuery( "*:*" );
        //query.addSortField( "price", SolrQuery.ORDER.asc );
        QueryResponse rsp = null;
        try
        {
            rsp = server.query( query );
        }catch (SolrServerException e)
        {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
        return rsp;
    }
    /*
    * utility function for reading all and get all results documents
    * */
    public SolrDocumentList readAllDocs ()
    {
        SolrQuery query = new SolrQuery();
        query.setQuery( "*:*" );
        //query.addSortField( "price", SolrQuery.ORDER.asc );
        QueryResponse rsp = null;
        try {
            rsp = server.query( query );
        } catch (SolrServerException e)
        {
            e.printStackTrace();
            return null;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
        SolrDocumentList docs = rsp.getResults();
        return docs;
    }

    /*
    * Configure solr server
    * */
    private void configureSolr(SolrClient server)
    {
        // configure solrj 4.1.0. (no need for solrj5.5.0)
//        server.setMaxRetries(1); // defaults to 0.  > 1 not recommended.
//        server.setConnectionTimeout(5000); // 5 seconds to establish TCP
//        // The following settings are provided here for completeness.
//        // They will not normally be required, and should only be used
//        // after consulting javadocs to know whether they are truly required.
//        server.setSoTimeout(1000);  // socket read timeout
//        server.setDefaultMaxConnectionsPerHost(100);
//        server.setMaxTotalConnections(100);
//        server.setFollowRedirects(false);  // defaults to false
//        // allowCompression defaults to false.
//        // Server side must support gzip or deflate for this to have any effect.
//        server.setAllowCompression(false);
    }

    private <U> Collection<U> createSingletonSet(U dao)
    {
        if (dao == null)
            return Collections.emptySet();
        return Collections.singleton(dao);
    }

    public void setHighlightQuery(SolrQuery solrQuery, int noOfSnippets, String highlightField){
        if(noOfSnippets <= 0){
            System.out.println("Invalid no of Snippets. We use 1 for default.");
            noOfSnippets = 1;
        }
        if(solrQuery != null){
            solrQuery.setHighlight(true);
            solrQuery.setHighlightSnippets(noOfSnippets);
            solrQuery.setParam("hl.fl", highlightField);
        }
    }

    public void setDistanceFilterQuery(SolrQuery solrQuery, String field, double lat, double lon, double distanceInKM){
        if(distanceInKM <= 0){
            System.out.println("Invalid unit is given! We use 10 by default.");
            distanceInKM = 10;
        }
        if(solrQuery != null) {
            //example: fq={!geofilt%20pt=1.28488256067379,103.843899287360%20sfield=geo%20d=100.0}
            StringJoiner sj = new StringJoiner(" ", "{!geofilt ", "}");
            sj.add("pt=" + String.valueOf(lat) + "," + String.valueOf(lon));
            sj.add("sfield=" + field);
            sj.add("d=" + String.valueOf(distanceInKM));
            solrQuery.addFilterQuery(sj.toString());
            System.out.println("====Filter query is:" + sj.toString());
        }
    }
}
