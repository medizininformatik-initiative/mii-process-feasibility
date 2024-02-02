package de.medizininformatik_initiative.process.feasibility;

import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import dev.dsf.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;

public class EnhancedFhirWebserviceClientProviderImpl implements EnhancedFhirWebserviceClientProvider {

    private final FhirWebserviceClientProvider fhirClientProvider;

    public EnhancedFhirWebserviceClientProviderImpl(FhirWebserviceClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    @Override
    public FhirWebserviceClient getWebserviceClientByReference(IdType reference) {
        if (reference.getBaseUrl() == null || reference.getBaseUrl().equals(getLocalBaseUrl())) {
            return getLocalWebserviceClient();
        } else {
            return getWebserviceClient(reference.getBaseUrl());
        }
    }

    public String getLocalBaseUrl() {
        return this.fhirClientProvider.getLocalWebserviceClient().getBaseUrl();
    }

    @Override
    public FhirWebserviceClient getLocalWebserviceClient() {
        return this.fhirClientProvider.getLocalWebserviceClient();
    }

    @Override
    public FhirWebserviceClient getWebserviceClient(String webserviceUrl) {
        return this.fhirClientProvider.getWebserviceClient(webserviceUrl);
    }
}
