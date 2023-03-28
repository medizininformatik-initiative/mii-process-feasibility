package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import javax.net.ssl.SSLSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FlareWebserviceClientImplTest {

    private HttpClient httpClient;
    private FlareWebserviceClient flareWebserviceClient;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        httpClient = mock(HttpClient.class);
        flareWebserviceClient = new FlareWebserviceClientImpl(httpClient, new URI("http://localhost:5000/"));
    }

    @Test
    public void testRequestFeasibility_FailsOnCommunicationError() throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenThrow(IOException.class);

        var structuredQuery = "foo".getBytes();
        assertThrows(IOException.class, () -> flareWebserviceClient.requestFeasibility(structuredQuery));
    }

    @Test
    public void testRequestFeasibility_FailsOnWrongBodyContent() throws IOException, InterruptedException {
        var response = new StringHttpResponse("{\"invalid\": true}");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(response);

        var structuredQuery = "foo".getBytes();
        assertThrows(NumberFormatException.class, () -> flareWebserviceClient.requestFeasibility(structuredQuery));
    }

    @Test
    public void testRequestFeasibility() throws IOException, InterruptedException {
        var response = new StringHttpResponse("15");
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
                .thenReturn(response);

        var structuredQuery = "foo".getBytes();
        var feasibility = flareWebserviceClient.requestFeasibility(structuredQuery);

        assertEquals(15, feasibility);
    }


    private class StringHttpResponse implements HttpResponse<String> {

        private final String body;

        public StringHttpResponse(String body) {
            this.body = body;
        }

        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return null;
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return null;
        }
    }
}
