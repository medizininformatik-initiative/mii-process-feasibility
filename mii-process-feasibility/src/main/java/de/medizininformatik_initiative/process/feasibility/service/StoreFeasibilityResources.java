package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.process.feasibility.StoreBundleProvider;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;

public class StoreFeasibilityResources extends AbstractServiceDelegate implements InitializingBean, StoreBundleProvider {

    private static final Logger logger = LoggerFactory.getLogger(StoreFeasibilityResources.class);
    private static final Pattern MEASURE_URL_PATTERN = Pattern.compile("(.+)/Measure/(.+)");
    private static final Pattern LIBRARY_URL_PATTERN = Pattern.compile("urn:uuid:(.+)");

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

        fixCanonical(measure, library);

        var transactionResponse = storeResources(measure, library);

        variables.setString(VARIABLE_MEASURE_ID,
                findIdPartInFirstBundleResourceType(transactionResponse, ResourceType.Measure));
    }

    @Override
    public Bundle storeBundle(Bundle bundle) {
        return storeClient.transaction().withBundle(bundle).execute();
    }
}
