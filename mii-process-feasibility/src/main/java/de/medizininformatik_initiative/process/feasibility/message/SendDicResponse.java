package de.medizininformatik_initiative.process.feasibility.message;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractTaskMessageSend;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;

import java.util.stream.Stream;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;

public class SendDicResponse extends AbstractTaskMessageSend {


    public SendDicResponse(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution,
                                                                           Variables variables) {
        return Stream.of(api.getTaskHelper()
                .createInput(new Reference().setReference(variables.getString(VARIABLE_MEASURE_REPORT_ID)),
                        CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE));
    }
}
