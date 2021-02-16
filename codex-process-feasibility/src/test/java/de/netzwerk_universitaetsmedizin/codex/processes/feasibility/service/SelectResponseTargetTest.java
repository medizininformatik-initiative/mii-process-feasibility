package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SelectResponseTargetTest {

    @Captor
    ArgumentCaptor<PrimitiveValue<Target>> targetsValuesCaptor;

    @Mock
    private TaskHelper taskHelper;

    @Mock
    private Task task;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private SelectResponseTarget service;

    @Test
    public void testDoExecute() throws Exception {
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterStringValue(task, CODESYSTEM_HIGHMED_BPMN,
                CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY))
                .thenReturn(Optional.of("correlation-key"));

        final Reference reference = new Reference()
                .setIdentifier(new Identifier()
                        .setSystem("http://localhost/systems/sample-system")
                        .setValue("requester-id"));

        when(task.getRequester())
                .thenReturn(reference);

        service.execute(execution);
        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGET), targetsValuesCaptor.capture());
        assertEquals("correlation-key", targetsValuesCaptor.getValue().getValue().getCorrelationKey());
        assertEquals("requester-id", targetsValuesCaptor.getValue().getValue()
                .getTargetOrganizationIdentifierValue());
    }

}
