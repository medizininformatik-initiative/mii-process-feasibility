package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.RateLimit;
import de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

/**
 * This class implements a rate limiting
 *
 * @author <a href="mailto:math2306@hotmail.com">Mathias Rühle</a>
 */
public class EvaluateRequestRate implements ServiceTask {

    private RateLimit rateLimit;

    public EvaluateRequestRate(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception {
        variables.setBoolean(ConstantsFeasibility.VARIABLE_REQUEST_RATE_BELOW_LIMIT,
                rateLimit.countRequestAndCheckLimit());
    }

}
