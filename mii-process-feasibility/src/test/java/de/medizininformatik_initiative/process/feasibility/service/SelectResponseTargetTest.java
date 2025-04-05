package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.EndpointProvider;
import dev.dsf.bpe.v2.service.OrganizationProvider;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntrySearchComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SelectResponseTargetTest {

    @Mock private TaskHelper taskHelper;
    @Mock private ProcessPluginApi api;
    @Mock private Variables variables;
    @Mock private DsfClient client;
    @Mock private OrganizationProvider organizationProvider;
    @Mock private EndpointProvider endpointProvider;
    @Mock private DsfClientProvider clientProvider;

    @InjectMocks private SelectResponseTarget service;

    @Test
    public void testDoExecute() throws Exception {
        var task = new Task();
        var organizationId = new Identifier()
                .setSystem("http://localhost/systems/sample-system")
                .setValue("requester-id");
        var requesterReference = new Reference().setIdentifier(organizationId);
        task.setRequester(requesterReference);
        var endpointId = new Identifier()
                .setSystem("http://localhost/systems/endpoint-system")
                .setValue("Test Endpoint");
        var endpoint = new Endpoint()
                .addIdentifier(endpointId)
                .setAddress("https://localhost/endpoint");
        var endpointReference = "endpoint-184410";
        var organization = new Organization()
                .setIdentifier(List.of(organizationId))
                .setEndpoint(List.of(new Reference(endpoint).setReference(endpointReference)));
        var target = mock(Target.class);
        var bundle = new Bundle();
        bundle.addEntry().setSearch(new BundleEntrySearchComponent().setMode(SearchEntryMode.MATCH))
                .setResource(organization);
        bundle.addEntry().setSearch(new BundleEntrySearchComponent().setMode(SearchEntryMode.INCLUDE))
                .setResource(endpoint);
        var correlationKey = "correlation-key-123547";

        when(variables.getStartTask()).thenReturn(task);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(taskHelper.getFirstInputParameterStringValue(eq(task),
                argThat(o -> o.equalsShallow(BpmnMessage.correlationKey()))))
                .thenReturn(Optional.of(correlationKey));
        when(api.getOrganizationProvider()).thenReturn(organizationProvider);
        when(organizationProvider.getOrganization(organizationId)).thenReturn(Optional.of(organization));
        when(api.getDsfClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getLocalDsfClient()).thenReturn(client);
        when(client.read(Endpoint.class, endpointReference)).thenReturn(endpoint);
        when(variables.createTarget(organizationId.getValue(), endpointId.getValue(), endpoint.getAddress(),
                correlationKey)).thenReturn(target);

        service.execute(api, variables);

        verify(variables).setTarget(target);
    }
}
