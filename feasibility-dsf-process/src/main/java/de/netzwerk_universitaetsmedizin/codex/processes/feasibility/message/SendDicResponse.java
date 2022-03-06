package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.message;

import ca.uhn.fhir.context.FhirContext;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;

import java.util.stream.Stream;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;

public class SendDicResponse extends AbstractTaskMessageSend {

    public SendDicResponse(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                           ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
                           FhirContext fhirContext) {
        super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
    }

    @Override
    protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution) {
        String measureReportId = (String) execution.getVariable(VARIABLE_MEASURE_REPORT_ID);

        return Stream.of(getTaskHelper().createInput(CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE,
                new Reference().setReference(measureReportId)));
    }
}
