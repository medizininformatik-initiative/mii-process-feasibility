package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogReceiveTimeout implements ServiceTask {

    private static final Logger logger = LoggerFactory.getLogger(LogReceiveTimeout.class);

    @Override
    public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception {
        var target = variables.getTarget();
        var task = variables.getStartTask();
        logger.warn("Timeout while waiting for result from {} (endpoint url: {}) [task: {}]",
                target.getOrganizationIdentifierValue(),
                target.getEndpointUrl(),
                api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));
    }

}
