package de.medizininformatik_initiative.feasibility_dsf_process.service;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v1.constants.NamingSystems;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Targets;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static org.hl7.fhir.r4.model.Task.TaskIntent.ORDER;

public class SendDicRequests extends AbstractServiceDelegate {

    private static final Logger logger = LoggerFactory.getLogger(SendDicRequests.class);

    private final ForkJoinPool forkJoinPool;

    // set via field injection
    private FixedValue instantiatesCanonical;
    private FixedValue messageName;
    private FixedValue profile;

    public SendDicRequests(ProcessPluginApi api, ForkJoinPool forkJoinPool) {
        super(api);
        this.forkJoinPool = forkJoinPool;
    }

    public void setInstantiatesCanonical(FixedValue instantiatesCanonical) {
        this.instantiatesCanonical = instantiatesCanonical;
    }

    public void setMessageName(FixedValue messageName) {
        this.messageName = messageName;
    }

    public void setProfile(FixedValue profile) {
        this.profile = profile;
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception {
        Targets targets = variables.getTargets();
        Task targetTaskTemplate = createTargetTaskTemplate(execution, variables);

        forkJoinPool.submit(
                () -> targets.getEntries()
                        .parallelStream()
                        .map((target) -> {
                            Task targetTask = targetTaskTemplate.copy();

                            targetTask.getRestriction().addRecipient(getRecipient(target));

                            String correlationKey = target.getCorrelationKey();
                            if (correlationKey != null) {
                                ParameterComponent correlationKeyInput = new ParameterComponent(
                                        new CodeableConcept(
                                                new Coding(BpmnMessage.URL, BpmnMessage.Codes.CORRELATION_KEY, null)),
                                        new StringType(correlationKey));
                                targetTask.getInput().add(correlationKeyInput);
                            }

                            return sendTask(targetTask, target);
                        })
                        .collect(Collectors.toList()))
                .get();
    }

    private Task createTargetTaskTemplate(DelegateExecution execution, Variables variables) {
        String instantiatesCanonical = checkNotNull(this.instantiatesCanonical).getExpressionText();
        String messageName = checkNotNull(this.messageName).getExpressionText();
        String profile = checkNotNull(this.profile).getExpressionText();
        Reference measure = new Reference()
                .setReference(checkNotNull(variables.getString("measure-id"), "variable 'measure-id' not set"));
        String businessKey = execution.getBusinessKey();
        Task targetTask = new Task();
        targetTask.copy();
        targetTask.setMeta(new Meta().addProfile(profile));
        targetTask.setStatus(TaskStatus.REQUESTED);
        targetTask.setIntent(ORDER);
        targetTask.setAuthoredOn(new Date());
        targetTask.setRequester(getRequester());
        targetTask.setInstantiatesCanonical(instantiatesCanonical);

        ParameterComponent messageNameInput = new ParameterComponent(
                new CodeableConcept(new Coding(BpmnMessage.URL, BpmnMessage.Codes.MESSAGE_NAME, null)),
                new StringType(messageName));
        targetTask.addInput(messageNameInput);

        ParameterComponent businessKeyInput = new ParameterComponent(
                new CodeableConcept(new Coding(BpmnMessage.URL, BpmnMessage.Codes.BUSINESS_KEY, null)),
                new StringType(businessKey));
        targetTask.getInput().add(businessKeyInput);
        targetTask.getInput().add(api.getTaskHelper().createInput(measure, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE));
        return targetTask;
    }

    private Reference getRequester() {
        return new Reference().setType("Organization")
                .setIdentifier(new Identifier().setSystem(NamingSystems.OrganizationIdentifier.SID)
                        .setValue(api.getOrganizationProvider().getLocalOrganizationIdentifierValue().get()));
    }

    private Reference getRecipient(Target target) {
        return new Reference().setType("Organization")
                .setIdentifier(new Identifier().setSystem(NamingSystems.OrganizationIdentifier.SID)
                        .setValue(target.getOrganizationIdentifierValue()));
    }

    private Boolean sendTask(Task task, Target target) {
        try {
            logger.info("Sending task to target organization {}, endpoint {}",
                    target.getOrganizationIdentifierValue(), target.getEndpointUrl());
            api.getFhirWebserviceClientProvider()
                    .getWebserviceClient(target.getEndpointUrl())
                    .create(task);
            return true;
        } catch (Exception e) {
            logger.error("Error sending task to target organization {}, endpoint {}: {}",
                    target.getOrganizationIdentifierValue(), target.getEndpointUrl(), e.getMessage());
            return false;
        }
    }
}
