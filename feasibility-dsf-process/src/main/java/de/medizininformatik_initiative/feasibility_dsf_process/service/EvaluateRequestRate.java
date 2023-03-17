package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.RateLimit;
import de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.InitializingBean;

/**
 * This class implements a rate limiting
 *
 * @author <a href="mailto:math2306@hotmail.com">Mathias Rühle</a>
 */
public class EvaluateRequestRate extends AbstractServiceDelegate implements InitializingBean {

    private RateLimit rateLimit;

    public EvaluateRequestRate(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
            ReadAccessHelper readAccessHelper, RateLimit rateLimit) {
        super(clientProvider, taskHelper, readAccessHelper);
        this.rateLimit = rateLimit;
    }

    @Override
    protected void doExecute(DelegateExecution execution) throws BpmnError, Exception {
        execution.setVariable(ConstantsFeasibility.VARIABLE_REQUEST_RATE_BELOW_LIMIT,
                rateLimit.countRequestAndCheckLimit());
    }
}
