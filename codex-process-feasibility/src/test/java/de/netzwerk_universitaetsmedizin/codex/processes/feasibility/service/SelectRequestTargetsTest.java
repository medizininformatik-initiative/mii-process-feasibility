package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.variables.Targets;
import org.hl7.fhir.r4.model.Identifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SelectRequestTargetsTest {

    @Captor
    ArgumentCaptor<PrimitiveValue<Targets>> targetsValuesCaptor;

    @Mock
    private OrganizationProvider orgProvider;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private SelectRequestTargets service;

    @Test
    public void testDoExecute_NoTargets() {
        when(orgProvider.getRemoteIdentifiers()).thenReturn(new ArrayList<>());
        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());
        assertEquals(0, targetsValuesCaptor.getValue().getValue().getEntries().size());
    }

    @Test
    public void testDoExecute_SingleTarget() {
        final Identifier id = new Identifier();
        id.setValue("http://localhost/id-140423");

        when(orgProvider.getRemoteIdentifiers()).thenReturn(new ArrayList<>(List.of(id)));
        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());
        assertEquals(1, targetsValuesCaptor.getValue().getValue().getEntries().size());
    }

    @Test
    public void testDoExecute_SingleTargetWithoutValue() {
        final Identifier id = new Identifier();

        when(orgProvider.getRemoteIdentifiers()).thenReturn(new ArrayList<>(List.of(id)));
        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());
        assertEquals(1, targetsValuesCaptor.getValue().getValue().getEntries().size());
    }

    @Test
    public void testDoExecute_MultipleTargets() {
        final Identifier idA = new Identifier();
        idA.setValue("http://localhost/id-140423");
        final Identifier idB = new Identifier();
        idB.setValue("http://localhost/id-140556");

        when(orgProvider.getRemoteIdentifiers()).thenReturn(new ArrayList<>(List.of(idA, idB)));
        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());
        assertEquals(2, targetsValuesCaptor.getValue().getValue().getEntries().size());
    }
}
