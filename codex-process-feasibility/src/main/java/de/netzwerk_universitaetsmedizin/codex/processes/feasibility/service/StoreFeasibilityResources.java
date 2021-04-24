package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Objects;

public class StoreFeasibilityResources extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(StoreFeasibilityResources.class);
    private static final String CQL_QUERY_CONTENT_TYPE = "text/cql";

    private final IGenericClient storeClient;

    public StoreFeasibilityResources(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                     IGenericClient storeClient) {
        super(clientProvider, taskHelper);

        this.storeClient = storeClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        Objects.requireNonNull(storeClient, "storeClient");
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        Measure measure = (Measure) execution.getVariable(ConstantsFeasibility.VARIABLE_MEASURE);
        Library library = (Library) execution.getVariable(ConstantsFeasibility.VARIABLE_LIBRARY);

        Bundle transactionResponse = storeResources(measure, stripNonCqlAttachments(library));

        execution.setVariable(ConstantsFeasibility.VARIABLE_MEASURE_ID, extractMeasureId(transactionResponse));
    }

    private Library stripNonCqlAttachments(Library library) {
        var cqlAttachment = library.getContent()
                .stream()
                .filter(a -> a.getContentType().equalsIgnoreCase(CQL_QUERY_CONTENT_TYPE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("query is missing content of type " + CQL_QUERY_CONTENT_TYPE));

        return library.setContent(List.of(cqlAttachment));
    }

    private Bundle storeResources(Measure measure, Library library) {
        logger.info("Store Measure `{}` and Library `{}`", measure.getId(), library.getUrl());

        Bundle bundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
        bundle.addEntry().setResource(measure).getRequest()
                .setMethod(Bundle.HTTPVerb.POST).setUrl("Measure");
        bundle.addEntry().setResource(library).getRequest()
                .setMethod(Bundle.HTTPVerb.POST).setUrl("Library");
        return storeClient.transaction().withBundle(bundle).execute();
    }

    private String extractMeasureId(Bundle transactionResponse) {
        return new IdType(transactionResponse.getEntryFirstRep().getResponse().getLocation()).getIdPart();
    }
}
