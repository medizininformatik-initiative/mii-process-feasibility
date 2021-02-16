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
import static org.junit.Assert.assertNotEquals;
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
        when(orgProvider.getRemoteIdentifiers())
                .thenReturn(new ArrayList<>());
        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());
        assertEquals(0, targetsValuesCaptor.getValue().getValue().getEntries().size());
    }

    @Test
    public void testDoExecute_SingleTarget() {
        final Identifier id = new Identifier()
                .setValue("http://localhost/foo");

        when(orgProvider.getRemoteIdentifiers())
                .thenReturn(new ArrayList<>(List.of(id)));
        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());
        assertEquals(1, targetsValuesCaptor.getValue().getValue().getEntries().size());
        assertEquals("http://localhost/foo", targetsValuesCaptor.getValue().getValue().getEntries().get(0)
                .getTargetOrganizationIdentifierValue());
    }

    @Test
    public void testDoExecute_MultipleTargets() {
        final Identifier idA = new Identifier()
                .setValue("http://localhost/foo");
        final Identifier idB = new Identifier()
                .setValue("http://localhost/bar");

        when(orgProvider.getRemoteIdentifiers())
                .thenReturn(new ArrayList<>(List.of(idA, idB)));
        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());
        assertEquals(2, targetsValuesCaptor.getValue().getValue().getEntries().size());
        assertEquals("http://localhost/foo", targetsValuesCaptor.getValue().getValue().getEntries().get(0)
                .getTargetOrganizationIdentifierValue());
        assertEquals("http://localhost/bar", targetsValuesCaptor.getValue().getValue().getEntries().get(1)
                .getTargetOrganizationIdentifierValue());
        assertNotEquals(targetsValuesCaptor.getValue().getValue().getEntries().get(0).getCorrelationKey(),
                targetsValuesCaptor.getValue().getValue().getEntries().get(1).getCorrelationKey());
    }
}
