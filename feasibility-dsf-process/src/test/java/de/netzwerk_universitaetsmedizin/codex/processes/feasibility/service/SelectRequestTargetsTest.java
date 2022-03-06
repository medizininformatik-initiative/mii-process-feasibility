package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.variables.Targets;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    private EndpointProvider endpointProvider;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private SelectRequestTargets service;

    @Test
    public void testDoExecute_NoTargets() {
        when(orgProvider.getRemoteOrganizations())
                .thenReturn(new ArrayList<>());
        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());
        assertEquals(0, targetsValuesCaptor.getValue().getValue().getEntries().size());
    }

    @Test
    public void testDoExecute_SingleTarget() {
        var organizationId = new Identifier().setValue("http://localhost/foo");
        var dic_endpoint = new Endpoint()
                .setIdentifier(List.of(new Identifier()
                        .setSystem("http://highmed.org/sid/endpoint-identifier")
                        .setValue("DIC Endpoint")))
                .setAddress("https://dic/fhir");
        var organization = new Organization()
                .setIdentifier(List.of(organizationId))
                .setEndpoint(List.of(new Reference(dic_endpoint)));

        when(orgProvider.getRemoteOrganizations())
                .thenReturn(List.of(organization));
        when(endpointProvider.getFirstDefaultEndpointAddress(organizationId.getValue()))
                .thenReturn(Optional.of("https://dic/fhir"));

        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());

        var targets = targetsValuesCaptor.getValue().getValue();
        assertEquals(1, targets.getEntries().size());
        assertEquals("http://localhost/foo", targets.getEntries().get(0).getTargetOrganizationIdentifierValue());
        assertEquals("https://dic/fhir", targets.getEntries().get(0).getTargetEndpointUrl());
    }

    @Test
    public void testDoExecute_MultipleTargets() {
        var organizationAId = new Identifier().setValue("http://localhost/foo");
        var organizationBId = new Identifier().setValue("http://localhost/bar");
        var dic_1_endpoint = new Endpoint()
                .setIdentifier(List.of(new Identifier()
                        .setSystem("http://highmed.org/sid/endpoint-identifier")
                        .setValue("DIC 1 Endpoint")))
                .setAddress("https://dic-1/fhir");
        var dic_2_endpoint = new Endpoint()
                .setIdentifier(List.of(new Identifier()
                        .setSystem("http://highmed.org/sid/endpoint-identifier")
                        .setValue("DIC 2 Endpoint")))
                .setAddress("https://dic-2/fhir");
        var organizationA = new Organization()
                .setIdentifier(List.of(organizationAId))
                .setEndpoint(List.of(new Reference(dic_1_endpoint)));
        var organizationB = new Organization()
                .setIdentifier(List.of(organizationBId))
                .setEndpoint(List.of(new Reference(dic_2_endpoint)));

        when(orgProvider.getRemoteOrganizations())
                .thenReturn(List.of(organizationA, organizationB));

        when(endpointProvider.getFirstDefaultEndpointAddress(organizationAId.getValue()))
                .thenReturn(Optional.of("https://dic-1/fhir"));
        when(endpointProvider.getFirstDefaultEndpointAddress(organizationBId.getValue()))
                .thenReturn(Optional.of("https://dic-2/fhir"));

        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());

        var targets = targetsValuesCaptor.getValue().getValue();
        assertEquals(2, targets.getEntries().size());
        assertEquals("http://localhost/foo", targets.getEntries().get(0)
                .getTargetOrganizationIdentifierValue());
        assertEquals("http://localhost/bar", targets.getEntries().get(1)
                .getTargetOrganizationIdentifierValue());
        assertNotEquals(targetsValuesCaptor.getValue().getValue().getEntries().get(0).getCorrelationKey(),
                targetsValuesCaptor.getValue().getValue().getEntries().get(1).getCorrelationKey());
        assertEquals("https://dic-1/fhir", targets.getEntries().get(0)
                .getTargetEndpointUrl());
        assertEquals("https://dic-2/fhir", targets.getEntries().get(1)
                .getTargetEndpointUrl());
    }
}
