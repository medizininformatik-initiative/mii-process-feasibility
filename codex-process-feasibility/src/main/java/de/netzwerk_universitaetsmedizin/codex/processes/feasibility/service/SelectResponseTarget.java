package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY;

public class SelectResponseTarget extends AbstractServiceDelegate implements InitializingBean {

    private final OrganizationProvider organizationProvider;

    public SelectResponseTarget(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
        Task task = getCurrentTaskFromExecutionVariables();

        String correlationKey = getTaskHelper()
                .getFirstInputParameterStringValue(task, CODESYSTEM_HIGHMED_BPMN,
                        CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY).get();
        Identifier targetOrganizationIdentifier = task.getRequester().getIdentifier();

        execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET, TargetValues
                .create(Target.createBiDirectionalTarget(targetOrganizationIdentifier.getValue(), correlationKey)));
    }
}
