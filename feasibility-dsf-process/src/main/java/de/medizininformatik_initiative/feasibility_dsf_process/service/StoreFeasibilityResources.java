package de.medizininformatik_initiative.feasibility_dsf_process.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Objects;

import static org.highmed.dsf.fhir.authorization.read.ReadAccessHelper.READ_ACCESS_TAG_SYSTEM;

public class StoreFeasibilityResources extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(StoreFeasibilityResources.class);
    private static final String CQL_QUERY_CONTENT_TYPE = "text/cql";

    private final IGenericClient storeClient;

    public StoreFeasibilityResources(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                     ReadAccessHelper readAccessHelper, IGenericClient storeClient) {
        super(clientProvider, taskHelper, readAccessHelper);

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

        var cleanedLibrary = stripReadAccessInformation(stripNonCqlAttachments(library));
        var libraryRes = storeLibraryResource(cleanedLibrary);
        var measureRes = storeMeasureResource(stripReadAccessInformation(measure), libraryRes.getId());

        execution.setVariable(ConstantsFeasibility.VARIABLE_MEASURE_ID, measureRes.getId().getIdPart());
    }

    private <T extends Resource> T stripReadAccessInformation(T resource) {
        resource.getMeta().getTag().removeIf(t -> READ_ACCESS_TAG_SYSTEM.equals(t.getSystem()));
        return resource;
    }

    private Library stripNonCqlAttachments(Library library) {
        var cqlAttachment = library.getContent()
                .stream()
                .filter(a -> a.getContentType().equalsIgnoreCase(CQL_QUERY_CONTENT_TYPE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("query is missing content of type " + CQL_QUERY_CONTENT_TYPE));

        return library.setContent(List.of(cqlAttachment));
    }

    private MethodOutcome storeLibraryResource(Library library) {
        logger.info("Store Library `{}`", library.getId());
        return storeClient.create().resource(library).execute();
    }

    private MethodOutcome storeMeasureResource(Measure measure, IIdType libraryId) {
        logger.info("Store Measure `{}`", measure.getId());
        measure.getLibrary().clear();
        measure.addLibrary("Library/" + libraryId.getIdPart());
        return storeClient.create().resource(measure).execute();
    }
}
