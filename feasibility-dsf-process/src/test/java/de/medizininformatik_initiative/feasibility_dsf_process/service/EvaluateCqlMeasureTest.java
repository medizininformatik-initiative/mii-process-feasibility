package de.medizininformatik_initiative.feasibility_dsf_process.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperation;
import de.medizininformatik_initiative.feasibility_dsf_process.service.EvaluateCqlMeasure;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
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
import java.util.Optional;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_ID;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR;
import static org.hl7.fhir.r4.model.Task.TaskStatus.FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class EvaluateCqlMeasureTest {

    private static final String CODE_SYSTEM_MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    private static final String CODE_INITIAL_POPULATION = "initial-population";
    private static final String MEASURE_ID = "id-145128";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IOperation storeOperation;

    @Mock
    private IGenericClient storeClient;

    @Mock
    private TaskHelper taskHelper;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private EvaluateCqlMeasure service;

    private final MeasureReport measureReport;
    private final Optional<String> expectedErrMsg;
    private Task task;
    private Task.TaskOutputComponent taskOutputComponent;

    @SuppressWarnings("unused")
    public EvaluateCqlMeasureTest(String name, Optional<String> expectedErrMsg, MeasureReport measureReport) {
        this.expectedErrMsg = expectedErrMsg;
        this.measureReport = measureReport;
    }

    @Before
    public void setUp() {
        task = new Task();
        taskOutputComponent = new Task.TaskOutputComponent();
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> measureReports() {
        return Arrays.asList(new Object[][]{
                {"MissingMeasureReportDate", Optional.of("Missing MeasureReport date"), new MeasureReport()},
                {"MissingMeasureReportGroup", Optional.of("Missing MeasureReport group"), new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")))},
                {"MissingMeasureReportPopulation", Optional.of("Missing MeasureReport population"), new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")))
                        .setGroup(List.of(
                        new MeasureReport.MeasureReportGroupComponent()
                                .setCode(new CodeableConcept())
                                .setMeasureScore(new Quantity(1))
                                .setStratifier(List.of(new MeasureReport.MeasureReportGroupStratifierComponent()))
                                .setPopulation(List.of(new MeasureReport.MeasureReportGroupPopulationComponent()))))},
                {"MissingMeasureReportPopulationCode", Optional.of("Missing MeasureReport population code"), new MeasureReport()
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
                {"MeasureReportWrongCoding", Optional.of("Missing MeasureReport initial-population code"), new MeasureReport()
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
                {"MissingMeasureReportPopulationCount", Optional.of("Missing MeasureReport population count"), new MeasureReport()
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
                {"ValidMeasureReport", Optional.empty(), new MeasureReport()
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
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
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

        if (expectedErrMsg.isPresent()) {
            when(taskHelper.createOutput(CODESYSTEM_HIGHMED_BPMN, CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR,
                    "Process null has fatal error in step null, reason: " + expectedErrMsg.get()))
                    .thenReturn(taskOutputComponent);

            assertThrows(RuntimeException.class, () -> service.execute(execution));
            assertSame(FAILED, task.getStatus());
            assertEquals(taskOutputComponent, task.getOutputFirstRep());
        } else {
            service.execute(execution);
            verify(execution).setVariable(VARIABLE_MEASURE_REPORT, measureReport);
        }
    }
}
