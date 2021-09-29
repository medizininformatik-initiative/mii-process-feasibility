package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.fhir.client.FhirWebserviceClient;
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

    @Override
    public String getLocalBaseUrl() {
        return this.fhirClientProvider.getLocalBaseUrl();
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
