package de.medizininformatik_initiative.process.feasibility.message;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.MessageIntermediateThrowEvent;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.error.MessageIntermediateThrowEventErrorHandler;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;

public class SendDicRequest implements MessageIntermediateThrowEvent {
    private static final Logger logger = LoggerFactory.getLogger(SendDicRequest.class);

    @Override
    public List<ParameterComponent> getAdditionalInputParameters(ProcessPluginApi api, Variables variables,
                                                                 SendTaskValues sendTaskValues, Target target) {
        // TODO Auto-generated method stub
        return List.of(api.getTaskHelper().createInput(
                new Reference(checkNotNull(variables.getString("measure-id"), "variable 'measure-id' not set")),
                CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE));
    }

    @Override
    public MessageIntermediateThrowEventErrorHandler getErrorHandler() {
        return new MessageIntermediateThrowEventErrorHandler() {

            @Override
            public Exception handleException(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
                                             Exception exception) {
                logger.debug("Error while executing Task message send {}", getClass().getName(), exception);
                logger.error("Process {} has fatal error in step {} for task {}, reason: {} - {}",
                        variables.getProcessDefinitionId(), variables.getActivityInstanceId(),
                        api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()),
                        exception.getClass().getName(), exception.getMessage());

                variables.setBooleanLocal("sendError", true);
                return null;
            }
        };
    }

}
