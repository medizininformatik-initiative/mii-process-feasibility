package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;

public class DownloadMeasureReport implements ServiceTask {

    private static final Logger logger = LoggerFactory.getLogger(DownloadMeasureReport.class);

    @Override
    public void execute(ProcessPluginApi api, Variables variables) {
        var task = variables.getLatestTask();
        var measureReportId = getMeasureReportId(api, task);
        var client = api.getDsfClientProvider()
                .getDsfClient(measureReportId.getBaseUrl());
        var measureReport = downloadMeasureReport(api, client, measureReportId, task);
        variables.setFhirResourceLocal(VARIABLE_MEASURE_REPORT, measureReport);
    }

    private MeasureReport downloadMeasureReport(ProcessPluginApi api, DsfClient client, IdType measureReportId,
                                                Task task) {
        logger.debug("Download MeasureReport with ID {} from {} [task: {}]", measureReportId.getIdPart(),
                client.getBaseUrl(), api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));
        return client.read(MeasureReport.class, measureReportId.getIdPart());
    }

    private IdType getMeasureReportId(ProcessPluginApi api, Task task) {
        Optional<Reference> measureRef = api.getTaskHelper()
                .getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                        CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE, Reference.class);
        if (measureRef.isPresent()) {
            return new IdType(measureRef.get().getReference());
        } else {
            logger.error("Task is missing the measure report reference [task: {}]",
                    api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));
            throw new RuntimeException("Missing measure report reference.");
        }
    }
}
