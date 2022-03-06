package de.medizininformatik_initiative.feasibility_dsf_process.message;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Stream;

public class SendDicRequest extends AbstractTaskMessageSend {

    private static final Logger logger = LoggerFactory.getLogger(SendDicRequest.class);

    public SendDicRequest(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                          ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
                          FhirContext fhirContext) {
        super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
    }

    protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution) {
        Task task = getCurrentTaskFromExecutionVariables();

        String measureId = getFhirWebserviceClientProvider().getLocalBaseUrl() + "/" + getMeasureId(task);
        logger.debug("measureId = {}", measureId);

        return Stream.of(getTaskHelper().createInput(ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE,
                new Reference().setReference(measureId)));
    }

    private String getMeasureId(Task task) {
        Optional<Reference> measureRef = getTaskHelper()
                .getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                        ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE);
        if (measureRef.isPresent()) {
            return measureRef.get().getReference();
        } else {
            logger.error("Task {} is missing the measure reference.", task.getId());
            throw new RuntimeException("Missing measure reference.");
        }
    }
}
