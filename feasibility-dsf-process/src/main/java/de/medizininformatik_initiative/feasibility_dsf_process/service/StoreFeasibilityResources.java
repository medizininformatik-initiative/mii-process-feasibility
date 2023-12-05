package de.medizininformatik_initiative.feasibility_dsf_process.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.*;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;

public class StoreFeasibilityResources extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(StoreFeasibilityResources.class);

    private final IGenericClient storeClient;
    private final FeasibilityResourceCleaner cleaner;

    public StoreFeasibilityResources(IGenericClient storeClient, ProcessPluginApi api, FeasibilityResourceCleaner cleaner) {
        super(api);

        this.storeClient = storeClient;
        this.cleaner = cleaner;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        Objects.requireNonNull(storeClient, "storeClient");
        Objects.requireNonNull(cleaner, "cleaner");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        Measure measure = variables.getResource(VARIABLE_MEASURE);
        Library library = variables.getResource(VARIABLE_LIBRARY);

        cleaner.cleanLibrary(library);
        cleaner.cleanMeasure(measure);

        var transactionResponse = storeResources(measure, library);

        variables.setString(VARIABLE_MEASURE_ID, extractMeasureId(transactionResponse));
    }

    private Bundle storeResources(Measure measure, Library library) {
        logger.info("Store Measure `{}` and Library `{}`", measure.getId(), library.getUrl());

        Bundle bundle = new Bundle().setType(TRANSACTION);
        bundle.addEntry().setResource(measure).getRequest().setMethod(POST).setUrl("Measure");
        bundle.addEntry().setResource(library).getRequest().setMethod(POST).setUrl("Library");
        return storeClient.transaction().withBundle(bundle).execute();
    }

    private String extractMeasureId(Bundle transactionResponse) {
        return new IdType(transactionResponse.getEntryFirstRep().getResponse().getLocation()).getIdPart();
    }
}
