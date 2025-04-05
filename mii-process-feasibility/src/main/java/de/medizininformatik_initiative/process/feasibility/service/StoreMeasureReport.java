package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.INITIAL_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;

public class StoreMeasureReport implements ServiceTask
{

    private static final Logger logger = LoggerFactory.getLogger(StoreMeasureReport.class);

    @Override
    public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception {
        var task = variables.getStartTask();
        MeasureReport measureReport = variables.getFhirResource(VARIABLE_MEASURE_REPORT);

        addReadAccessTag(api, measureReport, task);
        referenceZarsMeasure(api, measureReport, task);
        stripEvaluatedResources(measureReport);

        var measureReportId = storeMeasureReport(api, measureReport);
        logger.debug("Stored MeasureReport '{}' (initial population count: {}) [task: {}]", measureReportId.getValue(),
                getPopulation(measureReport),
                api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));

        addMeasureReportReferenceToTaskOutputs(api, task, measureReportId.getValue());
        variables.updateTask(task);
        variables.setString(VARIABLE_MEASURE_REPORT_ID, measureReportId.getValue());
    }

    private Integer getPopulation(MeasureReport measureReport) {
        return measureReport.getGroup().stream()
                .filter(MeasureReportGroupComponent::hasPopulation)
                .map(MeasureReportGroupComponent::getPopulation)
                .flatMap(List::stream)
                .filter(p -> p.hasCode() && p.getCode().hasCoding(MEASURE_POPULATION, INITIAL_POPULATION))
                .filter(MeasureReportGroupPopulationComponent::hasCount)
                .findFirst()
                .map(MeasureReportGroupPopulationComponent::getCount)
                .orElse(0);
    }

    private void addMeasureReportReferenceToTaskOutputs(ProcessPluginApi api, Task task, String measureReportId) {
        task.getOutput().add(api.getTaskHelper().createOutput(new Reference().setReference(measureReportId),
                CODESYSTEM_FEASIBILITY, CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE));
    }

    private void addReadAccessTag(ProcessPluginApi api, MeasureReport measureReport, Task task)
    {
        var identifier = task.getRequester().getIdentifier().getValue();
        api.getReadAccessHelper().addOrganization(measureReport, identifier);
    }

    private void referenceZarsMeasure(ProcessPluginApi api, MeasureReport measureReport, Task task) {
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

    private IdType storeMeasureReport(ProcessPluginApi api, MeasureReport measureReport) {
        return api.getDsfClientProvider()
                .getLocalDsfClient()
                .withMinimalReturn()
                .create(measureReport);
    }
}
