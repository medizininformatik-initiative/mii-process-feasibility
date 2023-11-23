package de.medizininformatik_initiative.feasibility_dsf_process.client.listener;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.constants.BpmnExecutionVariables;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Variables;
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
        Variables variables = api.getVariables(execution);
        Target target = variables.getTarget();

        execution.setVariableLocal(BpmnExecutionVariables.CORRELATION_KEY, target.getCorrelationKey());
    }
}
