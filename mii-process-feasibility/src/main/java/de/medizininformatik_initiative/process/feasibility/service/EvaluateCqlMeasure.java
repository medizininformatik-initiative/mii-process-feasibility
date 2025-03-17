package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.HEADER_PREFER;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.HEADER_PREFER_RESPOND_ASYNC;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.INITIAL_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_END;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_START;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_TYPE_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_ID;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;

public class EvaluateCqlMeasure extends AbstractServiceDelegate implements InitializingBean {

    private static final String BLAZE_EVAL_DURATION_URL = "https://samply.github.io/blaze/fhir/StructureDefinition/eval-duration";
    private static final Logger logger = LoggerFactory.getLogger(EvaluateCqlMeasure.class);

    private final IGenericClient storeClient;

    private String taskId;

    public EvaluateCqlMeasure(IGenericClient storeClient, ProcessPluginApi api) {
        super(api);

        this.storeClient = storeClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        Objects.requireNonNull(storeClient, "storeClient");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        var measureId = new IdType(variables.getString(VARIABLE_MEASURE_ID));
        taskId = api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask());

        logger.debug("Start evaluating Measure '{}' [task: {}]", measureId.getValue(), taskId);
        var start = System.currentTimeMillis();
        var response = executeEvaluateMeasure(measureId);
        var duration = System.currentTimeMillis() - start;
        var report = response.filter(Parameters::hasParameter)
                .map(Parameters::getParameterFirstRep)
                .filter(ParametersParameterComponent::hasResource)
                .map(ParametersParameterComponent::getResource)
                .flatMap(this::toMeasureReport);
        if (report.isEmpty()) {
            logger.error("Failed to evaluate measure {} [task: {}]", measureId, taskId);
            throw new RuntimeException("Failed to extract MeasureReport from response");
        } else {
            logger.debug("Finished evaluating Measure '{}' {} [task: {}]", measureId.getValue(),
                    getFormattedExecutionTime(report.get(), duration), taskId);
            validateMeasureReport(report.get());
            variables.setResource(VARIABLE_MEASURE_REPORT, report.get());
        }
    }

    private String getFormattedExecutionTime(MeasureReport report, long duration) {
        var executionTimeMessage = new StringBuilder();
        var cqlDurationExt = report.getExtensionByUrl(BLAZE_EVAL_DURATION_URL);

        if (cqlDurationExt != null && cqlDurationExt.getValue() instanceof Quantity) {
            var cqlDuration = (Quantity) cqlDurationExt.getValue();
            executionTimeMessage.append(
                    "(blaze evaluation duration: %s%s) ".formatted(cqlDuration.getValue(), cqlDuration.getUnit()));
        }
        return executionTimeMessage.append("(total execution time: %.3fs)".formatted(duration / 1000.0d)).toString();
    }

    private Optional<MeasureReport> toMeasureReport(Resource r) {
        if (r instanceof MeasureReport) {
            return Optional.of((MeasureReport) r);
        } else if (r instanceof Bundle) {
            var report = Optional.of((Bundle) r)
                    .filter(Bundle::hasEntry)
                    .map(Bundle::getEntryFirstRep)
                    .filter(Bundle.BundleEntryComponent::hasResource)
                    .map(Bundle.BundleEntryComponent::getResource)
                    .filter(MeasureReport.class::isInstance)
                    .map(MeasureReport.class::cast);
            if (report.isEmpty()) {
                logger.error("Failed to extract MeasureReport from Bundle [task: {}]", taskId);
            }
            return report;
        } else if (r instanceof OperationOutcome) {
            logger.error("Operation failed: {} [task: {}]", ((OperationOutcome) r).getIssueFirstRep().getDiagnostics(),
                    taskId);
            return Optional.empty();
        } else {
            logger.error("Response contains unexpected resource type: {} [task: {}]", r.getClass().getSimpleName(),
                    taskId);
            return Optional.empty();
        }
    }

    private Optional<Parameters> executeEvaluateMeasure(IdType measureId) {
        return Optional.ofNullable(storeClient.operation()
                .onInstance("Measure/" + measureId.getIdPart())
                .named("evaluate-measure")
                .withParameter(Parameters.class, "periodStart", new DateType(MEASURE_REPORT_PERIOD_START))
                .andParameter("periodEnd", new DateType(MEASURE_REPORT_PERIOD_END))
                .andParameter("reportType", new StringType(MEASURE_REPORT_TYPE_POPULATION))
                .useHttpGet()
                .preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class))
                .withAdditionalHeader(HEADER_PREFER, HEADER_PREFER_RESPOND_ASYNC)
                .execute());
    }

    private void validateMeasureReport(MeasureReport report) {
        if (!report.hasGroup()) {
            throw new RuntimeException("Missing MeasureReport group");
        }
        if (!report.getGroupFirstRep().hasPopulation()) {
            throw new RuntimeException("Missing MeasureReport population");
        }
        if (!report.getGroupFirstRep().getPopulationFirstRep().hasCode()) {
            throw new RuntimeException("Missing MeasureReport population code");
        }
        if (!report.getGroupFirstRep().getPopulationFirstRep().getCode().hasCoding(MEASURE_POPULATION,
                INITIAL_POPULATION)) {
            throw new RuntimeException("Missing MeasureReport initial-population code");
        }
        if (!report.getGroupFirstRep().getPopulationFirstRep().hasCount()) {
            throw new RuntimeException("Missing MeasureReport population count");
        }
    }
}
