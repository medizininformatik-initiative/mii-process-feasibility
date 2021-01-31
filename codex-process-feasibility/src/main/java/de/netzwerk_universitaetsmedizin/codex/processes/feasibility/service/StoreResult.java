package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

public class StoreResult extends AbstractServiceDelegate implements InitializingBean {

    public StoreResult(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper) {
        super(clientProvider, taskHelper);
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        MeasureReport aggMeasureReport = getAggregatedMeasureReport(execution);

        addMeasureReportReferenceToTask(aggMeasureReport);
    }

    private MeasureReport getAggregatedMeasureReport(DelegateExecution execution) {
        return (MeasureReport) execution.getVariable(ConstantsFeasibility.VARIABLE_AGGREGATED_MEASURE_REPORT);
    }

    private void addMeasureReportReferenceToTask(MeasureReport aggMeasureReport) {
        Task task = getCurrentTaskFromExecutionVariables();
        task.addOutput(createMeasureReportReferenceOutput(aggMeasureReport));
    }

    private Task.TaskOutputComponent createMeasureReportReferenceOutput(MeasureReport aggMeasureReport) {
        return getTaskHelper().createOutput(ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE,
                new Reference().setReference("MeasureReport/" + aggMeasureReport.getId()));
    }
}
