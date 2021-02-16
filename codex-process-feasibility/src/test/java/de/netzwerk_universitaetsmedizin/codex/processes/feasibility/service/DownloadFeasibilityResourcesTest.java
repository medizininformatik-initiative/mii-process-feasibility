package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EnhancedFhirWebserviceClientProvider;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DownloadFeasibilityResourcesTest {

    @Mock
    private EnhancedFhirWebserviceClientProvider clientProvider;

    @Mock
    private FhirWebserviceClient webserviceClient;

    @Mock
    private TaskHelper taskHelper;

    @Mock
    private DelegateExecution execution;

    @Mock
    private Task task;

    private static final String MEASURE_ID = "id-142416";

    @InjectMocks
    private DownloadFeasibilityResources service;

    private Map<String, List<String>> createSearchQueryParts(String measureId) {
        return Map.of("_id", Collections.singletonList(measureId), "_include",
                Collections.singletonList("Measure:depends-on"));
    }

    @Test
    public void testDoExecute_NoMeasureReference() {
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK)).thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_BundleWithTooFewResultEntries() {
        IdType measureRefId = new IdType("Measure/" + MEASURE_ID);
        Reference measureRef = new Reference(measureRefId);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getWebserviceClient(measureRefId))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(new Bundle());

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_FirstBundleEntryIsNoMeasure() {
        IdType measureRefId = new IdType("Measure/" + MEASURE_ID);
        Reference measureRef = new Reference(measureRefId);
        
        Bundle bundle = new Bundle()
                .addEntry(new Bundle.BundleEntryComponent()
                        .setResource(new Patient().setId("foo")))
                .addEntry(new Bundle.BundleEntryComponent()
                        .setResource(new Patient().setId("foo")));

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getWebserviceClient(measureRefId))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(bundle);

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_SecondBundleEntryIsNoLibrary() {
        IdType measureRefId = new IdType("Measure/" + MEASURE_ID);
        Reference measureRef = new Reference(measureRefId);

        Bundle bundle = new Bundle()
                .addEntry(new Bundle.BundleEntryComponent()
                        .setResource(new Measure().setId("foo")))
                .addEntry(new Bundle.BundleEntryComponent()
                        .setResource(new Measure().setId("foo")));

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getWebserviceClient(measureRefId))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(bundle);

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute() throws Exception {
        IdType measureRefId = new IdType("Measure/" + MEASURE_ID);
        Reference measureRef = new Reference(measureRefId);

        Resource measure = new Measure().setId("foo");
        Resource library = new Library().setId("foo");
        Bundle bundle = new Bundle()
                .addEntry(new Bundle.BundleEntryComponent()
                        .setResource(measure))
                .addEntry(new Bundle.BundleEntryComponent()
                        .setResource(library));

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getWebserviceClient(measureRefId))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(bundle);

        service.execute(execution);

        verify(execution).setVariable(VARIABLE_MEASURE, measure);
        verify(execution).setVariable(VARIABLE_LIBRARY, library);
    }
}
