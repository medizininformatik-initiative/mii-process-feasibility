package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.CodeableConcept;

import static org.hl7.fhir.r4.model.Task.TaskStatus.FAILED;

public class RateLimitExceededTaskRejecter implements ServiceTask {

    @Override
    public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception {
        var reason = new CodeableConcept().setText("The request rate limit has been exceeded.");
        variables.getStartTask().setStatus(FAILED).setStatusReason(reason);
    }

}
