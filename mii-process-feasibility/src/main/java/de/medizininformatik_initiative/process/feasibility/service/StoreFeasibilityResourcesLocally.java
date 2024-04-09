package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.client.FhirWebserviceClient;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.UUID;

public class StoreFeasibilityResourcesLocally extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(StoreFeasibilityResourcesLocally.class);

    private FhirWebserviceClient localWebserviceClient;

    public StoreFeasibilityResourcesLocally(ProcessPluginApi api) {
        super(api);
        this.localWebserviceClient = api.getFhirWebserviceClientProvider().getLocalWebserviceClient();
    }

    @Override
    protected void doExecute(DelegateExecution delegateExecution, Variables variables) {
        logger.info("doExecute store feasibility resource for distribution");
        // Measure and Library from Request
        Measure measureFromRequest = variables.getResource(ConstantsFeasibility.VARIABLE_MEASURE);
        Library libraryFromRequest = variables.getResource(ConstantsFeasibility.VARIABLE_LIBRARY);

        var libraryRes = storeLibraryResource(libraryFromRequest, UUID.randomUUID());
        var measureRes = storeMeasureResource(measureFromRequest, libraryRes.getUrl());

        variables.setString(ConstantsFeasibility.VARIABLE_DISTRIBUTION_MEASURE_ID, measureRes.getId());
        variables.setString(ConstantsFeasibility.VARIABLE_DISTRIBUTION_LIBRARY_ID, libraryRes.getId());
    }

    private Measure storeMeasureResource(Measure measureFromRequest, String libraryUrl) {
        logger.info("Store Measure `{}`", measureFromRequest.getId());
        measureFromRequest.getLibrary().clear();
        return localWebserviceClient.create(measureFromRequest.addLibrary(libraryUrl));
    }

    private Library storeLibraryResource(Library libraryFromRequest, UUID uuid) {
        logger.info("Store Library `{}`", libraryFromRequest.getId());
        return localWebserviceClient.create(libraryFromRequest.setUrl("urn:uuid:" + uuid));
    }

    //   2024-04-08 21:21:43 ERROR pool-2-thread-1 - AbstractServiceDelegate.execute(86) | Process medizininformatik-initiativede_feasibilityExecute:1:10 has fatal error in step storeFeasibilityResourcesLocally:226 for task https://dic-1/fhir/Task/be021b9d-8413-4048-8b7b-d691b13e17c8, reason: jakarta.ws.rs.WebApplicationException - WARNING PROCESSING mea-0: Name should be usable as an identifier for the module by machine processing applications such as code generation [name.matches('[A-Z]([A-Za-z0-9_]){0,254}')]
    //   2024-04-08 21:21:43 ERROR PROCESSING Canonical URLs must be absolute URLs if they are not fragment references (Library/843ad8fd-7fd6-491c-8631-0dda80e54c05)
    // 2024-04-08 21:21:43 INFORMATION PROCESSING The Library Library/843ad8fd-7fd6-491c-8631-0dda80e54c05 could not be resolved, so expression validation may not be correct


}
