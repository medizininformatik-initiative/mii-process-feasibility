package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.client.dsf.PreferReturnMinimalWithRetry;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
public class DownloadFeasibilityResourcesTest {

    private static final String TASK_ID = "task-15:20:12";

    private static final String MEASURE_ID = "id-142416";

    @Mock private DsfClient client;
    @Mock private PreferReturnMinimalWithRetry minimalReturn;
    @Mock private TaskHelper taskHelper;
    @Mock private ProcessPluginApi api;
    @Mock private Variables variables;
    @Mock private Task task;
    @Mock private DsfClientProvider clientProvider;

    @InjectMocks private DownloadFeasibilityResources service;

    @BeforeEach
    public void setUp() {
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(variables.getStartTask()).thenReturn(task);
    }

    private Map<String, List<String>> createSearchQueryParts(String measureId) {
        return Map.of("_id", Collections.singletonList(measureId), "_include",
                Collections.singletonList("Measure:depends-on"));
    }

    @Test
    public void execute_NoMeasureReference(CapturedOutput output) {
        when(variables.getStartTask()).thenReturn(task);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class))
                .thenReturn(Optional.empty());
        when(taskHelper.getLocalVersionlessAbsoluteUrl(task)).thenReturn(TASK_ID);

        assertThrows(RuntimeException.class, () -> service.execute(api, variables));
        assertThat(output.getOut(),
                containsString(format("Task is missing the measure reference [task: %s]", TASK_ID)));
    }

    @Test
    public void testDoExecute_BundleWithTooFewResultEntries(CapturedOutput output) {
        var measureRefId = new IdType("http://foo.bar/fhir/Measure/" + MEASURE_ID);
        var measureRef = new Reference(measureRefId);

        when(variables.getStartTask()).thenReturn(task);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class))
                .thenReturn(Optional.of(measureRef));
        when(api.getDsfClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getDsfClient(measureRefId.getBaseUrl()))
                .thenReturn(client);
        when(client.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(new Bundle());

        assertThrows(RuntimeException.class, () -> service.execute(api, variables));
        assertThat(output.getOut(), containsString("Returned search-set contained less then two entries"));
    }

    @Test
    public void testDoExecute_FirstBundleEntryIsNoMeasure(CapturedOutput output) {
        var measureRefId = new IdType("http://foo.bar/fhir/Measure/" + MEASURE_ID);
        var measureRef = new Reference(measureRefId);

        var bundle = new Bundle();
        bundle.addEntry().setResource(new Patient().setId("foo"));
        bundle.addEntry().setResource(new Patient().setId("foo"));

        when(variables.getStartTask()).thenReturn(task);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class))
                .thenReturn(Optional.of(measureRef));
        when(api.getDsfClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getDsfClient(measureRefId.getBaseUrl()))
                .thenReturn(client);
        when(client.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(bundle);

        assertThrows(RuntimeException.class, () -> service.execute(api, variables));
        assertThat(output.getOut(), containsString("Returned search-set did not contain Measure at index 0"));
    }

    @Test
    public void testDoExecute_SecondBundleEntryIsNoLibrary(CapturedOutput output) {
        var measureRefId = new IdType("http://foo.bar/fhir/Measure/" + MEASURE_ID);
        var measureRef = new Reference(measureRefId);

        var bundle = new Bundle();
        bundle.addEntry().setResource(new Measure().setId("foo"));
        bundle.addEntry().setResource(new Measure().setId("foo"));

        when(variables.getStartTask()).thenReturn(task);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class))
                .thenReturn(Optional.of(measureRef));
        when(api.getDsfClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getDsfClient(measureRefId.getBaseUrl()))
                .thenReturn(client);
        when(client.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(bundle);

        assertThrows(RuntimeException.class, () -> service.execute(api, variables));
        assertThat(output.getOut(), containsString("Returned search-set did not contain Library at index 1"));
    }

    @Test
    public void testDoExecute() throws Exception {
        var measureRefId = new IdType("http://foo.bar/fhir/Measure/" + MEASURE_ID);
        var measureRef = new Reference(measureRefId);

        var measure = new Measure().setId("foo");
        var library = new Library().setId("foo");
        var bundle = new Bundle();
        bundle.addEntry().setResource(measure);
        bundle.addEntry().setResource(library);

        when(variables.getStartTask()).thenReturn(task);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class))
                .thenReturn(Optional.of(measureRef));
        when(api.getDsfClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getDsfClient(measureRefId.getBaseUrl()))
                .thenReturn(client);
        when(client.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(bundle);

        service.execute(api, variables);

        verify(variables).setFhirResource(VARIABLE_MEASURE, measure);
        verify(variables).setFhirResource(VARIABLE_LIBRARY, library);
    }
}
