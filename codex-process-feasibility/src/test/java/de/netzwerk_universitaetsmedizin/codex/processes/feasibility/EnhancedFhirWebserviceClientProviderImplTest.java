package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class EnhancedFhirWebserviceClientProviderImplTest {

    @Mock
    private FhirWebserviceClient client;

    @Mock
    private FhirWebserviceClientProvider clientProvider;

    @InjectMocks
    private EnhancedFhirWebserviceClientProviderImpl enhancedFhirWebserviceClientProvider;

    private static final String BASE_URL = "http://localhost";
    private static final String PATH = "Something/id-123456";
    private static final String FULL_URL = BASE_URL + "/" + PATH;

    @Test
    public void testGetWebserviceClient_Local() {
        IdType idType = new IdType("Something/id-123456");
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(client);

        FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClient(idType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetWebserviceClient_ReferenceUrlEqualsLocalBaseUrl() {
        IdType localIdType = new IdType(FULL_URL);

        when(clientProvider.getLocalBaseUrl())
                .thenReturn(BASE_URL);
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(client);

        final FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClient(localIdType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetWebserviceClient_Remote() {
        IdType idType = new IdType("http://remote.host/Something/id-123456");
        when(clientProvider.getRemoteWebserviceClient("http://remote.host"))
                .thenReturn(client);

        FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClient(idType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetLocalBaseUrl() {
        String baseUrl = "http://localhost";
        when(clientProvider.getLocalBaseUrl())
                .thenReturn(baseUrl);

        String localBaseUrl = enhancedFhirWebserviceClientProvider.getLocalBaseUrl();

        assertEquals(baseUrl, localBaseUrl);
    }

    @Test
    public void testGetLocalWebserviceClient() {
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(client);

        FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getLocalWebserviceClient();

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetRemoteWebserviceClient_Reference() {
        IdType idType = new IdType(FULL_URL);

        when(clientProvider.getRemoteWebserviceClient(idType))
                .thenReturn(client);
        FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getRemoteWebserviceClient(idType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetRemoteWebserviceClient_Url() {
        when(clientProvider.getRemoteWebserviceClient(FULL_URL))
                .thenReturn(client);

        FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getRemoteWebserviceClient(FULL_URL);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetRemoteWebserviceClient_BaseUrlAndPath() {
        when(clientProvider.getRemoteWebserviceClient(BASE_URL, PATH))
                .thenReturn(client);

        FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getRemoteWebserviceClient(BASE_URL,
                PATH);

        assertSame(client, webserviceClient);
    }
}
