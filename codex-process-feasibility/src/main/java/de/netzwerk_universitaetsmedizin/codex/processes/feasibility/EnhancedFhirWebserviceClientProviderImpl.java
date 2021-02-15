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
    public FhirWebserviceClient getWebserviceClient(IdType reference) {
        if (reference.getBaseUrl() == null || reference.getBaseUrl().equals(getLocalBaseUrl())) {
            return getLocalWebserviceClient();
        } else {
            return getRemoteWebserviceClient(reference.getBaseUrl());
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
    public FhirWebserviceClient getRemoteWebserviceClient(IdType idType) {
        return this.fhirClientProvider.getRemoteWebserviceClient(idType);
    }

    @Override
    public FhirWebserviceClient getRemoteWebserviceClient(String s, String s1) {
        return this.fhirClientProvider.getRemoteWebserviceClient(s, s1);
    }

    @Override
    public FhirWebserviceClient getRemoteWebserviceClient(String s) {
        return this.fhirClientProvider.getRemoteWebserviceClient(s);
    }
}
