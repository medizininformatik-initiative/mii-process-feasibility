package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Optional;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;

public class StoreMeasureReport extends AbstractServiceDelegate implements InitializingBean
{

    private static final Logger logger = LoggerFactory.getLogger(StoreMeasureReport.class);

    public StoreMeasureReport(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        var task = variables.getStartTask();
        MeasureReport measureReport = variables.getResource(VARIABLE_MEASURE_REPORT);

        addReadAccessTag(measureReport, task);
        referenceZarsMeasure(measureReport, task);
        stripEvaluatedResources(measureReport);

        var measureReportId = storeMeasureReport(measureReport);
        logger.debug("Stored MeasureReport resource: {} [task: {}]",
                api.getFhirContext().newJsonParser().encodeResourceToString(measureReport),
                api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));

        addMeasureReportReferenceToTaskOutputs(task, measureReportId.getValue());
        variables.updateTask(task);
        variables.setString(VARIABLE_MEASURE_REPORT_ID, measureReportId.getValue());
    }

    private void addMeasureReportReferenceToTaskOutputs(Task task, String measureReportId) {
        task.getOutput().add(api.getTaskHelper().createOutput(new Reference().setReference(measureReportId),
                CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE));
    }

    private void addReadAccessTag(MeasureReport measureReport, Task task)
    {
        var identifier = task.getRequester().getIdentifier().getValue();
        api.getReadAccessHelper().addOrganization(measureReport, identifier);
    }

    private void referenceZarsMeasure(MeasureReport measureReport, Task task) {
        Optional<Reference> measureRef = api.getTaskHelper()
                .getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                        CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class);

        if (measureRef.isPresent()) {
            measureReport.setMeasure(measureRef.get().getReference());
        } else {
            logger.error("Task is missing the measure reference [task: {}]",
                    api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));
            throw new RuntimeException("Missing measure reference.");
        }
    }

    private void stripEvaluatedResources(MeasureReport measureReport) {
        measureReport.setEvaluatedResource(List.of());
    }

    private IdType storeMeasureReport(MeasureReport measureReport) {
        return api.getFhirWebserviceClientProvider()
                .getLocalWebserviceClient()
                .withMinimalReturn()
                .create(measureReport);
    }
}
