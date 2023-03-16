package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationSettingsProvider;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION;
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
        var expectedEvaluationStrategy = "cql";
        when(settingsProvider.evaluationStrategyRepresentation()).thenReturn(expectedEvaluationStrategy);
        when(settingsProvider.evaluationResultObfuscationEnabled()).thenReturn(true);

        service.execute(execution);
        verify(execution).setVariable(VARIABLE_EVALUATION_STRATEGY, expectedEvaluationStrategy);
        verify(execution).setVariable(VARIABLE_EVALUATION_OBFUSCATION, true);
    }

}
