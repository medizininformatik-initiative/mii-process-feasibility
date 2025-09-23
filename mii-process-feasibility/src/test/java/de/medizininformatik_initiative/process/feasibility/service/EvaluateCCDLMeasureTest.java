package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClient;
import de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_REQUESTER_PARENT_ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EvaluateCCDLMeasureTest {

    private static final String TASK_URL = "http://foo.bar/task/123";
    private static final String QUERY = "query-161643";
    private static final String MEASURE_ID = "measure-161521";
    private static final String PARENT_ORGANIZATION = "foo.bar";
    private static final String STORE_ID = "foo";

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Captor ArgumentCaptor<MeasureReport> measureReportCaptor;

    @Mock private FlareWebserviceClient flareWebserviceClient;
    @Mock private DelegateExecution execution;
    @Mock private ProcessPluginApi api;
    @Mock private TaskHelper taskHelper;
    @Mock private Variables variables;
    @Mock private Task task;

    private EvaluateCCDLMeasure service;

    @BeforeEach
    public void setup() {
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(new Measure().setId(MEASURE_ID));
        when(variables.getString(VARIABLE_REQUESTER_PARENT_ORGANIZATION)).thenReturn(PARENT_ORGANIZATION);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(variables.getStartTask()).thenReturn(task);
        when(taskHelper.getLocalVersionlessAbsoluteUrl(task)).thenReturn(TASK_URL);
        service = new EvaluateCCDLMeasure(Map.of(PARENT_ORGANIZATION, Set.of(STORE_ID)),
                Map.of(STORE_ID, flareWebserviceClient), api);
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    public void testDoExecute_FailsIfStructuredQueryContentIsMissing() {
        var library = new Library();
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);
        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Library of Measure %s is missing content of type '%s' [task: %s]".formatted(MEASURE_ID,
                        "application/json", TASK_URL));
    }

    @Test
    public void testDoExecute_FailsIfFeasibilityCannotBeRequested() throws IOException, InterruptedException {
        var structuredQuery = QUERY.getBytes();

        var measure = new Measure();
        measure.setUrl("https://my-zars/Measure/a9d981ed-58dd-4213-9abf-cb86ab757162");
        var library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("application/json")
                .setData(structuredQuery)));

        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);
        when(flareWebserviceClient.requestFeasibility(structuredQuery)).thenThrow(IOException.class);

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error(s) executing feasibility query for Measure %s [task: %s]".formatted(MEASURE_ID,
                        TASK_URL));
    }

    @Test
    public void testDoExecute_FailsIfLibraryDoesNotContainStructuredQuery() {
        var structuredQuery = STORE_ID.getBytes();

        var measure = new Measure();
        measure.setUrl("https://my-zars/Measure/a9d981ed-58dd-4213-9abf-cb86ab757162");
        var library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("text/plain")
                .setData(structuredQuery)));

        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);

        assertThrows(IllegalStateException.class, () -> service.doExecute(execution, variables));
    }

    @Test
    public void testDoExecute() throws IOException, InterruptedException {
        var structuredQuery = STORE_ID.getBytes();
        var measureRef = "https://my-zars/Measure/a9d981ed-58dd-4213-9abf-cb86ab757162";

        var measure = new Measure();
        measure.setUrl(measureRef);
        var library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("application/json")
                .setData(structuredQuery)));
        var feasibility = 10;

        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);
        when(flareWebserviceClient.requestFeasibility(structuredQuery)).thenReturn(feasibility);

        service.doExecute(execution, variables);

        verify(variables).setInteger(ConstantsFeasibility.VARIABLE_MEASURE_RESULT_CCDL, feasibility);
    }
}
