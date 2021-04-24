package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationStrategyProvider;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_STRATEGY;

public class PrepareEvaluationStrategy extends AbstractServiceDelegate implements InitializingBean {

    private final EvaluationStrategyProvider evaluationStrategyProvider;

    public PrepareEvaluationStrategy(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                     EvaluationStrategyProvider evaluationStrategyProvider) {
        super(clientProvider, taskHelper);
        this.evaluationStrategyProvider = evaluationStrategyProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(evaluationStrategyProvider, "evaluationStrategyProvider");
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        execution.setVariable(VARIABLE_EVALUATION_STRATEGY,
                evaluationStrategyProvider.provideEvaluationStrategy().getStrategyRepresentation().toLowerCase());
    }
}
