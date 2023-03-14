package de.medizininformatik_initiative.feasibility_dsf_process.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SelectResponseTargetTest {

    @Captor ArgumentCaptor<PrimitiveValue<Target>> targetsValuesCaptor;

    @Mock private TaskHelper taskHelper;
    @Mock private EndpointProvider endpointProvider;
    @Mock private DelegateExecution execution;

    @InjectMocks private SelectResponseTarget service;

    @Test
    public void testDoExecute() throws Exception {
        Task task = new Task();
        Reference requesterReference = new Reference()
                .setIdentifier(new Identifier()
                        .setSystem("http://localhost/systems/sample-system")
                        .setValue("requester-id"));
        task.setRequester(requesterReference);
        when(endpointProvider.getFirstDefaultEndpointAddress(requesterReference.getIdentifier().getValue()))
                .thenReturn(Optional.of("endpoint-url"));
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterStringValue(task, CODESYSTEM_HIGHMED_BPMN,
                CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY))
                .thenReturn(Optional.of("correlation-key"));

        service.execute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGET), targetsValuesCaptor.capture());

        var target = targetsValuesCaptor.getValue().getValue();
        assertEquals("correlation-key", target.getCorrelationKey());
        assertEquals("requester-id", target.getTargetOrganizationIdentifierValue());
        assertEquals("endpoint-url", target.getTargetEndpointUrl());
    }
}
