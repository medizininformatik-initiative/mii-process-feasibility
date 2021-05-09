package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

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
                .setHeader("Content-Type", "codex/json")
                .setHeader("Accept", "internal/json")
                .uri(flareBaseUrl.resolve("query-sync"))
                .build();

        var res = httpClient.send(req, ofString());
        return Integer.parseInt(res.body());
    }
}
