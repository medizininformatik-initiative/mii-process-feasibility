package de.medizininformatik_initiative.feasibility_dsf_process;

import de.medizininformatik_initiative.feasibility_dsf_process.EnhancedFhirWebserviceClientProviderImpl;
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
    public void testGetWebserviceClientByReference_Local() {
        IdType idType = new IdType("Something/id-123456");
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(client);

        FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClientByReference(idType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetWebserviceClientByReference_ReferenceUrlEqualsLocalBaseUrl() {
        IdType localIdType = new IdType(FULL_URL);

        when(clientProvider.getLocalBaseUrl())
                .thenReturn(BASE_URL);
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(client);

        final FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClientByReference(localIdType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetWebserviceClientByReference_Remote() {
        IdType idType = new IdType("http://remote.host/Something/id-123456");
        when(clientProvider.getWebserviceClient("http://remote.host"))
                .thenReturn(client);

        FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClientByReference(idType);

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
    public void testGetWebserviceClient() {
        when(clientProvider.getWebserviceClient(FULL_URL))
                .thenReturn(client);
        FhirWebserviceClient webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClient(FULL_URL);

        assertSame(client, webserviceClient);
    }
}
