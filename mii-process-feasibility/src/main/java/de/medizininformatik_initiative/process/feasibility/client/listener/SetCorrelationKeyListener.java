package de.medizininformatik_initiative.process.feasibility.client.listener;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.constants.BpmnExecutionVariables;
import dev.dsf.bpe.v2.variables.Variables;

public class SetCorrelationKeyListener implements dev.dsf.bpe.v2.activity.ExecutionListener {

    @Override
    public void notify(ProcessPluginApi api, Variables variables) throws Exception {
        // TODO Auto-generated method stub
        var target = variables.getTarget();

        variables.setStringLocal(BpmnExecutionVariables.CORRELATION_KEY, target.getCorrelationKey());
    }
}
