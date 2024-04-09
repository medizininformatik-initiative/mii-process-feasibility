package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClient;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Period;
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

public class EvaluateStructuredQueryMeasure extends AbstractServiceDelegate implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(EvaluateStructuredQueryMeasure.class);

    private static final String STRUCTURED_QUERY_CONTENT_TYPE = "application/json";

    private final FlareWebserviceClient flareClient;

    public EvaluateStructuredQueryMeasure(FlareWebserviceClient flareClient, ProcessPluginApi api) {
        super(api);
        this.flareClient = flareClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(flareClient, "flareClient");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables)
            throws IOException, InterruptedException {
        logger.info("doExecute evaluate Structured Query measure");

        var library = (Library) variables.getResource(VARIABLE_LIBRARY);
        var measure = (Measure) variables.getResource(VARIABLE_MEASURE);

        var structuredQuery = getStructuredQuery(library);
        var feasibility = getFeasibility(structuredQuery);
        var measureReport = buildMeasureReport(measure.getUrl(), feasibility);

        variables.setResource(VARIABLE_MEASURE_REPORT, measureReport);
    }

    private byte[] getStructuredQuery(Library library) {
        return library.getContent().stream()
                .filter(c -> c.getContentType().equalsIgnoreCase(STRUCTURED_QUERY_CONTENT_TYPE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("query is missing content of type " + STRUCTURED_QUERY_CONTENT_TYPE))
                .getData();
    }

    private int getFeasibility(byte[] structuredQuery) throws IOException, InterruptedException {
        return flareClient.requestFeasibility(structuredQuery);
    }

    private MeasureReport buildMeasureReport(String measureRef, int feasibility) {
        var measureReport = new MeasureReport()
                .setStatus(COMPLETE)
                .setType(SUMMARY)
                .setDate(new Date())
                .setMeasure(measureRef)
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
