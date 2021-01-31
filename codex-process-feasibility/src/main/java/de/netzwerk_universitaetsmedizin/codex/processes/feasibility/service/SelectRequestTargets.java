package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS;

public class SelectRequestTargets extends AbstractServiceDelegate {

    private static final Logger logger = LoggerFactory.getLogger(SelectRequestTargets.class);

    private final OrganizationProvider organizationProvider;

    public SelectRequestTargets(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                OrganizationProvider organizationProvider) {
        super(clientProvider, taskHelper);
        this.organizationProvider = organizationProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(organizationProvider, "organizationProvider");
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        List<Target> targets = organizationProvider.getRemoteIdentifiers().stream().map(identifier -> Target
                .createBiDirectionalTarget(identifier.getValue(), UUID.randomUUID().toString()))
                .collect(Collectors.toList());

        logger.debug("Targets: " + targets);

        execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGETS, TargetsValues.create(new Targets(targets)));
    }
}
