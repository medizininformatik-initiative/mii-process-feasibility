package de.medizininformatik_initiative.feasibility_dsf_process.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.springframework.beans.factory.InitializingBean;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;

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
    public StoreLiveResult(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                           ReadAccessHelper readAccessHelper) {
        super(clientProvider, taskHelper, readAccessHelper);
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        Task task = getCurrentTaskFromExecutionVariables();

        MeasureReport measureReport = getMeasureReport(execution);
        addReadAccessTag(measureReport);

        MeasureReport storedMeasureReport = storeMeasureReport(measureReport);
        addMeasureReportReferenceToTaskOutput(task, storedMeasureReport.getIdElement());

        execution.setVariable(VARIABLE_MEASURE_REPORT, storedMeasureReport);
    }

    private MeasureReport getMeasureReport(DelegateExecution execution) {
        return (MeasureReport) execution.getVariable(VARIABLE_MEASURE_REPORT);
    }

    private void addReadAccessTag(MeasureReport measureReport)
    {
        getReadAccessHelper().addLocal(measureReport);
    }

    private MeasureReport storeMeasureReport(MeasureReport measureReport) {
        return getFhirWebserviceClientProvider().getLocalWebserviceClient()
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
