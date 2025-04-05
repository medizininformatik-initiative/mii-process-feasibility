package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;

/**
 * The type Store live result.
 */
public class StoreLiveResult implements ServiceTask {

    private static final Logger logger = LoggerFactory.getLogger(StoreLiveResult.class);

    @Override
    public void execute(ProcessPluginApi api, Variables variables) {
        var task = variables.getLatestTask();

        MeasureReport measureReport = variables.getFhirResourceLocal(VARIABLE_MEASURE_REPORT);
        // addReadAccessTag(measureReport);

        var storedMeasureReport = storeMeasureReport(api, measureReport);
        addMeasureReportReferenceToTaskOutput(api, task, storedMeasureReport.getIdElement());
        logger.info("Added measure report {} [task: {}]", storedMeasureReport.getId(), task.getId());
    }

    // private void addReadAccessTag(MeasureReport measureReport) {
    // measureReport.getMeta().getTag().removeIf(t -> !READ_ACCESS_TAG_VALUE_LOCAL.equals(t.getCode()));
    //
    // if (!api.getReadAccessHelper().hasLocal(measureReport)) {
    // api.getReadAccessHelper().addLocal(measureReport);
    // }
    // }

    private MeasureReport storeMeasureReport(ProcessPluginApi api, MeasureReport measureReport) {
        return api.getDsfClientProvider().getLocalDsfClient().create(measureReport);
    }

    private void addMeasureReportReferenceToTaskOutput(ProcessPluginApi api, Task task, IdType measureReportId) {
        task.addOutput(createMeasureReportReferenceOutput(api, measureReportId));
    }

    private TaskOutputComponent createMeasureReportReferenceOutput(ProcessPluginApi api, IdType measureReportId) {
        return api.getTaskHelper().createOutput(
                new Reference().setReference("MeasureReport/" + measureReportId.getIdPart()),
                CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE);
    }
}
