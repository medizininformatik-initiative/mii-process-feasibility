package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;

public interface EnhancedFhirWebserviceClientProvider extends FhirWebserviceClientProvider {
    FhirWebserviceClient getWebserviceClient(IdType reference);
}
