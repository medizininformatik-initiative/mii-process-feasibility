package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS;

public class SelectRequestTargets extends AbstractServiceDelegate {

    private static final Logger logger = LoggerFactory.getLogger(SelectRequestTargets.class);

    private final OrganizationProvider organizationProvider;
    private final EndpointProvider endpointProvider;


    public SelectRequestTargets(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
                                EndpointProvider endpointProvider) {
        super(clientProvider, taskHelper, readAccessHelper);
        this.organizationProvider = organizationProvider;
        this.endpointProvider = endpointProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(organizationProvider, "organizationProvider");
        Objects.requireNonNull(endpointProvider, "endpointProvider");
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        var targets = organizationProvider.getRemoteOrganizations().stream()
                .filter(Organization::hasEndpoint)
                .filter(Organization::hasIdentifier)
                .map(organization -> {
                    String organizationIdentifier = organization.getIdentifierFirstRep().getValue();
                    return Target
                            .createBiDirectionalTarget(organization.getIdentifierFirstRep().getValue(),
                                    endpointProvider.getFirstDefaultEndpoint(organizationIdentifier).get().getId(),
                                    endpointProvider.getFirstDefaultEndpointAddress(organizationIdentifier).get(),
                                    UUID.randomUUID().toString());
                })
                .collect(Collectors.toList());

        targets.forEach(t -> logger.debug(t.getOrganizationIdentifierValue()));

        execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGETS, TargetsValues.create(new Targets(targets)));
        Task task = getCurrentTaskFromExecutionVariables(execution);
        execution.setVariable("measure-id",
                getFhirWebserviceClientProvider().getLocalBaseUrl() + "/" + getMeasureId(task));
    }

    private String getMeasureId(Task task) {
        Optional<Reference> measureRef = getTaskHelper()
                .getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                        ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE);
        if (measureRef.isPresent()) {
            return measureRef.get().getReference();
        } else {
            logger.error("Task {} is missing the measure reference.", task.getId());
            throw new RuntimeException("Missing measure reference.");
        }
    }
}
