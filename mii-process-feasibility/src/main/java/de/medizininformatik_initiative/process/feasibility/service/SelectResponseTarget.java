package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.client.FhirWebserviceClient;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.InitializingBean;

import java.net.URI;

import static java.lang.String.format;

public class SelectResponseTarget extends dev.dsf.bpe.v1.activity.AbstractServiceDelegate implements InitializingBean {

    public SelectResponseTarget(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception {
        var task = variables.getStartTask();
        var correlationKey = api.getTaskHelper()
                .getFirstInputParameterStringValue(task, BpmnMessage.URL, BpmnMessage.Codes.CORRELATION_KEY).get();
        var organizationIdentifier = task.getRequester().getIdentifier();
        var endpoint = api.getOrganizationProvider()
                .getOrganization(organizationIdentifier)
                .map(Organization::getEndpointFirstRep)
                .map(Reference::getReference)
                .map(r -> {
                    FhirWebserviceClient client = api.getFhirWebserviceClientProvider().getLocalWebserviceClient();
                    String path = URI.create(r).getPath();
                    return client.read(Endpoint.class, path.substring(path.lastIndexOf("/") + 1));
                })
                .orElseThrow(() -> new IllegalArgumentException(
                        format("No endpoint found for organization with identifier '%s'",
                                organizationIdentifier.getValue())));

        var target = variables.createTarget(organizationIdentifier.getValue(),
                endpoint.getIdentifierFirstRep().getValue(), endpoint.getAddress(), correlationKey);

        variables.setTarget(target);
    }
}
