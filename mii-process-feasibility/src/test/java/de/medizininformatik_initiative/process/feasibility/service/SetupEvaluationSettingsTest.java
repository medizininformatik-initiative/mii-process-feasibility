package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.EvaluationSettingsProvider;
import de.medizininformatik_initiative.process.feasibility.EvaluationStrategy;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_EPSILON;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_SENSITIVITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_STRATEGY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SetupEvaluationSettingsTest {

    @Mock private EvaluationSettingsProvider settingsProvider;
    @Mock private DelegateExecution execution;
    @Mock private Variables variables;
    @Mock private ProcessPluginApi api;

    @InjectMocks private SetupEvaluationSettings service;

    @Test
    public void testDoExecute() throws Exception {
        var expectedEvaluationStrategy = "cql";
        var sensitivity = 113045d;
        var epsilon = 113810d;

        when(api.getVariables(execution)).thenReturn(variables);
        when(settingsProvider.evaluationStrategy()).thenReturn(EvaluationStrategy.CQL);
        when(settingsProvider.evaluationResultObfuscationEnabled()).thenReturn(true);
        when(settingsProvider.resultObfuscationLaplaceSensitivity()).thenReturn(sensitivity);
        when(settingsProvider.resultObfuscationLaplaceEpsilon()).thenReturn(epsilon);

        service.execute(execution);
        verify(variables).setString(VARIABLE_EVALUATION_STRATEGY, expectedEvaluationStrategy);
        verify(variables).setBoolean(VARIABLE_EVALUATION_OBFUSCATION, true);
        verify(variables).setDouble(VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_SENSITIVITY, sensitivity);
        verify(variables).setDouble(VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_EPSILON, epsilon);
    }

}
