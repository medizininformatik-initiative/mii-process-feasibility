package de.medizininformatik_initiative.process.feasibility.message;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractTaskMessageSend;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.*;

public class SendDicResponse extends AbstractTaskMessageSend {

    private static final Logger logger = LoggerFactory.getLogger(SendDicResponse.class);

    public SendDicResponse(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution,
                                                                           Variables variables) {
        logger.info("getAdditionalInputParameters send result to requester");

        return Stream.of(api.getTaskHelper()
                .createInput(new Reference().setReference(variables.getString(VARIABLE_MEASURE_REPORT_ID)),
                        CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE));
    }
}
