package de.medizininformatik_initiative.feasibility_dsf_process.service;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
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

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_INSTANTIATES_URI;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_MESSAGE_NAME;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_PROFILE;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.hl7.fhir.r4.model.Task.TaskIntent.ORDER;

public class SendDicRequests extends AbstractServiceDelegate {

    private static final Logger logger = LoggerFactory.getLogger(SendDicRequests.class);

    private final ForkJoinPool forkJoinPool;
    private final OrganizationProvider organizationProvider;

    public SendDicRequests(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
            ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, ForkJoinPool forkJoinPool) {
        super(clientProvider, taskHelper, readAccessHelper);
        this.organizationProvider = organizationProvider;
        this.forkJoinPool = forkJoinPool;
    }

    @Override
    protected void doExecute(DelegateExecution execution) throws BpmnError, Exception {
        Targets targets = (Targets) execution.getVariable(BPMN_EXECUTION_VARIABLE_TARGETS);
        Task targetTaskTemplate = createTargetTaskTemplate(execution);

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
                                                new Coding(CODESYSTEM_HIGHMED_BPMN,
                                                           CODESYSTEM_HIGHMED_BPMN_VALUE_CORRELATION_KEY,
                                                           null)),
                                        new StringType(correlationKey));
                                targetTask.getInput().add(correlationKeyInput);
                            }

                            return sendTask(targetTask, target);
                        })
                        .collect(Collectors.toList()))
                .get();
    }

    private Task createTargetTaskTemplate(DelegateExecution execution) {
        String instantiatesUri = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_INSTANTIATES_URI);
        String messageName = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_MESSAGE_NAME);
        String profile = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PROFILE);
        Reference measure = new Reference().setReference((String) execution.getVariable("measure-id"));
        String businessKey = execution.getBusinessKey();
        Task targetTask = new Task();
        targetTask.copy();
        targetTask.setMeta(new Meta().addProfile(profile));
        targetTask.setStatus(TaskStatus.REQUESTED);
        targetTask.setIntent(ORDER);
        targetTask.setAuthoredOn(new Date());
        targetTask.setRequester(getRequester());
        targetTask.setInstantiatesUri(instantiatesUri);

        ParameterComponent messageNameInput = new ParameterComponent(
                new CodeableConcept(
                        new Coding(CODESYSTEM_HIGHMED_BPMN,
                                CODESYSTEM_HIGHMED_BPMN_VALUE_MESSAGE_NAME,
                                null)),
                new StringType(messageName));
        targetTask.addInput(messageNameInput);

        ParameterComponent businessKeyInput = new ParameterComponent(
                new CodeableConcept(
                        new Coding(CODESYSTEM_HIGHMED_BPMN,
                                CODESYSTEM_HIGHMED_BPMN_VALUE_BUSINESS_KEY,
                                null)),
                new StringType(businessKey));
        targetTask.getInput().add(businessKeyInput);
        targetTask.getInput().add(getTaskHelper().createInput(CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE,
                measure));
        return targetTask;
    }

    private Reference getRequester() {
        return new Reference().setType("Organization")
                .setIdentifier(new Identifier().setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
                        .setValue(organizationProvider.getLocalIdentifierValue()));
    }

    private Reference getRecipient(Target target) {
        return new Reference().setType("Organization")
                .setIdentifier(new Identifier().setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
                        .setValue(target.getOrganizationIdentifierValue()));
    }

    private Boolean sendTask(Task task, Target target) {
        try {
            logger.info("Sending task to target organization {}, endpoint {}",
                    target.getOrganizationIdentifierValue(), target.getEndpointUrl());
            getFhirWebserviceClientProvider()
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
