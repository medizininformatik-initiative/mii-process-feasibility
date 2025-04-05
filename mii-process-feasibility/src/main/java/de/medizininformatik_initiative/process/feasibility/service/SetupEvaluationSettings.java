package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.EvaluationSettingsProvider;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_EPSILON;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_SENSITIVITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_STRATEGY;

public class SetupEvaluationSettings implements ServiceTask, InitializingBean {

    private final EvaluationSettingsProvider evaluationSettingsProvider;

    public SetupEvaluationSettings(EvaluationSettingsProvider evaluationSettingsProvider) {
        this.evaluationSettingsProvider = evaluationSettingsProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Objects.requireNonNull(evaluationSettingsProvider, "variablesSettingsProvider");
    }

    @Override
    public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception {
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
