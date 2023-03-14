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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

@ExtendWith(MockitoExtension.class)
public class EvaluateCqlMeasureTest {

    private static final String CODE_SYSTEM_MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    private static final String CODE_INITIAL_POPULATION = "initial-population";
    private static final String MEASURE_ID = "id-145128";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private IOperation storeOperation;

    @Mock private IGenericClient storeClient;
    @Mock private TaskHelper taskHelper;
    @Mock private DelegateExecution execution;

    @InjectMocks private EvaluateCqlMeasure service;

    private Task task;
    private Task.TaskOutputComponent taskOutputComponent;

    @BeforeEach
    public void setUp() {
        task = new Task();
        taskOutputComponent = new Task.TaskOutputComponent();
    }

    public static Stream<Arguments> measureReports() {
        return Stream.of(
                Arguments.of("MissingMeasureReportGroup", Optional.of("Missing MeasureReport group"), new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z"))),
                        Arguments.of("MissingMeasureReportPopulation", Optional.of("Missing MeasureReport population"), new MeasureReport()
                        .setDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")))
                        .setGroup(List.of(
                         new MeasureReport.MeasureReportGroupComponent()
                                .setCode(new CodeableConcept())
                                .setMeasureScore(new Quantity(1))
                                .setStratifier(List.of(new MeasureReport.MeasureReportGroupStratifierComponent()))
                                .setPopulation(List.of(new MeasureReport.MeasureReportGroupPopulationComponent()))))),
                Arguments.of("MissingMeasureReportPopulationCode", Optional.of("Missing MeasureReport population code"), new MeasureReport()
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
                                                .setSubjectResults(new Reference("http://localhost/Patient/123"))))))),
                Arguments.of("MeasureReportWrongCoding", Optional.of("Missing MeasureReport initial-population code"), new MeasureReport()
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
                                                .setSubjectResults(new Reference("http://localhost/Patient/123"))))))),
                Arguments.of("MissingMeasureReportPopulationCount", Optional.of("Missing MeasureReport population count"), new MeasureReport()
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
                                                .setSubjectResults(new Reference("http://localhost/Patient/123"))))))),
                Arguments.of("ValidMeasureReport", Optional.empty(), new MeasureReport()
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
                                                .setSubjectResults(new Reference("http://localhost/Patient/123")))))))
                ));
    }

    @ParameterizedTest
    @MethodSource("measureReports")
    public void testDoExecute(String name, Optional<String> expectedErrMsg, MeasureReport measureReport) throws Exception {
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
