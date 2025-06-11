package de.medizininformatik_initiative.process.feasibility.listener;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.constants.BpmnExecutionVariables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

public class SetCorrelationKeyListener implements ExecutionListener, InitializingBean
{
    private final ProcessPluginApi api;

    public SetCorrelationKeyListener(ProcessPluginApi api) {
        this.api = api;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Objects.requireNonNull(api, "api");
    }

    @Override
    public void notify(DelegateExecution execution) throws Exception {
        var variables = api.getVariables(execution);
        var target = variables.getTarget();

        execution.setVariableLocal(BpmnExecutionVariables.CORRELATION_KEY, target.getCorrelationKey());
    }
}
