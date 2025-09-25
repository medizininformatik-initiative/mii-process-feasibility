package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.HEADER_PREFER;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.HEADER_PREFER_RESPOND_ASYNC;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.INITIAL_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_END;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_START;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_TYPE_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_ID;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_RESULT_CQL;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_REQUESTER_PARENT_ORGANIZATION;

public class EvaluateCQLMeasure extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(EvaluateCQLMeasure.class);

    private static final String BLAZE_EVAL_DURATION_URL = "https://samply.github.io/blaze/fhir/StructureDefinition/eval-duration";

    private final Map<String, IGenericClient> storeClients;

    private Map<String, Set<String>> networkStores;

    public EvaluateCQLMeasure(Map<String, Set<String>> networkStores, Map<String, IGenericClient> storeClients,
            ProcessPluginApi api) {
        super(api);
        this.networkStores = networkStores;
        this.storeClients = storeClients;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        Objects.requireNonNull(storeClients, "storeClients");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        logger.info("doExecute evaluate CQL measure");

        var requesterParentOrganization = variables.getString(VARIABLE_REQUESTER_PARENT_ORGANIZATION);
        variables.setInteger(VARIABLE_MEASURE_RESULT_CQL,
                executeEvaluateMeasure(requesterParentOrganization, variables));
    }

    private int executeEvaluateMeasure(String requesterParentOrganization, Variables variables) {
        var taskId = api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask());
        var start = System.currentTimeMillis();
        var errorsAndResults = Optional.ofNullable(networkStores.get(requesterParentOrganization))
                .orElse(Set.of())
                .stream()
                .filter(storeId -> storeClients.containsKey(storeId))
                .collect(Collectors.toMap(id -> id, id -> storeClients.get(id)))
                .entrySet()
                .parallelStream()
                .peek(e -> logger.debug("Start evaluating Measure {} at store '{}' [task: {}]",
                        variables.getString("%s_%s".formatted(VARIABLE_MEASURE_ID, e.getKey())), e.getKey(), taskId))
                .collect(Collectors.toConcurrentMap(e -> e.getKey(), e -> executeMeasure(e.getKey(), e.getValue(),
                        variables.getString("%s_%s".formatted(VARIABLE_MEASURE_ID, e.getKey())),
                        taskId)))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(),
                        e -> toMeasureReport(e.getValue(), e.getKey(),
                                variables.getString("%s_%s".formatted(VARIABLE_MEASURE_ID, e.getKey())),
                                taskId)))
                .entrySet()
                .stream()
                .collect(Collectors.partitioningBy(
                        e -> e.getValue().isPresent()
                                && validateMeasureReport(e.getValue().get(), e.getKey(), taskId)));

        if (errorsAndResults.getOrDefault(false, List.of()).isEmpty()) {
            return errorsAndResults.getOrDefault(true, List.of())
                    .stream()
                    .peek(e -> logger.debug("Finished evaluating Measure {} at store '{}' in {} [task: {}]",
                            e.getValue().get().getMeasure(), e.getKey(),
                            getFormattedExecutionTime(e.getValue().get(), System.currentTimeMillis() - start), taskId))
                    .map(e -> e.getValue().get().getGroupFirstRep().getPopulationFirstRep().getCount())
                    .reduce(0, (a, b) -> a + b);
        } else {
            errorsAndResults.getOrDefault(false, List.of())
                    .forEach(e -> logger.error("Failed to evaluate Measure {} at store '{}' [task: {}]",
                            variables.getString("%s_%s".formatted(VARIABLE_MEASURE_ID, e.getKey())), e.getKey(),
                            taskId));
            throw new IllegalStateException("Invalid measure report(s) [task: %s]".formatted(taskId));
        }
    }

    private Parameters executeMeasure(String storeId, IGenericClient client, String measureId, String taskId) {
        if (measureId == null || measureId.isBlank()) {
            throw new IllegalStateException(
                    "No measure id to execute for store '%s' found in execution variables [task: %s]".formatted(storeId,
                            taskId));
        } else {
            return client.operation()
                    .onInstance("Measure/" + measureId)
                    .named("evaluate-measure")
                    .withParameter(Parameters.class, "periodStart", new DateType(MEASURE_REPORT_PERIOD_START))
                    .andParameter("periodEnd", new DateType(MEASURE_REPORT_PERIOD_END))
                    .andParameter("reportType", new StringType(MEASURE_REPORT_TYPE_POPULATION))
                    .useHttpGet()
                    .preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class))
                    .withAdditionalHeader(HEADER_PREFER, HEADER_PREFER_RESPOND_ASYNC)
                    .execute();
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

    private Optional<MeasureReport> toMeasureReport(Parameters params, String storeId, String measureId,
                                                    String taskId) {
        if (params.hasParameter() && params.getParameterFirstRep().hasResource()) {
            var r = params.getParameterFirstRep().getResource();
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
                    logger.error(
                            "Failed to extract MeasureReport from Bundle returned by store '{}' for Measure {} [task: {}]",
                            storeId, measureId, taskId);
                }
                return report;
            } else if (r instanceof OperationOutcome) {
                logger.error("Evaluating Measure {} at store '{}' failed: {} [task: {}]", measureId, storeId,
                        ((OperationOutcome) r).getIssueFirstRep().getDiagnostics(), taskId);
                return Optional.empty();
            } else {
                logger.error(
                        "Response from store '{}' for Measure {} contains unexpected resource type '{}' [task: {}]",
                        storeId, measureId, r.getClass().getSimpleName(), taskId);
                return Optional.empty();
            }
        } else {
            logger.error("Response from store '{}' for Measure {} does not contain a resource [task: {}]", storeId,
                    measureId, taskId);
            return Optional.empty();
        }
    }

    private boolean validateMeasureReport(MeasureReport report, String storeId, String taskId) {
        if (!report.hasGroup()) {
            logger.error("Missing group in MeasureReport from store '{}' for Measure {} [task: {}]", storeId,
                    report.getMeasure(), taskId);
            return false;
        }
        if (!report.getGroupFirstRep().hasPopulation()) {
            logger.error("Missing population in MeasureReport from store '{}' for Measure {} [task: {}]", storeId,
                    report.getMeasure(), taskId);
            return false;
        }
        if (!report.getGroupFirstRep().getPopulationFirstRep().hasCode()) {
            logger.error("Missing population code in MeasureReport from store '{}' for Measure {} [task: {}]", storeId,
                    report.getMeasure(), taskId);
            return false;
        }
        if (!report.getGroupFirstRep().getPopulationFirstRep().getCode().hasCoding(MEASURE_POPULATION,
                INITIAL_POPULATION)) {
            logger.error("Missing initial-population code in MeasureReport from store '{}' for Measure {} [task: {}]",
                    storeId, report.getMeasure(), taskId);
            return false;
        }
        if (!report.getGroupFirstRep().getPopulationFirstRep().hasCount()) {
            logger.error("Missing population count in MeasureReport from store '{}' for Measure {} [task: {}]", storeId,
                    report.getMeasure(), taskId);
            return false;
        }
        return true;
    }
}
