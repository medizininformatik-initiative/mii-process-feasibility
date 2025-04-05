package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;

import java.net.URI;

public class SelectResponseTarget implements ServiceTask {

    @Override
    public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception {
        var task = variables.getStartTask();
        var correlationKey = api.getTaskHelper()
                .getFirstInputParameterStringValue(task, BpmnMessage.correlationKey()).get();
        var organizationIdentifier = task.getRequester().getIdentifier();
        var endpoint = api.getOrganizationProvider()
                .getOrganization(organizationIdentifier)
                .map(Organization::getEndpointFirstRep)
                .map(Reference::getReference)
                .map(r -> {
                    DsfClient client = api.getDsfClientProvider().getLocalDsfClient();
                    String path = URI.create(r).getPath();
                    return client.read(Endpoint.class, path.substring(path.lastIndexOf("/") + 1));
                })
                .orElseThrow(() -> new IllegalArgumentException(
                        "No endpoint found for organization with identifier '%s'"
                                .formatted(organizationIdentifier.getValue())));

        var target = variables.createTarget(organizationIdentifier.getValue(),
                endpoint.getIdentifierFirstRep().getValue(), endpoint.getAddress(), correlationKey);

        variables.setTarget(target);
    }
}
