package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationSettingsProvider;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_OBFUSCATION;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_STRATEGY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SetupEvaluationSettingsTest {

    @Mock
    private EvaluationSettingsProvider settingsProvider;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private SetupEvaluationSettings service;

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
