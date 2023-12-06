package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationSettingsProvider;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_EPSILON;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_SENSITIVITY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_STRATEGY;

public class SetupEvaluationSettings extends AbstractServiceDelegate
        implements InitializingBean {

    private final EvaluationSettingsProvider evaluationSettingsProvider;

    public SetupEvaluationSettings(EvaluationSettingsProvider evaluationSettingsProvider, ProcessPluginApi api) {
        super(api);
        this.evaluationSettingsProvider = evaluationSettingsProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(evaluationSettingsProvider, "variablesSettingsProvider");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        variables.setString(VARIABLE_EVALUATION_STRATEGY,
                evaluationSettingsProvider.evaluationStrategy().toString());
        variables.setBoolean(VARIABLE_EVALUATION_OBFUSCATION,
                evaluationSettingsProvider.evaluationResultObfuscationEnabled());
        variables.setDouble(VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_SENSITIVITY,
                evaluationSettingsProvider.resultObfuscationLaplaceSensitivity());
        variables.setDouble(VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_EPSILON,
                evaluationSettingsProvider.resultObfuscationLaplaceEpsilon());
    }
}
