package de.medizininformatik_initiative.feasibility_dsf_process.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Objects;

import static dev.dsf.fhir.authorization.read.ReadAccessHelper.READ_ACCESS_TAG_SYSTEM;

public class StoreFeasibilityResources extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(StoreFeasibilityResources.class);
    private static final String CQL_QUERY_CONTENT_TYPE = "text/cql";

    private final IGenericClient storeClient;

    public StoreFeasibilityResources(IGenericClient storeClient, ProcessPluginApi api) {
        super(api);

        this.storeClient = storeClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        Objects.requireNonNull(storeClient, "storeClient");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        Measure measure = (Measure) variables.getResource(ConstantsFeasibility.VARIABLE_MEASURE);
        Library library = (Library) variables.getResource(ConstantsFeasibility.VARIABLE_LIBRARY);

        var cleanedLibrary = stripReadAccessInformation(stripNonCqlAttachments(library));
        var libraryRes = storeLibraryResource(cleanedLibrary);
        var measureRes = storeMeasureResource(stripReadAccessInformation(measure), libraryRes.getId());

        variables.setString(ConstantsFeasibility.VARIABLE_MEASURE_ID, measureRes.getId().getIdPart());
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
