package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.FlareWebserviceClient;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Period;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus.COMPLETE;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;

public class EvaluateStructuredQueryMeasure extends AbstractServiceDelegate implements InitializingBean {

    private static final String STRUCTURED_QUERY_CONTENT_TYPE = "application/json";

    private final FlareWebserviceClient flareClient;

    public EvaluateStructuredQueryMeasure(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                          FlareWebserviceClient flareClient) {
        super(clientProvider, taskHelper);
        this.flareClient = flareClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(flareClient, "flareClient");
    }

    @Override
    protected void doExecute(DelegateExecution execution) throws IOException, InterruptedException {
        var library = (Library) execution.getVariable(VARIABLE_LIBRARY);
        var measure = (Measure) execution.getVariable(VARIABLE_MEASURE);

        var structuredQuery = getStructuredQuery(library);
        var feasibility = getFeasibility(structuredQuery);
        var measureReport = buildMeasureReport(measure.getUrl(), feasibility);

        execution.setVariable(VARIABLE_MEASURE_REPORT, measureReport);
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
                        .setStart(new LocalDate(1900, 1, 1).toDate())
                        .setEnd(new LocalDate(2100, 1, 1).toDate()));

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
