package com.aibot.dao;


import org.apache.solr.client.solrj.impl.HttpSolrClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SolrServerFactory
{
    Map <String, HttpSolrClient> urlToServer = new ConcurrentHashMap <String, HttpSolrClient> ();
    static SolrServerFactory instance = new SolrServerFactory ();

    public static SolrServerFactory getInstance()
    {
        return instance;
    }

    private SolrServerFactory ()
    {
    }

    public HttpSolrClient createServer (String solrURL)
    {
        if (urlToServer.containsKey(solrURL))
            return urlToServer.get(solrURL);

        /*
        HttpSolrServer is thread-safe and if you are using the following constructor,
        you *MUST* re-use the same instance for all requests.  If instances are created on
        the fly, it can cause a connection leak. The recommended practice is to keep a
        static instance of HttpSolrServer per solr server url and share it for all requests.
        See https://issues.apache.org/jira/browse/SOLR-861 for more details
      */
        HttpSolrClient server = new HttpSolrClient( solrURL );
        urlToServer.put(solrURL, server);
        return server;
    }
}
