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

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_MAP;

/**
 * The type Aggregate measure reports
 */
public class AggregateMeasureReports extends AbstractServiceDelegate implements InitializingBean {

    /**
     * Instantiates a new Aggregate measure reports.
     *
     * @param clientProvider the client provider
     * @param taskHelper     the task helper
     */
    public AggregateMeasureReports(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper) {
        super(clientProvider, taskHelper);
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        Task task = getCurrentTaskFromExecutionVariables();
        Map<Reference, MeasureReport> measureReports = getMeasureReportMap(execution);
        measureReports.put(task.getRequester(), getMeasureReport(execution));
        execution.setVariable(VARIABLE_MEASURE_REPORT_MAP, measureReports);
    }

    private MeasureReport getMeasureReport(DelegateExecution execution) {
        return (MeasureReport) execution.getVariable(VARIABLE_MEASURE_REPORT);
    }

    private Map<Reference, MeasureReport> getMeasureReportMap(DelegateExecution execution) {
        Map<Reference, MeasureReport> measureReports = (Map<Reference, MeasureReport>) execution.getVariable(VARIABLE_MEASURE_REPORT_MAP);
        return measureReports != null ? measureReports : new HashMap<>();
    }
}
