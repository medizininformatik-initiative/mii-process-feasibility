package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static dev.dsf.fhir.authorization.read.ReadAccessHelper.READ_ACCESS_TAG_VALUE_LOCAL;

/**
 * The type Store live result.
 */
public class StoreLiveResult extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(StoreLiveResult.class);

    /**
     * Instantiates a new Store live result.
     *
     * @param api
     * @param clientProvider the client provider
     * @param taskHelper the task helper
     */
    public StoreLiveResult(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        var task = variables.getLatestTask();

        var measureReport = (MeasureReport) execution.getVariableLocal(VARIABLE_MEASURE_REPORT);
        addReadAccessTag(measureReport);

        var storedMeasureReport = storeMeasureReport(measureReport);
        addMeasureReportReferenceToTaskOutput(task, storedMeasureReport.getIdElement());
        logger.info("Added measure report {} [task: {}]", storedMeasureReport.getId(), task.getId());
    }

    private void addReadAccessTag(MeasureReport measureReport) {
        measureReport.getMeta().getTag().removeIf(t -> !READ_ACCESS_TAG_VALUE_LOCAL.equals(t.getCode()));

        if (!api.getReadAccessHelper().hasLocal(measureReport)) {
            api.getReadAccessHelper().addLocal(measureReport);
        }
    }

    private MeasureReport storeMeasureReport(MeasureReport measureReport) {
        return api.getFhirWebserviceClientProvider().getLocalWebserviceClient()
                .create(measureReport);
    }

    private void addMeasureReportReferenceToTaskOutput(Task task, IdType measureReportId) {
        task.addOutput(createMeasureReportReferenceOutput(measureReportId));
    }

    private TaskOutputComponent createMeasureReportReferenceOutput(IdType measureReportId) {
        return api.getTaskHelper().createOutput(
                new Reference().setReference("MeasureReport/" + measureReportId.getIdPart()),
                CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE);
    }
}
