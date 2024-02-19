package de.medizininformatik_initiative.process.feasibility;

import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import dev.dsf.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EnhancedFhirWebserviceClientProviderImplTest {

    @Mock private FhirWebserviceClient client;
    @Mock private FhirWebserviceClientProvider clientProvider;

    @InjectMocks private EnhancedFhirWebserviceClientProviderImpl enhancedFhirWebserviceClientProvider;

    private static final String BASE_URL = "http://localhost";
    private static final String PATH = "Something/id-123456";
    private static final String FULL_URL = BASE_URL + "/" + PATH;

    @Test
    public void testGetWebserviceClientByReference_Local() {
        var idType = new IdType("Something/id-123456");
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(client);

        var webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClientByReference(idType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetWebserviceClientByReference_ReferenceUrlEqualsLocalBaseUrl() {
        var localIdType = new IdType(FULL_URL);

        when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
        when(client.getBaseUrl()).thenReturn(BASE_URL);

        final var webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClientByReference(localIdType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetWebserviceClientByReference_Remote() {
        var idType = new IdType("http://remote.host/Something/id-123456");
        when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
        when(clientProvider.getWebserviceClient("http://remote.host"))
                .thenReturn(client);

        var webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClientByReference(idType);

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetLocalBaseUrl() {
        var baseUrl = "http://localhost";
        when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
        when(client.getBaseUrl()).thenReturn(baseUrl);

        var localBaseUrl = enhancedFhirWebserviceClientProvider.getLocalBaseUrl();

        assertEquals(baseUrl, localBaseUrl);
    }

    @Test
    public void testGetLocalWebserviceClient() {
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(client);

        var webserviceClient = enhancedFhirWebserviceClientProvider.getLocalWebserviceClient();

        assertSame(client, webserviceClient);
    }

    @Test
    public void testGetWebserviceClient() {
        when(clientProvider.getWebserviceClient(FULL_URL))
                .thenReturn(client);
        var webserviceClient = enhancedFhirWebserviceClientProvider.getWebserviceClient(FULL_URL);

        assertSame(client, webserviceClient);
    }
}
