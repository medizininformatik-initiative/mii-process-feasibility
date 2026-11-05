package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.process.feasibility.CanonicalFixer;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_ID;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_REQUESTER_PARENT_ORGANIZATION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;

public class StoreFeasibilityResources extends AbstractServiceDelegate implements InitializingBean, CanonicalFixer {

    private static final Logger logger = LoggerFactory.getLogger(StoreFeasibilityResources.class);
    private final Map<String, IGenericClient> storeClients;
    private final FeasibilityResourceCleaner cleaner;
    private Map<String, Set<String>> networkStores;

    public StoreFeasibilityResources(Map<String, Set<String>> networkStores, Map<String, IGenericClient> storeClients,
            ProcessPluginApi api, FeasibilityResourceCleaner cleaner) {
        super(api);
        this.networkStores = networkStores;

        this.storeClients = storeClients;
        this.cleaner = cleaner;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        Objects.requireNonNull(storeClients, "storeClients");
        Objects.requireNonNull(cleaner, "cleaner");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        logger.info("doExecute store feasibility resources");

        var measure = ((Measure) variables.getResource(VARIABLE_MEASURE)).copy();
        var library = ((Library) variables.getResource(VARIABLE_LIBRARY)).copy();
        var requesterParentOrganization = variables.getString(VARIABLE_REQUESTER_PARENT_ORGANIZATION);
        var task = variables.getStartTask();

        cleaner.cleanLibrary(library);
        cleaner.cleanMeasure(measure);

        fixCanonical(measure, library);

        if (networkStores.containsKey(requesterParentOrganization)) {
            networkStores.get(requesterParentOrganization)
                    .parallelStream()
                    .forEach(storeId -> {
                        if (storeClients.containsKey(storeId)) {
                            var transactionResponse = storeResources(storeClients.get(storeId), measure, library,
                                    storeId, task);
                            variables.setString(VARIABLE_MEASURE_ID + "_" + storeId,
                                    extractMeasureId(transactionResponse));
                        }
                    });
        }
    }

    private Bundle storeResources(IGenericClient storeClient, Measure measure, Library library, String storeId,
                                  Task task) {
        logger.info("Storing Measure '{}' and Library '{}' in store '{}' [task: {}]", measure.getId(), library.getId(),
                storeId, api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));
        Bundle bundle = new Bundle().setType(TRANSACTION);
        bundle.addEntry().setResource(measure).getRequest().setMethod(POST).setUrl("Measure");
        bundle.addEntry().setResource(library).getRequest().setMethod(POST).setUrl("Library");
        return storeClient.transaction().withBundle(bundle).execute();
    }

    private String extractMeasureId(Bundle transactionResponse) {
        return new IdType(transactionResponse.getEntryFirstRep().getResponse().getLocation()).getValue();
    }
}
