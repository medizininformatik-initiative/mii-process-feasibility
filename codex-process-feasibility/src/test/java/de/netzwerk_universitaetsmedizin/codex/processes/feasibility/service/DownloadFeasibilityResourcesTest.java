package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumeration;
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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DownloadFeasibilityResourcesTest {

    @Mock
    private FhirWebserviceClientProvider clientProvider;

    @Mock
    private FhirWebserviceClient localWebserviceClient;

    @Mock
    private TaskHelper taskHelper;

    @Mock
    private DelegateExecution execution;

    @Mock
    private Task task;

    @InjectMocks
    private DownloadFeasibilityResources service;

    @Test
    public void testDoExecute_NoMeasureReference() {
        when(execution.getVariable(Mockito.eq("task"))).thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_BundleWithTooFewResultEntries() {
        final Reference measureRef = new Reference("http://localhost/Measure/id-151003");

        final Bundle measureOnlyBundle = new Bundle(new Enumeration<>(new Bundle.BundleTypeEnumFactory()));
        measureOnlyBundle.setType(Bundle.BundleType.BATCHRESPONSE);

        when(execution.getVariable(Mockito.eq("task")))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(localWebserviceClient);
        when(clientProvider.getRemoteWebserviceClient(Mockito.anyString()))
                .thenReturn(localWebserviceClient);
        when(localWebserviceClient.searchWithStrictHandling(Mockito.any(), Mockito.anyMap()))
                .thenReturn(measureOnlyBundle);
        when(localWebserviceClient.getBaseUrl()).thenReturn("http://localhost");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_FirstBundleEntryIsNoMeasure() {
        final Reference measureRef = new Reference("http://localhost/Measure/id-151003");

        final Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setId("id-165214");
        patientEntry.setResource(new Patient().setId("id-170524"));

        final Bundle.BundleEntryComponent measureEntry = new Bundle.BundleEntryComponent();
        measureEntry.setId("id-151003");
        measureEntry.setResource(new Measure().setId("id-170418"));

        final Bundle measureOnlyBundle = new Bundle(new Enumeration<>(new Bundle.BundleTypeEnumFactory()));
        measureOnlyBundle.setType(Bundle.BundleType.BATCHRESPONSE);
        measureOnlyBundle.addEntry(patientEntry);
        measureOnlyBundle.addEntry(measureEntry);

        when(execution.getVariable(Mockito.eq("task")))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(localWebserviceClient);
        when(clientProvider.getRemoteWebserviceClient(Mockito.anyString()))
                .thenReturn(localWebserviceClient);
        when(localWebserviceClient.searchWithStrictHandling(Mockito.any(), Mockito.anyMap()))
                .thenReturn(measureOnlyBundle);
        when(localWebserviceClient.getBaseUrl()).thenReturn("http://localhost");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute_SecondBundleEntryIsNoLibrary() {
        final Reference measureRef = new Reference("http://localhost/Measure/id-151003");

        final Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setId("id-165214");
        patientEntry.setResource(new Patient().setId("id-170524"));

        final Bundle.BundleEntryComponent measureEntry = new Bundle.BundleEntryComponent();
        measureEntry.setId("id-151003");
        measureEntry.setResource(new Measure().setId("id-170418"));

        final Bundle measureOnlyBundle = new Bundle(new Enumeration<>(new Bundle.BundleTypeEnumFactory()));
        measureOnlyBundle.setType(Bundle.BundleType.BATCHRESPONSE);
        measureOnlyBundle.addEntry(measureEntry);
        measureOnlyBundle.addEntry(patientEntry);

        when(execution.getVariable(Mockito.eq("task")))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(localWebserviceClient);
        when(clientProvider.getRemoteWebserviceClient(Mockito.anyString()))
                .thenReturn(localWebserviceClient);
        when(localWebserviceClient.searchWithStrictHandling(Mockito.any(), Mockito.anyMap()))
                .thenReturn(measureOnlyBundle);
        when(localWebserviceClient.getBaseUrl()).thenReturn("http://localhost");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        verify(task).setStatus(Task.TaskStatus.FAILED);
    }

    @Test
    public void testDoExecute() throws Exception {
        final Reference measureRef = new Reference("http://localhost/Measure/id-151003");

        final Resource measure = new Measure();
        measure.setId("id-170418");
        final Bundle.BundleEntryComponent measureEntry = new Bundle.BundleEntryComponent();
        measureEntry.setId("id-151003");
        measureEntry.setResource(measure);

        final Resource library = new Library();
        library.setId("id-170912");
        final Bundle.BundleEntryComponent libraryEntry = new Bundle.BundleEntryComponent();
        libraryEntry.setId("id-170854");
        libraryEntry.setResource(library);

        final Bundle measureOnlyBundle = new Bundle(new Enumeration<>(new Bundle.BundleTypeEnumFactory()));
        measureOnlyBundle.setType(Bundle.BundleType.BATCHRESPONSE);
        measureOnlyBundle.addEntry(measureEntry);
        measureOnlyBundle.addEntry(libraryEntry);

        when(execution.getVariable(Mockito.eq("task")))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(localWebserviceClient);
        when(clientProvider.getRemoteWebserviceClient(Mockito.anyString()))
                .thenReturn(localWebserviceClient);
        when(localWebserviceClient.searchWithStrictHandling(Mockito.any(), Mockito.anyMap()))
                .thenReturn(measureOnlyBundle);
        when(localWebserviceClient.getBaseUrl()).thenReturn("http://localhost");
        
        service.execute(execution);

        verify(execution).setVariable(VARIABLE_MEASURE, measure);
        verify(execution).setVariable(VARIABLE_LIBRARY, library);
    }
}
