package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.springframework.beans.factory.InitializingBean;

import static org.hl7.fhir.r4.model.Task.TaskStatus.FAILED;

public class RateLimitExceededTaskRejecter extends AbstractServiceDelegate implements InitializingBean {

    public RateLimitExceededTaskRejecter(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception {
        var reason = new CodeableConcept().setText("The request rate limit has been exceeded.");
        variables.getStartTask().setStatus(FAILED).setStatusReason(reason);
    }

}
