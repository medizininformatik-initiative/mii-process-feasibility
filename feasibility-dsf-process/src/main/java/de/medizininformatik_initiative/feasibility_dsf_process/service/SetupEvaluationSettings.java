package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationSettingsProvider;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_STRATEGY;

public class SetupEvaluationSettings extends AbstractServiceDelegate implements InitializingBean {

    private final EvaluationSettingsProvider evaluationSettingsProvider;

    public SetupEvaluationSettings(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                   ReadAccessHelper readAccessHelper,
                                   EvaluationSettingsProvider evaluationSettingsProvider) {
        super(clientProvider, taskHelper, readAccessHelper);
        this.evaluationSettingsProvider = evaluationSettingsProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(evaluationSettingsProvider, "executionSettingsProvider");
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        execution.setVariable(VARIABLE_EVALUATION_STRATEGY,
                evaluationSettingsProvider.evaluationStrategyRepresentation());
        execution.setVariable(VARIABLE_EVALUATION_OBFUSCATION,
                evaluationSettingsProvider.evaluationResultObfuscationEnabled());
    }
}
