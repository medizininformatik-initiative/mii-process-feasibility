package de.medizininformatik_initiative.feasibility_dsf_process.service;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.Map;

import static java.lang.String.format;

public class SelectResponseTarget extends dev.dsf.bpe.v1.activity.AbstractServiceDelegate implements InitializingBean {

    public SelectResponseTarget(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception {
        Task task = variables.getStartTask();
        String correlationKey = api.getTaskHelper()
                .getFirstInputParameterStringValue(task, BpmnMessage.URL, BpmnMessage.Codes.CORRELATION_KEY).get();
        Identifier organizationIdentifier = task.getRequester().getIdentifier();

        // Workaround till https://github.com/datasharingframework/dsf/pull/62 is released
        Bundle resultBundle = api.getFhirWebserviceClientProvider().getLocalWebserviceClient().searchWithStrictHandling(
                Organization.class,
                Map.of("active", Collections.singletonList("true"),
                        "identifier", Collections.singletonList(organizationIdentifier.getValue()),
                        "_include", Collections.singletonList("Organization:endpoint")));
        if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getEntry().size() != 2
                || resultBundle.getEntryFirstRep().getResource() == null
                || !(resultBundle.getEntryFirstRep().getResource() instanceof Organization)
                || resultBundle.getEntry().get(1).getResource() == null
                || !(resultBundle.getEntry().get(1).getResource() instanceof Endpoint)) {
            throw new BpmnError("orgNotFound",
                    format("No active (or more than one) Organization or no Endpoint found for identifier '%s'",
                            organizationIdentifier.getValue()));
        }

        Target target = resultBundle.getEntry().stream()
                .filter(BundleEntryComponent::hasSearch)
                .filter(e -> SearchEntryMode.INCLUDE.equals(e.getSearch().getMode()))
                .filter(BundleEntryComponent::hasResource)
                .map(BundleEntryComponent::getResource)
                .filter(r -> r instanceof Endpoint)
                .map(r -> (Endpoint) r)
                .findFirst()
                .map(e -> variables.createTarget(organizationIdentifier.getValue(),
                        e.getIdentifierFirstRep().getValue(), e.getAddress(), correlationKey))
                .get();

        variables.setTarget(target);
    }
}
