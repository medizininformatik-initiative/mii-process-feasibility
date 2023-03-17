package de.medizininformatik_initiative.feasibility_dsf_process.service;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.springframework.beans.factory.InitializingBean;

import static org.hl7.fhir.r4.model.Task.TaskStatus.FAILED;

public class RateLimitExceededTaskRejecter extends AbstractServiceDelegate implements InitializingBean {

    private final TaskHelper taskHelper;

    public RateLimitExceededTaskRejecter(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
            ReadAccessHelper readAccessHelper) {
        super(clientProvider, taskHelper, readAccessHelper);
        this.taskHelper = taskHelper;
    }

    @Override
    protected void doExecute(DelegateExecution execution) throws BpmnError, Exception {
        CodeableConcept reason = new CodeableConcept().setText("The request rate limit has been exceeded.");
        taskHelper.getTask(execution).setStatus(FAILED).setStatusReason(reason);
    }

}
