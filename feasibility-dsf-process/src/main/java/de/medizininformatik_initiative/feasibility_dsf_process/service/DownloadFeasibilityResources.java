package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.EnhancedFhirWebserviceClientProvider;
import de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DownloadFeasibilityResources extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(DownloadFeasibilityResources.class);

    private final OrganizationProvider organizationProvider;

    public DownloadFeasibilityResources(EnhancedFhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                        ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider) {
        super(clientProvider, taskHelper, readAccessHelper);
        this.organizationProvider = organizationProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(organizationProvider, "organizationProvider");
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        Task task = getCurrentTaskFromExecutionVariables();

        IdType measureId = getMeasureId(task);
        FhirWebserviceClient client = ((EnhancedFhirWebserviceClientProvider) getFhirWebserviceClientProvider())
                .getWebserviceClientByReference(measureId);
        Bundle bundle = getMeasureAndLibrary(measureId, client);

        execution.setVariable(ConstantsFeasibility.VARIABLE_MEASURE, bundle.getEntry().get(0).getResource());
        execution.setVariable(ConstantsFeasibility.VARIABLE_LIBRARY, bundle.getEntry().get(1).getResource());
    }

    private IdType getMeasureId(Task task) {
        Optional<Reference> measureRef = getTaskHelper()
                .getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                        ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE);
        if (measureRef.isPresent()) {
            return new IdType(measureRef.get().getReference());
        } else {
            logger.error("Task {} is missing the measure reference.", task.getId());
            throw new RuntimeException("Missing measure reference.");
        }
    }

    private Bundle getMeasureAndLibrary(IdType measureId, FhirWebserviceClient client) {
        try {
            Bundle bundle = client.searchWithStrictHandling(Measure.class,
                    Map.of("_id", Collections.singletonList(measureId.getIdPart()), "_include",
                            Collections.singletonList("Measure:depends-on")));

            if (bundle.getEntry().size() < 2) {
                throw new RuntimeException("Returned search-set contained less then two entries");
            } else if (!bundle.getEntry().get(0).hasResource() ||
                    !(bundle.getEntry().get(0).getResource() instanceof Measure)) {
                throw new RuntimeException("Returned search-set did not contain Measure at index 0");
            } else if (!bundle.getEntry().get(1).hasResource() ||
                    !(bundle.getEntry().get(1).getResource() instanceof Library)) {
                throw new RuntimeException("Returned search-set did not contain Library at index 1");
            }

            return bundle;
        } catch (Exception e) {
            logger.warn("Error while reading Measure with id {} including its Library from {}: {}",
                    measureId.getIdPart(), client.getBaseUrl(), e.getMessage());
            throw e;
        }
    }
}
