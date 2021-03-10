package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

import java.util.HashMap;
import java.util.Map;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.EXTENSION_DIC_URI;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_MAP;


/**
 * The type prepare query for further evaluation
 */
public class PrepareForFurtherEvaluation extends AbstractServiceDelegate implements InitializingBean {


    /**
     * Instantiates a new Prepare for further evaluation.
     *
     * @param clientProvider the client provider
     * @param taskHelper     the task helper
     */
    public PrepareForFurtherEvaluation(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper) {
        super(clientProvider, taskHelper);
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        for (Map.Entry<Reference, MeasureReport> entry : getMeasureReportMap(execution).entrySet()) {
            getLeadingTaskFromExecutionVariables().addOutput(
                    addOrganizationReference(createMeasureReportReferenceOutput(entry.getValue()), entry.getKey()));
        }
    }

    private Map<Reference, MeasureReport> getMeasureReportMap(DelegateExecution execution) {
        Map<Reference, MeasureReport> measureReports = (Map<Reference, MeasureReport>) execution.getVariable(VARIABLE_MEASURE_REPORT_MAP);
        return measureReports != null ? measureReports : new HashMap<>();
    }

    private Task.TaskOutputComponent createMeasureReportReferenceOutput(MeasureReport measureReport) {
        return getTaskHelper().createOutput(CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE,
                new Reference().setReference("MeasureReport/" + measureReport.getIdElement().getIdPart()));
    }

    private Task.TaskOutputComponent addOrganizationReference(Task.TaskOutputComponent output, Reference organization) {
        output.addExtension(EXTENSION_DIC_URI, organization);
        return output;
    }
}
