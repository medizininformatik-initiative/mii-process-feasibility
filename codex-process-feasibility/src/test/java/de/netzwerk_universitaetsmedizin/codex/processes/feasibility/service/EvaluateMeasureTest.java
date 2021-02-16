package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperation;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_ID;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class EvaluateMeasureTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IOperation storeOperation;

    @Mock
    private IGenericClient storeClient;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private EvaluateMeasure service;

    private final MeasureReport measureReport;
    private final boolean expectedToFail;
    private static final String CODE_SYSTEM_MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    private static final String CODE_INITIAL_POPULATION = "initial-population";
    private static final String MEASURE_ID = "id-145128";

    @SuppressWarnings("unused")
    public EvaluateMeasureTest(String name, boolean expectedToFail, MeasureReport measureReport) {
        this.expectedToFail = expectedToFail;
        this.measureReport = measureReport;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> measureReports() {
        return Arrays.asList(new Object[][]{
                {"MissingMeasureReportDate", true, new MeasureReport()},
                {"MissingMeasureReportGroup", true, new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")))},
                {"MissingMeasureReportPopulation", true, new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")))
                        .setGroup(List.of(
                        new MeasureReport.MeasureReportGroupComponent()
                                .setCode(new CodeableConcept())
                                .setMeasureScore(new Quantity(1))
                                .setStratifier(List.of(new MeasureReport.MeasureReportGroupStratifierComponent()))
                                .setPopulation(List.of(new MeasureReport.MeasureReportGroupPopulationComponent()))))},
                {"MissingMeasureReportPopulationCode", true, new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")))
                        .setGroup(List.of(
                        new MeasureReport.MeasureReportGroupComponent()
                                .setCode(new CodeableConcept())
                                .setMeasureScore(new Quantity(1))
                                .setStratifier(List.of(new MeasureReport.MeasureReportGroupStratifierComponent()))
                                .setPopulation(List.of(
                                        new MeasureReport.MeasureReportGroupPopulationComponent()
                                                .setCode(new CodeableConcept())
                                                .setCount(0)
                                                .setSubjectResults(new Reference("http://localhost/Patient/123"))))))},
                {"MeasureReportWrongCoding", true, new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")))
                        .setGroup(List.of(
                        new MeasureReport.MeasureReportGroupComponent()
                                .setCode(new CodeableConcept())
                                .setMeasureScore(new Quantity(1))
                                .setStratifier(List.of(new MeasureReport.MeasureReportGroupStratifierComponent()))
                                .setPopulation(List.of(
                                        new MeasureReport.MeasureReportGroupPopulationComponent()
                                                .setCode(new CodeableConcept().setCoding(List.of(
                                                        new Coding(CODE_SYSTEM_MEASURE_POPULATION, "something", "foo")
                                                )))
                                                .setCount(0)
                                                .setSubjectResults(new Reference("http://localhost/Patient/123"))))))},
                {"MissingMeasureReportPopulationCount", true, new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")))
                        .setGroup(List.of(
                        new MeasureReport.MeasureReportGroupComponent()
                                .setCode(new CodeableConcept())
                                .setMeasureScore(new Quantity(1))
                                .setStratifier(List.of(new MeasureReport.MeasureReportGroupStratifierComponent()))
                                .setPopulation(List.of(
                                        new MeasureReport.MeasureReportGroupPopulationComponent()
                                                .setCode(new CodeableConcept().setCoding(List.of(
                                                        new Coding(CODE_SYSTEM_MEASURE_POPULATION, CODE_INITIAL_POPULATION, "foo")
                                                )))
                                                .setSubjectResults(new Reference("http://localhost/Patient/123"))))))},
                {"ValidMeasureReport", false, new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")))
                        .setGroup(List.of(
                        new MeasureReport.MeasureReportGroupComponent()
                                .setCode(new CodeableConcept())
                                .setMeasureScore(new Quantity(1))
                                .setStratifier(List.of(new MeasureReport.MeasureReportGroupStratifierComponent()))
                                .setPopulation(List.of(
                                        new MeasureReport.MeasureReportGroupPopulationComponent()
                                                .setCode(new CodeableConcept().setCoding(List.of(
                                                        new Coding(CODE_SYSTEM_MEASURE_POPULATION, CODE_INITIAL_POPULATION, "foo")
                                                )))
                                                .setCount(0)
                                                .setSubjectResults(new Reference("http://localhost/Patient/123"))))))}
        });
    }

    @Test
    public void testDoExecute() throws Exception {
        when(execution.getVariable(VARIABLE_MEASURE_ID))
                .thenReturn(MEASURE_ID);
        when(storeClient.operation())
                .thenReturn(storeOperation);
        when(storeOperation.onInstance("Measure/" + MEASURE_ID)
                .named("evaluate-measure")
                .withParameter(ArgumentMatchers.<Class<org.hl7.fhir.r4.model.Parameters>>any(), eq("periodStart"), any(DateType.class))
                .andParameter(eq("periodEnd"), any(DateType.class))
                .useHttpGet()
                .returnResourceType(MeasureReport.class)
                .execute())
                .thenReturn(measureReport);

        if (expectedToFail) {
            assertThrows(RuntimeException.class, () -> service.doExecute(execution));
        } else {
            service.execute(execution);
            verify(execution).setVariable(VARIABLE_MEASURE_REPORT, measureReport);
        }
    }
}
