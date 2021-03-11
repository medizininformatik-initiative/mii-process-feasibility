package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;

/**
 * The type Store live result.
 */
public class StoreLiveResult extends AbstractServiceDelegate implements InitializingBean {

    /**
     * Instantiates a new Store live result.
     *
     * @param clientProvider the client provider
     * @param taskHelper     the task helper
     */
    public StoreLiveResult(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper) {
        super(clientProvider, taskHelper);
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        MeasureReport measureReport = getMeasureReport(execution);
        Task task = getCurrentTaskFromExecutionVariables();
        IdType measureReportId = storeMeasureReport(measureReport);
        addMeasureReportReferenceToTaskOutput(task, measureReportId);
    }

    private MeasureReport getMeasureReport(DelegateExecution execution) {
        return (MeasureReport) execution.getVariable(VARIABLE_MEASURE_REPORT);
    }

    private IdType storeMeasureReport(MeasureReport measureReport) {
        measureReport.setMeta(
                new Meta().setTag(
                        List.of(new Coding()
                                .setSystem("http://highmed.org/fhir/CodeSystem/authorization-role")
                                .setCode("LOCAL"))
                )
        );
        return getFhirWebserviceClientProvider().getLocalWebserviceClient()
                .withMinimalReturn()
                .create(measureReport);
    }

    private void addMeasureReportReferenceToTaskOutput(Task task, IdType measureReportId) {
        task.addOutput(createMeasureReportReferenceOutput(measureReportId));
    }

    private TaskOutputComponent createMeasureReportReferenceOutput(IdType measureReportId) {
        return getTaskHelper().createOutput(CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE,
                new Reference().setReference("MeasureReport/" + measureReportId.getIdPart()));
    }
}
