package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class EnhancedFhirWebserviceClientProviderImplTest {

    @Mock
    private FhirWebserviceClient client;

    @Mock
    private FhirWebserviceClientProvider clientProvider;

    @InjectMocks
    private EnhancedFhirWebserviceClientProviderImpl enhancedFhirWebserviceClientProvider;

    @Test
    public void testGetWebserviceClient_Local() {
        final IdType idType = new IdType("Something/id-123456");
        when(clientProvider.getLocalWebserviceClient()).thenReturn(client);

        final FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClient(idType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetWebserviceClient_Remote() {
        final IdType idType = new IdType("http://remote.host/Something/id-123456");
        when(clientProvider.getRemoteWebserviceClient("http://remote.host")).thenReturn(client);

        final FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClient(idType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetLocalBaseUrl() {
        enhancedFhirWebserviceClientProvider.getLocalBaseUrl();
        verify(clientProvider).getLocalBaseUrl();
    }

    @Test
    public void testGetLocalWebserviceClient() {
        enhancedFhirWebserviceClientProvider.getLocalWebserviceClient();
        verify(clientProvider).getLocalWebserviceClient();
    }

    @Test
    public void testGetRemoteWebserviceClient() {
        final String baseUrl = "http://localhost";
        final String path = "/Something/id-123456";
        final String fullUrl = baseUrl + path;

        final IdType idType = new IdType(fullUrl);
        enhancedFhirWebserviceClientProvider.getRemoteWebserviceClient(idType);
        verify(clientProvider).getRemoteWebserviceClient(idType);

        enhancedFhirWebserviceClientProvider.getRemoteWebserviceClient(baseUrl, path);
        verify(clientProvider).getRemoteWebserviceClient(baseUrl, path);

        enhancedFhirWebserviceClientProvider.getRemoteWebserviceClient(baseUrl);
        verify(clientProvider).getRemoteWebserviceClient(baseUrl);
    }
}
