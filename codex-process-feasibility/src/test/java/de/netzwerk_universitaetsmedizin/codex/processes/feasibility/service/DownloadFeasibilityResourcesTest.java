package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DownloadFeasibilityResourcesTest {

    @Mock
    private FhirWebserviceClientProvider clientProvider;

    @Mock
    private FhirWebserviceClient webserviceClient;

    @Mock
    private TaskHelper taskHelper;

    @Mock
    private DelegateExecution execution;

    @Mock
    private Task task;

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
    public void testDoExecute_BundleWithTooFewResultEntriesFromLocal() {
        final String measureId = "id-151003";
        final Reference measureRef = new Reference("/Measure/" + measureId);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(ArgumentMatchers.<Class<MeasureReport>>any(), eq(createSearchQueryParts(measureId))))
                .thenReturn(new Bundle());
        when(webserviceClient.getBaseUrl()).thenReturn("http://localhost");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_BundleWithTooFewResultEntriesFromRemote() {
        final String measureId = "id-151003";
        final Reference measureRef = new Reference("http://remote.host/Measure/" + measureId);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getRemoteWebserviceClient("http://remote.host"))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(ArgumentMatchers.<Class<MeasureReport>>any(), eq(createSearchQueryParts(measureId))))
                .thenReturn(new Bundle());
        when(webserviceClient.getBaseUrl()).thenReturn("http://remote.host");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_FirstBundleEntryIsNoMeasureFromLocal() {
        final String measureId = "id-151003";
        final Reference measureRef = new Reference("/Measure/" + measureId);

        final Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setResource(new Patient().setId("id-170524"));

        final Bundle.BundleEntryComponent measureEntry = new Bundle.BundleEntryComponent();
        measureEntry.setResource(new Measure().setId("id-170418"));

        final Bundle measureOnlyBundle = new Bundle()
                .addEntry(patientEntry)
                .addEntry(measureEntry);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(ArgumentMatchers.<Class<MeasureReport>>any(), eq(createSearchQueryParts(measureId))))
                .thenReturn(measureOnlyBundle);
        when(webserviceClient.getBaseUrl()).thenReturn("http://localhost");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_FirstBundleEntryIsNoMeasureFromRemmote() {
        final String measureId = "id-151003";
        final Reference measureRef = new Reference("http://remote.host/Measure/" + measureId);

        final Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setResource(new Patient().setId("id-170524"));

        final Bundle.BundleEntryComponent measureEntry = new Bundle.BundleEntryComponent();
        measureEntry.setResource(new Measure().setId("id-170418"));

        final Bundle measureOnlyBundle = new Bundle()
                .addEntry(patientEntry)
                .addEntry(measureEntry);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getRemoteWebserviceClient("http://remote.host"))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(ArgumentMatchers.<Class<MeasureReport>>any(), eq(createSearchQueryParts(measureId))))
                .thenReturn(measureOnlyBundle);
        when(webserviceClient.getBaseUrl()).thenReturn("http://remote.host");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_SecondBundleEntryIsNoLibraryFromLocal() {
        final String measureId = "id-151003";
        final Reference measureRef = new Reference("/Measure/" + measureId);

        final Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setResource(new Patient().setId("id-170524"));

        final Bundle.BundleEntryComponent measureEntry = new Bundle.BundleEntryComponent();
        measureEntry.setResource(new Measure().setId("id-170418"));

        final Bundle measureOnlyBundle = new Bundle()
                .addEntry(measureEntry)
                .addEntry(patientEntry);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(ArgumentMatchers.<Class<MeasureReport>>any(), eq(createSearchQueryParts(measureId))))
                .thenReturn(measureOnlyBundle);
        when(webserviceClient.getBaseUrl()).thenReturn("http://localhost");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_SecondBundleEntryIsNoLibraryFromRemote() {
        final String measureId = "id-151003";
        final Reference measureRef = new Reference("http://remote.host/Measure/" + measureId);

        final Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setResource(new Patient().setId("id-170524"));

        final Bundle.BundleEntryComponent measureEntry = new Bundle.BundleEntryComponent();
        measureEntry.setResource(new Measure().setId("id-170418"));

        final Bundle measureOnlyBundle = new Bundle()
                .addEntry(measureEntry)
                .addEntry(patientEntry);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getRemoteWebserviceClient("http://remote.host"))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(ArgumentMatchers.<Class<MeasureReport>>any(), eq(createSearchQueryParts(measureId))))
                .thenReturn(measureOnlyBundle);
        when(webserviceClient.getBaseUrl()).thenReturn("http://remote.host");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecuteLocal() throws Exception {
        final String measureId = "id-151003";
        final Reference measureRef = new Reference("/Measure/" + measureId);

        final Resource measure = new Measure();
        measure.setId("id-170418");
        final Bundle.BundleEntryComponent measureEntry = new Bundle.BundleEntryComponent();
        measureEntry.setResource(measure);

        final Resource library = new Library();
        library.setId("id-170912");
        final Bundle.BundleEntryComponent libraryEntry = new Bundle.BundleEntryComponent();
        libraryEntry.setResource(library);

        final Bundle measureOnlyBundle = new Bundle()
                .addEntry(measureEntry)
                .addEntry(libraryEntry);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(ArgumentMatchers.<Class<MeasureReport>>any(), eq(createSearchQueryParts(measureId))))
                .thenReturn(measureOnlyBundle);

        service.execute(execution);

        verify(execution).setVariable(VARIABLE_MEASURE, measure);
        verify(execution).setVariable(VARIABLE_LIBRARY, library);
    }

    @Test
    public void testDoExecuteRemote() throws Exception {
        final String measureId = "id-151003";
        final Reference measureRef = new Reference("http://remote.host/Measure/" + measureId);

        final Resource measure = new Measure();
        measure.setId("id-170418");
        final Bundle.BundleEntryComponent measureEntry = new Bundle.BundleEntryComponent();
        measureEntry.setResource(measure);

        final Resource library = new Library();
        library.setId("id-170912");
        final Bundle.BundleEntryComponent libraryEntry = new Bundle.BundleEntryComponent();
        libraryEntry.setResource(library);

        final Bundle measureOnlyBundle = new Bundle()
                .addEntry(measureEntry)
                .addEntry(libraryEntry);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getRemoteWebserviceClient("http://remote.host"))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(ArgumentMatchers.<Class<MeasureReport>>any(), eq(createSearchQueryParts(measureId))))
                .thenReturn(measureOnlyBundle);

        service.execute(execution);

        verify(execution).setVariable(VARIABLE_MEASURE, measure);
        verify(execution).setVariable(VARIABLE_LIBRARY, library);
    }
}
