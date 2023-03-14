package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationSettingsProvider;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_EPSILON;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_SENSITIVITY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_STRATEGY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SetupEvaluationSettingsTest {

    @Mock private EvaluationSettingsProvider settingsProvider;
    @Mock private DelegateExecution execution;

    @InjectMocks private SetupEvaluationSettings service;

    @Test
    public void testDoExecute() throws Exception {
        String expectedEvaluationStrategy = "cql";
        double sensitivity = 113045d;
        double epsilon = 113810d;

        when(settingsProvider.evaluationStrategyRepresentation()).thenReturn(expectedEvaluationStrategy);
        when(settingsProvider.evaluationResultObfuscationEnabled()).thenReturn(true);
        when(settingsProvider.resultObfuscationLaplaceSensitivity()).thenReturn(sensitivity);
        when(settingsProvider.resultObfuscationLaplaceEpsilon()).thenReturn(epsilon);

        service.execute(execution);
        verify(execution).setVariable(VARIABLE_EVALUATION_STRATEGY, expectedEvaluationStrategy);
        verify(execution).setVariable(VARIABLE_EVALUATION_OBFUSCATION, true);
        verify(execution).setVariable(VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_SENSITIVITY, sensitivity);
        verify(execution).setVariable(VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_EPSILON, epsilon);
    }

}
