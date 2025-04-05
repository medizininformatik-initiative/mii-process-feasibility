package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClient;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_END;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_START;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus.COMPLETE;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;

public class EvaluateStructuredQueryMeasure implements ServiceTask, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(EvaluateStructuredQueryMeasure.class);
    private static final String STRUCTURED_QUERY_CONTENT_TYPE = "application/json";

    private final FlareWebserviceClient flareClient;

    public EvaluateStructuredQueryMeasure(FlareWebserviceClient flareClient) {
        this.flareClient = flareClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Objects.requireNonNull(flareClient, "flareClient");
    }

    @Override
    public void execute(ProcessPluginApi api, Variables variables)
            throws IOException, InterruptedException {
        Library library = variables.getFhirResource(VARIABLE_LIBRARY);
        Measure measure = variables.getFhirResource(VARIABLE_MEASURE);
        var structuredQuery = getStructuredQuery(library);

        var feasibility = getFeasibility(api, structuredQuery, measure, variables.getStartTask());

        var measureReport = buildMeasureReport(feasibility);

        variables.setFhirResource(VARIABLE_MEASURE_REPORT, measureReport);
    }

    private byte[] getStructuredQuery(Library library) {
        return library.getContent().stream()
                .filter(c -> c.getContentType().equalsIgnoreCase(STRUCTURED_QUERY_CONTENT_TYPE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("query is missing content of type " + STRUCTURED_QUERY_CONTENT_TYPE))
                .getData();
    }

    private int getFeasibility(ProcessPluginApi api, byte[] structuredQuery, Measure measure, Task task)
            throws IOException, InterruptedException {
        logger.debug("Start evaluating Measure '{}' [task: {}]", measure.getId(),
                api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));
        var startTime = System.currentTimeMillis();

        var feasibilityCount = flareClient.requestFeasibility(structuredQuery);

        var durationSeconds = (System.currentTimeMillis() - startTime) / 1000.0d;
        logger.debug("Finished evaluating Measure '{}' (total execution time: {}s) [task: {}]",
                measure.getId(), "%.3f".formatted(durationSeconds),
                api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));

        return feasibilityCount;
    }

    private MeasureReport buildMeasureReport(int feasibility) {
        var measureReport = new MeasureReport()
                .setStatus(COMPLETE)
                .setType(SUMMARY)
                .setDate(new Date())
                .setPeriod(new Period()
                        .setStart(MEASURE_REPORT_PERIOD_START)
                        .setEnd(MEASURE_REPORT_PERIOD_END));

        var populationGroup = new MeasureReportGroupPopulationComponent()
                .setCount(feasibility)
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(CODESYSTEM_MEASURE_POPULATION)
                                .setCode(CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION)));

        measureReport.getGroup()
                .add(new MeasureReportGroupComponent()
                        .setPopulation(List.of(populationGroup)));

        return measureReport;
    }
}
