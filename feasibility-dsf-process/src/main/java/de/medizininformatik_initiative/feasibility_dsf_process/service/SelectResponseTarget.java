package de.medizininformatik_initiative.feasibility_dsf_process.service;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

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
        Endpoint endpoint = api.getOrganizationProvider()
                .getOrganization(organizationIdentifier)
                .map(Organization::getEndpointFirstRep)
                .map(Reference::getReference)
                .map(r -> api.getEndpointProvider().getEndpoint(r).get())
                .get();

        Target target = variables.createTarget(organizationIdentifier.getValue(),
                endpoint.getIdentifierFirstRep().getValue(), endpoint.getAddress(), correlationKey);

        variables.setTarget(target);
    }
}
