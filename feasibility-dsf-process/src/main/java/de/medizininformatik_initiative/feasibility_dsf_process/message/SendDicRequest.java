package de.medizininformatik_initiative.feasibility_dsf_process.message;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractTaskMessageSend;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;

import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;

public class SendDicRequest extends AbstractTaskMessageSend {

    public SendDicRequest(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected Stream<ParameterComponent> getAdditionalInputParameters(DelegateExecution execution,
                                                                      Variables variables) {
        return Stream.of(api.getTaskHelper().createInput(
                new Reference(checkNotNull(variables.getString("measure-id"), "variable 'measure-id' not set")),
                CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE));
    }

    @Override
    protected void handleIntermediateThrowEventError(DelegateExecution execution, Variables variables,
                                                     Exception exception, String errorMessage) {
        execution.setVariableLocal("sendError", true);
    }

    @Override
    protected void addErrorMessage(Task task, String errorMessage) {
    }
}
