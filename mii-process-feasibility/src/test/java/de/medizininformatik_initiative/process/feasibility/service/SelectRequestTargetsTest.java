package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.EndpointProvider;
import dev.dsf.bpe.v2.service.OrganizationProvider;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Targets;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
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

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SelectRequestTargetsTest {

    private static final String BASE_URL = "foo/";
    private static final String MEASURE_ID = "measure-id-11:57:29";

    @Captor ArgumentCaptor<Targets> targetsValuesCaptor;

    @Mock private EndpointProvider endpointProvider;
    @Mock private OrganizationProvider organizationProvider;
    @Mock private TaskHelper taskHelper;
    @Mock private DsfClientProvider clientProvider;
    @Mock private DsfClient client;
    @Mock private ProcessPluginApi api;
    @Mock private Variables variables;
    @Mock private Task task;
    @Mock private Endpoint endpointA;
    @Mock private Endpoint endpointB;
    @Mock Bundle bundle;
    @Mock private Targets targets;
    @Mock private Target target;

    @InjectMocks private SelectRequestTargets service;

    @BeforeEach
    public void setup() {
        var measureReference = new Reference(MEASURE_ID);
        when(api.getOrganizationProvider()).thenReturn(organizationProvider);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(variables.getStartTask()).thenReturn(task);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class))
                        .thenReturn(Optional.of(measureReference));
        when(api.getDsfClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getLocalDsfClient()).thenReturn(client);
        when(client.getBaseUrl()).thenReturn(BASE_URL);
    }

    @Test
    public void doExecute_NoTargets() {
        when(organizationProvider.getOrganizations(any(Identifier.class), any(Coding.class)))
                .thenReturn(List.of());
        when(variables.createTargets(eq(List.of()))).thenReturn(targets);

        service.execute(api, variables);

        verify(variables).setTargets(targetsValuesCaptor.capture());
        verify(variables).setString("measure-id", BASE_URL + MEASURE_ID);
        assertEquals(0, targetsValuesCaptor.getValue().getEntries().size());
    }

    @Test
    public void testDoExecute_SingleTarget() {
        var organizationId = new Identifier().setValue("http://localhost/foo");
        var endpointId = new Identifier()
                .setSystem("http://highmed.org/sid/endpointA-identifier")
                .setValue("DIC Endpoint");
        var endpointAddress = "https://dic/fhir";
        var dic_endpoint = new Endpoint()
                .setIdentifier(List.of(endpointId))
                .setAddress(endpointAddress);
        var endpointReference = "reference-133340";
        var organization = new Organization()
                .setIdentifier(List.of(organizationId))
                .setEndpoint(List.of(new Reference(dic_endpoint).setReference(endpointReference)))
                .setActiveElement((BooleanType) new BooleanType().setValue(true));

        when(organizationProvider.getOrganizations(any(Identifier.class), any(Coding.class)))
                .thenReturn(List.of(organization));
        when(client.read(Endpoint.class, endpointReference)).thenReturn(dic_endpoint);
        when(variables.createTarget(eq(organizationId.getValue()), eq(endpointId.getValue()), eq(endpointAddress),
                                    anyString()))
                        .thenReturn(target);
        when(variables.createTargets(eq(List.of(target)))).thenReturn(targets);

        service.execute(api, variables);

        verify(variables).setTargets(targets);
    }

    @Test
    public void testDoExecute_SingleTargetUseEndpointReferenceIdOnly() {
        var organizationId = new Identifier().setValue("http://localhost/foo");
        var endpointId = new Identifier()
                .setSystem("http://highmed.org/sid/endpointA-identifier")
                .setValue("DIC Endpoint");
        var endpointAddress = "https://dic/fhir";
        var dic_endpoint = new Endpoint()
                .setIdentifier(List.of(endpointId))
                .setAddress(endpointAddress);
        var endpointReferenceId = "reference-133340";
        var endpointReference = "Endpoint/" + endpointReferenceId;
        var organization = new Organization()
                .setIdentifier(List.of(organizationId))
                .setEndpoint(List.of(new Reference(dic_endpoint).setReference(endpointReference)))
                .setActiveElement((BooleanType) new BooleanType().setValue(true));

        when(organizationProvider.getOrganizations(any(Identifier.class), any(Coding.class)))
                .thenReturn(List.of(organization));
        when(client.read(Endpoint.class, endpointReferenceId)).thenReturn(dic_endpoint);
        when(variables.createTarget(eq(organizationId.getValue()), eq(endpointId.getValue()), eq(endpointAddress),
                anyString()))
                        .thenReturn(target);
        when(variables.createTargets(eq(List.of(target)))).thenReturn(targets);

        service.execute(api, variables);

        verify(variables).setTargets(targets);
    }

    @Test
    public void testDoExecute_MultipleTargets() {
        var organizationAId = new Identifier().setValue("http://localhost/foo");
        var organizationBId = new Identifier().setValue("http://localhost/bar");
        var endpointAId = new Identifier()
                .setSystem("http://highmed.org/sid/endpoint-identifier")
                .setValue("DIC 1 Endpoint");
        var endpointAAddress = "https://dic-1/fhir";
        var dic_1_endpoint = new Endpoint()
                .setIdentifier(List.of(endpointAId))
                .setAddress(endpointAAddress);
        var endpointBId = new Identifier()
                .setSystem("http://highmed.org/sid/endpoint-identifier")
                .setValue("DIC 2 Endpoint");
        var endpointBAddress = "https://dic-2/fhir";
        var dic_2_endpoint = new Endpoint()
                .setIdentifier(List.of(endpointBId))
                .setAddress(endpointBAddress);
        var dic_1_endpointReference = "reference-133515";
        var organizationA = new Organization()
                .setIdentifier(List.of(organizationAId))
                .setEndpoint(List.of(new Reference(dic_1_endpoint).setReference(dic_1_endpointReference)))
                .setActiveElement((BooleanType) new BooleanType().setValue(true));
        var dic_2_endpointReference = "reference-133620";
        var organizationB = new Organization()
                .setIdentifier(List.of(organizationBId))
                .setEndpoint(List.of(new Reference(dic_2_endpoint).setReference(dic_2_endpointReference)))
                .setActiveElement((BooleanType) new BooleanType().setValue(true));
        var targetA = Mockito.mock(Target.class);
        var targetB = Mockito.mock(Target.class);

        when(organizationProvider.getOrganizations(any(Identifier.class), any(Coding.class)))
                .thenReturn(List.of(organizationA, organizationB));
        when(client.read(Endpoint.class, dic_1_endpointReference)).thenReturn(dic_1_endpoint);
        when(client.read(Endpoint.class, dic_2_endpointReference)).thenReturn(dic_2_endpoint);
        when(variables.createTarget(eq(organizationAId.getValue()), eq(endpointAId.getValue()), eq(endpointAAddress),
                anyString()))
                        .thenReturn(targetA);
        when(variables.createTarget(eq(organizationBId.getValue()), eq(endpointBId.getValue()), eq(endpointBAddress),
                anyString()))
                        .thenReturn(targetB);
        when(variables.createTargets(eq(List.of(targetA, targetB)))).thenReturn(targets);

        service.execute(api, variables);

        verify(variables).setTargets(targets);
    }
}
