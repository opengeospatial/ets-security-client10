package org.opengis.cite.securityclient10.httpServer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public class TestServerTest {

    @Test
    public void testTestServer()
                            throws Exception {
        URL serverUrl = new URL( "http://localhost:9999/teamengine/ABGTJ" );
        TestServer testServer = new TestServer( serverUrl, null, null );
        ServerOptions serverOptions = new ServerOptions( "wms111" );
        testServer.registerHandler( "ABGTJ", serverOptions );

        HttpClient httpClient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet( "http://localhost:9999/teamengine/ABGTJ?request=GetCapabilities" );
        HttpResponse response = httpClient.execute( httpGet );

        assertThat( response.getStatusLine().getStatusCode(), is( 200 ) );

    }

}
