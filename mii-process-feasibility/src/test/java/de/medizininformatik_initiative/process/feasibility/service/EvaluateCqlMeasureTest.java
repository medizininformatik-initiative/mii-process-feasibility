package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.HEADER_PREFER;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.HEADER_PREFER_RESPOND_ASYNC;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_TYPE_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_ID;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_RESULT_CQL;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_REQUESTER_PARENT_ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EvaluateCQLMeasureTest {

    private static final String BUNDLE_ID = "bundle-160328";
    private static final int POPULATION_COUNT = 134157;
    private static final String TASK_URL = "http://foo.bar/Task/1234";
    private static final String STORE_ID = "foo";
    private static final String PARENT_ORGANIZATION = "foo.bar";
    private static final String MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    private static final String INITIAL_POPULATION = "initial-population";
    private static final String MEASURE_ID = "id-145128";

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Captor ArgumentCaptor<StringType> stringTypeCaptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private IGenericClient storeClient;
    @Mock private DelegateExecution execution;
    @Mock private Variables variables;
    @Mock private IOperationUntypedWithInput<Parameters> operation;
    @Mock private ProcessPluginApi api;
    @Mock private TaskHelper taskHelper;

    private EvaluateCQLMeasure service;
    private Task task;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
        when(variables.getString("%s_%s".formatted(VARIABLE_MEASURE_ID, STORE_ID))).thenReturn(MEASURE_ID);
        when(variables.getString(VARIABLE_REQUESTER_PARENT_ORGANIZATION)).thenReturn(PARENT_ORGANIZATION);
        when(variables.getStartTask()).thenReturn(task);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(taskHelper.getLocalVersionlessAbsoluteUrl(task)).thenReturn(TASK_URL);
        when(storeClient.operation()
                .onInstance("Measure/" + MEASURE_ID)
                .named("evaluate-measure")
                .withParameter(ArgumentMatchers.<Class<org.hl7.fhir.r4.model.Parameters>>any(), eq("periodStart"),
                        any(DateType.class))
                .andParameter(eq("periodEnd"), any(DateType.class))
                .andParameter(eq("reportType"), stringTypeCaptor.capture())
                .useHttpGet()
                .preferResponseTypes(eq(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class))))
                        .thenReturn(operation);
        when(operation.withAdditionalHeader(eq(HEADER_PREFER), eq(HEADER_PREFER_RESPOND_ASYNC))).thenReturn(operation);
        service = new EvaluateCQLMeasure(Map.of(PARENT_ORGANIZATION, Set.of(STORE_ID)), Map.of(STORE_ID, storeClient),
                api);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testDoExecuteReturnsMeasureReport() {
        var report = new MeasureReport();
        var code = new CodeableConcept();
        var coding = code.getCodingFirstRep();
        var count = 1234;
        coding.setSystem(MEASURE_POPULATION);
        coding.setCode(INITIAL_POPULATION);
        report.getGroupFirstRep().getPopulationFirstRep().setCode(code).setCount(count);
        when(operation.execute())
                .thenReturn(new Parameters().addParameter(new ParametersParameterComponent().setResource(report)));

        service.doExecute(execution, variables);

        verify(variables).setInteger(ConstantsFeasibility.VARIABLE_MEASURE_RESULT_CQL, count);
        assertThat(stringTypeCaptor.getValue()).isNotNull();
        assertThat(stringTypeCaptor.getValue().getValue()).isEqualTo(MEASURE_REPORT_TYPE_POPULATION);
    }

    @Test
    void testFailsOnEmptyMeasureReport() {
        MeasureReport report = new MeasureReport().setIdentifier(List.of(new Identifier().setValue("foo")))
                .setMeasure(MEASURE_ID);
        when(operation.execute())
                .thenReturn(new Parameters()
                        .addParameter(new ParametersParameterComponent()
                                .setName("foo")
                                .setResource(report)));

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString()).contains("Missing group in MeasureReport from store '%s' for Measure %s [task: %s]"
                .formatted(STORE_ID, MEASURE_ID, TASK_URL));
    }

    @Test
    void testFailsOnMissingPopulation() {
        var report = new MeasureReport().setIdentifier(List.of(new Identifier().setValue("foo")))
                .setMeasure(MEASURE_ID);
        report.getGroupFirstRep().setCode(new CodeableConcept().setText("foo"));
        when(operation.execute())
                .thenReturn(new Parameters().addParameter(new ParametersParameterComponent().setResource(report)));

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString())
                .contains("Missing population in MeasureReport from store '%s' for Measure %s [task: %s]"
                        .formatted(STORE_ID, MEASURE_ID, TASK_URL));
    }

    @Test
    void testFailsOnMissingPopulationCode() {
        var report = new MeasureReport().setIdentifier(List.of(new Identifier().setValue("foo")))
                .setMeasure(MEASURE_ID);
        report.getGroupFirstRep().getPopulationFirstRep().setCount(0);
        when(operation.execute())
                .thenReturn(new Parameters().addParameter(new ParametersParameterComponent().setResource(report)));

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString())
                .contains("Missing population code in MeasureReport from store '%s' for Measure %s [task: %s]"
                        .formatted(STORE_ID, MEASURE_ID, TASK_URL));
    }

    @Test
    void testFailsOnWrongPopulationCode() {
        var report = new MeasureReport().setIdentifier(List.of(new Identifier().setValue("foo")))
                .setMeasure(MEASURE_ID);
        report.getGroupFirstRep().getPopulationFirstRep().setCode(new CodeableConcept().setText("foo"));
        when(operation.execute())
                .thenReturn(new Parameters().addParameter(new ParametersParameterComponent().setResource(report)));

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString())
                .contains("Missing initial-population code in MeasureReport from store '%s' for Measure %s [task: %s]"
                        .formatted(STORE_ID, MEASURE_ID, TASK_URL));
    }

    @Test
    void testFailsOnMissingPopulationCount() {
        var report = new MeasureReport().setIdentifier(List.of(new Identifier().setValue("foo")))
                .setMeasure(MEASURE_ID);
        var code = new CodeableConcept();
        var coding = code.getCodingFirstRep();
        coding.setSystem(MEASURE_POPULATION);
        coding.setCode(INITIAL_POPULATION);
        report.getGroupFirstRep().getPopulationFirstRep().setCode(code);
        when(operation.execute())
                .thenReturn(new Parameters().addParameter(new ParametersParameterComponent().setResource(report)));

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString())
                .contains("Missing population count in MeasureReport from store '%s' for Measure %s [task: %s]"
                        .formatted(STORE_ID, MEASURE_ID, TASK_URL));
    }

    @Test
    void testDoExecuteReturnsBundleContainingMeasureReport() {
        var report = new MeasureReport();
        var code = new CodeableConcept();
        var coding = code.getCodingFirstRep();
        coding.setSystem(MEASURE_POPULATION);
        coding.setCode(INITIAL_POPULATION);
        report.getGroupFirstRep().getPopulationFirstRep().setCode(code).setCount(POPULATION_COUNT);
        var bundle = new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(report));
        when(operation.execute())
                .thenReturn(new Parameters().addParameter(new ParametersParameterComponent().setResource(bundle)));

        service.doExecute(execution, variables);

        verify(variables).setInteger(VARIABLE_MEASURE_RESULT_CQL, POPULATION_COUNT);
        assertThat(stringTypeCaptor.getValue()).isNotNull();
        assertThat(stringTypeCaptor.getValue().getValue()).isEqualTo(MEASURE_REPORT_TYPE_POPULATION);
    }

    @Test
    void testFailsOnBundleIsEmpty() {
        var bundle = new Bundle().setId(BUNDLE_ID);
        when(operation.execute())
                .thenReturn(new Parameters().addParameter(new ParametersParameterComponent().setResource(bundle)));

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString())
                .contains("Failed to extract MeasureReport from Bundle returned by store '%s' for Measure %s [task: %s]"
                        .formatted(STORE_ID, MEASURE_ID, TASK_URL));
    }

    @Test
    void testFailsOnBundleContainingNonMeasureReportResource() {
        var resource = new Medication().setIdentifier(List.of(new Identifier().setValue("foo")));
        var bundle = new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(resource));
        when(operation.execute())
                .thenReturn(new Parameters().addParameter(new ParametersParameterComponent().setResource(bundle)));

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString())
                .contains("Failed to extract MeasureReport from Bundle returned by store '%s' for Measure %s [task: %s]"
                        .formatted(STORE_ID, MEASURE_ID, TASK_URL));
    }

    @Test
    void testFailsWithOperationOutcomeResource() {
        var errorMessage = "error-163935";
        var resource = new OperationOutcome()
                .addIssue(new OperationOutcome.OperationOutcomeIssueComponent().setDiagnostics(errorMessage));
        when(operation.execute())
                .thenReturn(new Parameters().addParameter(new ParametersParameterComponent().setResource(resource)));

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString())
                .contains("Evaluating Measure %s at store '%s' failed: %s [task: %s]"
                        .formatted(MEASURE_ID, STORE_ID, errorMessage, TASK_URL));
    }

    @Test
    void testFailsOnNonMeasureReportResource() {
        var resource = new Medication().setIdentifier(List.of(new Identifier().setValue("foo")));
        when(operation.execute())
                .thenReturn(new Parameters()
                        .addParameter(new ParametersParameterComponent().setResource(resource)));

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString())
                .contains("Response from store '%s' for Measure %s contains unexpected resource type '%s' [task: %s]"
                        .formatted(STORE_ID, MEASURE_ID, Medication.class.getSimpleName(), TASK_URL));
    }

    @Test
    void testFailsOnEmptyResponse() {
        when(operation.execute()).thenReturn(new Parameters());

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Invalid measure report(s) [task: %s]".formatted(TASK_URL));
        assertThat(out.toString())
                .contains("Response from store '%s' for Measure %s does not contain a resource [task: %s]"
                        .formatted(STORE_ID, MEASURE_ID, TASK_URL));
    }
}
