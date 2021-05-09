package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationStrategy;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationStrategyProvider;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_EVALUATION_STRATEGY;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class PrepareEvaluationStrategyTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private EvaluationStrategyProvider provider;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private PrepareEvaluationStrategy service;

    private final EvaluationStrategy evaluationStrategy;

    public PrepareEvaluationStrategyTest(String name, EvaluationStrategy evaluationStrategy) {
        this.evaluationStrategy = evaluationStrategy;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> evaluationStrategies() {
        return Arrays.stream(EvaluationStrategy.values()).map(es -> new Object[]{
                es.getStrategyRepresentation(), es
        })
                .collect(Collectors.toList());
    }

    @Test
    public void testDoExecute() throws Exception {
        when(provider.provideEvaluationStrategy()).thenReturn(evaluationStrategy);

        service.execute(execution);
        verify(execution).setVariable(VARIABLE_EVALUATION_STRATEGY, evaluationStrategy.getStrategyRepresentation());
    }
}
