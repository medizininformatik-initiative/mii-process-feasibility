package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SelectRequestTargetsTest {

    private static final String SYSTEM = "system-03:07:12";
    private static final String IDENTIFIER_VALUE = "id-02:52:02";
    private static final String BASE_URL = "foo";
    private static final String MEASURE_ID = "measure-id-11:57:29";

    @Captor ArgumentCaptor<PrimitiveValue<Targets>> targetsValuesCaptor;

    @Mock private EndpointProvider endpointProvider;
    @Mock private OrganizationProvider organizationProvider;
    @Mock private DelegateExecution execution;
    @Mock private TaskHelper taskHelper;
    @Mock private FhirWebserviceClientProvider clientProvider;
    @Mock private FhirWebserviceClient client;
    @Mock private Task task;
    @Mock private Endpoint endpointA;
    @Mock private Endpoint endpointB;
    @Mock Bundle bundle;

    @InjectMocks private SelectRequestTargets service;

    @BeforeEach
    public void setup() {
        Reference measureReference = new Reference(MEASURE_ID);

        when(taskHelper.getCurrentTaskFromExecutionVariables(execution)).thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
        .thenReturn(Optional.of(measureReference));
        when(clientProvider.getLocalBaseUrl()).thenReturn(BASE_URL);
        when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
        when(client.searchWithStrictHandling(Mockito.any(Class.class), Mockito.anyMap())).thenReturn(bundle);
    }

    @Test
    public void testDoExecute_NoTargets() {
        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());
        verify(execution).setVariable("measure-id", BASE_URL + "/" + MEASURE_ID);
        assertEquals(0, targetsValuesCaptor.getValue().getValue().getEntries().size());
    }

    @Test
    public void testDoExecute_SingleTarget() {
        var organizationId = new Identifier().setValue("http://localhost/foo");
        var dic_endpoint = new Endpoint()
                .setIdentifier(List.of(new Identifier()
                        .setSystem("http://highmed.org/sid/endpointA-identifier")
                        .setValue("DIC Endpoint")))
                .setAddress("https://dic/fhir");
        var organization = new Organization()
                .setIdentifier(List.of(organizationId))
                .setEndpoint(List.of(new Reference(dic_endpoint)))
                .setActiveElement((BooleanType) new BooleanType().setValue(true));
        var endpointId = "endpointId-12:24:41";


        when(endpointProvider.getFirstDefaultEndpointAddress(organizationId.getValue()))
                .thenReturn(Optional.of("https://dic/fhir"));
        when(endpointProvider.getFirstDefaultEndpoint(organizationId.getValue())).thenReturn(Optional.of(endpointA));
        when(endpointA.getId()).thenReturn(endpointId);
        when(bundle.getEntry()).thenReturn(List.of(new BundleEntryComponent().setResource(organization)));
        when(organizationProvider.getLocalIdentifier())
                .thenReturn(new Identifier().setSystem(SYSTEM).setValue(IDENTIFIER_VALUE));

        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());

        var targets = targetsValuesCaptor.getValue().getValue();
        assertEquals(1, targets.getEntries().size());
        assertEquals("http://localhost/foo", targets.getEntries().get(0).getOrganizationIdentifierValue());
        assertEquals("https://dic/fhir", targets.getEntries().get(0).getEndpointUrl());
        assertEquals(endpointId, targets.getEntries().get(0).getEndpointIdentifierValue());
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
                .setEndpoint(List.of(new Reference(dic_1_endpoint)))
                .setActiveElement((BooleanType) new BooleanType().setValue(true));
        var organizationB = new Organization()
                .setIdentifier(List.of(organizationBId))
                .setEndpoint(List.of(new Reference(dic_2_endpoint)))
                .setActiveElement((BooleanType) new BooleanType().setValue(true));
        var endpointAId = "endpointAId-12:24:41";
        var endpointBId = "endpointBId-12:27:09";


        when(endpointProvider.getFirstDefaultEndpointAddress(organizationAId.getValue()))
                .thenReturn(Optional.of("https://dic-1/fhir"));
        when(endpointProvider.getFirstDefaultEndpointAddress(organizationBId.getValue()))
                .thenReturn(Optional.of("https://dic-2/fhir"));
        when(endpointProvider.getFirstDefaultEndpoint(organizationAId.getValue())).thenReturn(Optional.of(endpointA));
        when(endpointProvider.getFirstDefaultEndpoint(organizationBId.getValue())).thenReturn(Optional.of(endpointB));
        when(endpointA.getId()).thenReturn(endpointAId);
        when(endpointB.getId()).thenReturn(endpointBId);
        when(bundle.getEntry()).thenReturn(List.of(new BundleEntryComponent().setResource(organizationA),
                new BundleEntryComponent().setResource(organizationB)));
        when(organizationProvider.getLocalIdentifier())
                .thenReturn(new Identifier().setSystem(SYSTEM).setValue(IDENTIFIER_VALUE));

        service.doExecute(execution);

        verify(execution).setVariable(eq(BPMN_EXECUTION_VARIABLE_TARGETS), targetsValuesCaptor.capture());

        var targets = targetsValuesCaptor.getValue().getValue();
        assertEquals(2, targets.getEntries().size());
        assertEquals("http://localhost/foo", targets.getEntries().get(0)
                .getOrganizationIdentifierValue());
        assertEquals("http://localhost/bar", targets.getEntries().get(1)
                .getOrganizationIdentifierValue());
        assertNotEquals(targetsValuesCaptor.getValue().getValue().getEntries().get(0).getCorrelationKey(),
                targetsValuesCaptor.getValue().getValue().getEntries().get(1).getCorrelationKey());
        assertEquals("https://dic-1/fhir", targets.getEntries().get(0)
                .getEndpointUrl());
        assertEquals("https://dic-2/fhir", targets.getEntries().get(1)
                .getEndpointUrl());
        assertEquals(endpointAId, targets.getEntries().get(0).getEndpointIdentifierValue());
        assertEquals(endpointBId, targets.getEntries().get(1).getEndpointIdentifierValue());
    }
}
