package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;

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
        logger.info("doExecute store feasibility resources");

        Measure measure = variables.getResource(VARIABLE_MEASURE);
        Library library = variables.getResource(VARIABLE_LIBRARY);

        cleaner.cleanLibrary(library);
        cleaner.cleanMeasure(measure);

        var libraryRes = storeLibraryResource(library);
        var measureRes = storeMeasureResource(measure, libraryRes.getId());

        variables.setString(ConstantsFeasibility.VARIABLE_MEASURE_ID, measureRes.getId().getIdPart());
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
