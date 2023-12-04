package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.net.URI;

import static ca.uhn.fhir.rest.api.Constants.HEADER_CONTENT_TYPE;

/**
 * Client for communicating with a Flare instance.
 */
public class FlareWebserviceClientImpl implements FlareWebserviceClient {

    private final org.apache.http.client.HttpClient httpClient;
    private final URI flareBaseUrl;

    public FlareWebserviceClientImpl(HttpClient httpClient, URI flareBaseUrl) {
        this.httpClient = httpClient;
        this.flareBaseUrl = flareBaseUrl;
    }

    @Override
    public int requestFeasibility(byte[] structuredQuery) throws IOException, InterruptedException {
        var req = new HttpPost(resolve("/query/execute"));
        req.setEntity(new ByteArrayEntity(structuredQuery));
        req.setHeader(new BasicHeader(HEADER_CONTENT_TYPE, "application/sq+json"));

        var response = httpClient.execute(req, new BasicResponseHandler());

        return Integer.parseInt(response);
    }

    private URI resolve(String path) {
        return flareBaseUrl.resolve((flareBaseUrl.getPath() + path).replaceAll("//", "/"));
    }

    @Override
    public void testConnection() throws ClientProtocolException, IOException {
        var req = new HttpGet(resolve("/cache/stats"));

        httpClient.execute(req, new BasicResponseHandler());
    }
}
