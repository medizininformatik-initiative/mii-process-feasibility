package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.RateLimit;
import de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.InitializingBean;

/**
 * This class implements a rate limiting
 *
 * @author <a href="mailto:math2306@hotmail.com">Mathias Rühle</a>
 */
public class EvaluateRequestRate extends AbstractServiceDelegate implements InitializingBean {

    private RateLimit rateLimit;

    public EvaluateRequestRate(RateLimit rateLimit, ProcessPluginApi api) {
        super(api);
        this.rateLimit = rateLimit;
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception {
        variables.setBoolean(ConstantsFeasibility.VARIABLE_REQUEST_RATE_BELOW_LIMIT,
                rateLimit.countRequestAndCheckLimit());
    }
}
