package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;
import static java.net.http.HttpResponse.BodyHandlers.ofString;

/**
 * Client for communicating with a Flare instance.
 */
public class FlareWebserviceClientImpl implements FlareWebserviceClient {

    private final HttpClient httpClient;
    private final URI flareBaseUrl;

    public FlareWebserviceClientImpl(HttpClient httpClient, URI flareBaseUrl) {
        this.httpClient = httpClient;
        this.flareBaseUrl = flareBaseUrl;
    }

    @Override
    public int requestFeasibility(byte[] structuredQuery) throws IOException, InterruptedException {
        var req = HttpRequest.newBuilder()
                .POST(ofByteArray(structuredQuery))
                .setHeader("Content-Type", "application/sq+json")
                .uri(flareBaseUrl.resolve("/query/execute"))
                .build();

        var res = httpClient.send(req, ofString());
        return Integer.parseInt(res.body());
    }
}
