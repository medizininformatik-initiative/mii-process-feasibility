package de.medizininformatik_initiative.feasibility_dsf_process.service;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v1.service.EndpointProvider;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import dev.dsf.bpe.v1.service.OrganizationProvider;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.client.FhirWebserviceClient;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.value.PrimitiveValue;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SelectResponseTargetTest {

    @Captor ArgumentCaptor<PrimitiveValue<Target>> targetsValuesCaptor;

    @Mock private TaskHelper taskHelper;
    @Mock private DelegateExecution execution;
    @Mock private ProcessPluginApi api;
    @Mock private Variables variables;
    @Mock private FhirWebserviceClient client;
    @Mock private OrganizationProvider organizationProvider;
    @Mock private EndpointProvider endpointProvider;
    @Mock private FhirWebserviceClientProvider clientProvider;

    @InjectMocks private SelectResponseTarget service;




    @Test
    public void testDoExecute() throws Exception {
        Task task = new Task();
        Identifier organizationId = new Identifier()
                .setSystem("http://localhost/systems/sample-system")
                .setValue("requester-id");
        Reference requesterReference = new Reference().setIdentifier(organizationId);
        task.setRequester(requesterReference);
        Identifier endpointId = new Identifier()
                .setSystem("http://localhost/systems/endpoint-system")
                .setValue("Test Endpoint");
        Endpoint endpoint = new Endpoint()
                .addIdentifier(endpointId)
                .setAddress("https://localhost/endpoint");
        String endpointReference = "endpoint-184410";
        Organization organization = new Organization()
                .setIdentifier(List.of(organizationId))
                .setEndpoint(List.of(new Reference(endpoint).setReference(endpointReference)));
        Target target = mock(Target.class);
        Bundle bundle = new Bundle();
        bundle.addEntry().setSearch(new BundleEntrySearchComponent().setMode(SearchEntryMode.MATCH))
                .setResource(organization);
        bundle.addEntry().setSearch(new BundleEntrySearchComponent().setMode(SearchEntryMode.INCLUDE))
                .setResource(endpoint);
        String correlationKey = "correlation-key-123547";

        when(api.getVariables(execution)).thenReturn(variables);
        when(variables.getStartTask()).thenReturn(task);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(taskHelper.getFirstInputParameterStringValue(task, BpmnMessage.URL, BpmnMessage.Codes.CORRELATION_KEY))
                .thenReturn(Optional.of(correlationKey));
        when(api.getOrganizationProvider()).thenReturn(organizationProvider);
        when(organizationProvider.getOrganization(organizationId)).thenReturn(Optional.of(organization));
        when(api.getFhirWebserviceClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
        when(client.read(Endpoint.class, endpointReference)).thenReturn(endpoint);
        when(variables.createTarget(organizationId.getValue(), endpointId.getValue(), endpoint.getAddress(),
                correlationKey)).thenReturn(target);

        service.execute(execution);

        verify(variables).setTarget(target);
    }
}
