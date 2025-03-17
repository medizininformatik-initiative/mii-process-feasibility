package de.medizininformatik_initiative.process.feasibility.client.flare;

import de.medizininformatik_initiative.process.feasibility.EvaluationSettingsProviderImpl;
import de.medizininformatik_initiative.process.feasibility.EvaluationStrategy;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import static jakarta.ws.rs.HttpMethod.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FlareWebserviceClientImplTest {

    private HttpClient httpClient;
    private URI flareBaseUrl;
    private FlareWebserviceClient flareWebserviceClient;
    @Captor ArgumentCaptor<HttpPost> postCaptor;

    @BeforeEach
    public void setUp() throws URISyntaxException {
        httpClient = mock(HttpClient.class);
        flareBaseUrl = new URI("http://localhost:5000/");
        flareWebserviceClient = new FlareWebserviceClientImpl(httpClient, flareBaseUrl);
    }

    @Test
    public void failsOnCommunicationError() throws IOException, InterruptedException {
        when(httpClient.execute(any(HttpPost.class), any(BasicResponseHandler.class)))
                .thenThrow(IOException.class);

        var structuredQuery = "foo".getBytes();
        assertThatThrownBy(() -> flareWebserviceClient.requestFeasibility(structuredQuery))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void failsOnWrongBodyContent() throws IOException, InterruptedException {
        var response = "{\"invalid\": true}";
        when(httpClient.execute(any(HttpPost.class), any(BasicResponseHandler.class)))
                .thenReturn(response);

        var structuredQuery = "foo".getBytes();
        assertThatThrownBy(() -> flareWebserviceClient.requestFeasibility(structuredQuery))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(response);
    }

    @Test
    public void requestFeasibility() throws IOException, InterruptedException {
        var response = "15";
        when(httpClient.execute(any(HttpPost.class), any(BasicResponseHandler.class)))
                .thenReturn(response);

        var structuredQuery = "foo".getBytes();
        var feasibility = flareWebserviceClient.requestFeasibility(structuredQuery);

        assertThat(feasibility).isEqualTo(15);
    }

    @Test
    public void whitespaceInResponseIsIgnored() throws IOException, InterruptedException {
        var response = " \t  \n 15  \n  ";
        when(httpClient.execute(any(HttpPost.class), any(BasicResponseHandler.class)))
                .thenReturn(response);

        var structuredQuery = "foo".getBytes();
        var feasibility = flareWebserviceClient.requestFeasibility(structuredQuery);

        assertThat(feasibility).isEqualTo(15);
    }

    @Test
    public void baseUrlPathIsKept() throws Exception {
        var path = "/foo/bar/";
        flareWebserviceClient = new FlareWebserviceClientImpl(httpClient, URI.create("http://foo.bar:1234" + path));
        var structuredQuery = "foo".getBytes();

        when(httpClient.execute(postCaptor.capture(), any(BasicResponseHandler.class)))
                .thenReturn("99");

        flareWebserviceClient.requestFeasibility(structuredQuery);

        assertThat(postCaptor.getValue().getURI().getPath()).isEqualTo(path + "query/execute");
    }

    @Test
    void nullBaseUrlDoesNotFailAtInit() throws Exception {
        var config = new FlareWebserviceClientSpringConfig();
        assertThatNoException().isThrownBy(() -> {
            config.flareWebserviceClient(httpClient, new EvaluationSettingsProviderImpl(
                    EvaluationStrategy.STRUCTURED_QUERY, true, 0d, 0d, 0, Duration.ofMillis(1)));
        });
    }

    @Test
    void nullBaseUrlFails() throws Exception {
        var config = new FlareWebserviceClientSpringConfig();
        var structuredQuery = "foo".getBytes();
        flareWebserviceClient = config.flareWebserviceClient(httpClient, new EvaluationSettingsProviderImpl(
                EvaluationStrategy.STRUCTURED_QUERY, true, 0d, 0d, 0, Duration.ofMillis(1)));

        assertThatThrownBy(() -> flareWebserviceClient.requestFeasibility(structuredQuery))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FLARE_BASE_URL is not set.");
    }

    @Test
    void illegalBaseUrlFails() throws Exception {
        var config = new FlareWebserviceClientSpringConfig();
        var invalidUrl = "{ßöäü;";
        var structuredQuery = "foo".getBytes();
        ReflectionTestUtils.setField(config, "flareBaseUrl", invalidUrl);
        flareWebserviceClient = config.flareWebserviceClient(httpClient, new EvaluationSettingsProviderImpl(
                EvaluationStrategy.STRUCTURED_QUERY, true, 0d, 0d, 0, Duration.ofMillis(1)));

        assertThatThrownBy(() -> flareWebserviceClient.requestFeasibility(structuredQuery))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Could not parse FLARE_BASE_URL '" + invalidUrl + "' as URI.");
    }

    @Test
    void otherEvaluationStrategyFails() throws Exception {
        var config = new FlareWebserviceClientSpringConfig();
        var invalidUrl = "{ßöäü;";
        var structuredQuery = "foo".getBytes();
        ReflectionTestUtils.setField(config, "flareBaseUrl", invalidUrl);
        flareWebserviceClient = config.flareWebserviceClient(httpClient, new EvaluationSettingsProviderImpl(
                EvaluationStrategy.CQL, true, 0d, 0d, 0, Duration.ofMillis(1)));

        assertThatThrownBy(() -> flareWebserviceClient.requestFeasibility(structuredQuery))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EVALUATION_STRATEGY is not set to 'structured-query'.");
    }

    @Test
    void sendErrorGetsRethrownWithAdditionalInformation() throws Exception {
        var structuredQuery = "foo".getBytes();
        var errorMessage = "error-151930";
        var error = new IOException(errorMessage);
        when(httpClient.execute(any(HttpPost.class), any(BasicResponseHandler.class))).thenThrow(error);

        assertThatThrownBy(() -> flareWebserviceClient.requestFeasibility(structuredQuery))
                .hasMessage("Error sending %s request to flare webservice url '%s': %s", POST,
                        flareBaseUrl.resolve("/query/execute"), errorMessage)
                .hasCause(error);
    }
}
