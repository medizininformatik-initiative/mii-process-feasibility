package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.EnhancedFhirWebserviceClientProvider;
import de.medizininformatik_initiative.feasibility_dsf_process.service.DownloadFeasibilityResources;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR;
import static org.hl7.fhir.r4.model.Task.TaskStatus.FAILED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DownloadFeasibilityResourcesTest {

    private static final String MEASURE_ID = "id-142416";

    @Mock
    private EnhancedFhirWebserviceClientProvider clientProvider;

    @Mock
    private FhirWebserviceClient webserviceClient;

    @Mock
    private TaskHelper taskHelper;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private DownloadFeasibilityResources service;

    private Task task;
    private Task.TaskOutputComponent taskOutputComponent;

    @Before
    public void setUp() {
        task = new Task();
        taskOutputComponent = new Task.TaskOutputComponent();
    }

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
        when(taskHelper.createOutput(CODESYSTEM_HIGHMED_BPMN, CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR,
                "Process null has fatal error in step null, reason: Missing measure reference."))
                .thenReturn(taskOutputComponent);

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        assertSame(FAILED, task.getStatus());
        assertEquals(taskOutputComponent, task.getOutputFirstRep());
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
        when(clientProvider.getWebserviceClientByReference(measureRefId))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(new Bundle());
        when(taskHelper.createOutput(CODESYSTEM_HIGHMED_BPMN, CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR,
                "Process null has fatal error in step null, reason: Returned search-set contained less then two entries"))
                .thenReturn(taskOutputComponent);

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        assertSame(FAILED, task.getStatus());
        assertEquals(taskOutputComponent, task.getOutputFirstRep());
    }

    @Test
    public void testDoExecute_FirstBundleEntryIsNoMeasure() {
        IdType measureRefId = new IdType("Measure/" + MEASURE_ID);
        Reference measureRef = new Reference(measureRefId);

        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Patient().setId("foo"));
        bundle.addEntry().setResource(new Patient().setId("foo"));

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getWebserviceClientByReference(measureRefId))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(bundle);
        when(taskHelper.createOutput(CODESYSTEM_HIGHMED_BPMN, CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR,
                "Process null has fatal error in step null, reason: Returned search-set did not contain Measure at index 0"))
                .thenReturn(taskOutputComponent);

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        assertSame(FAILED, task.getStatus());
        assertEquals(taskOutputComponent, task.getOutputFirstRep());
    }

    @Test
    public void testDoExecute_SecondBundleEntryIsNoLibrary() {
        IdType measureRefId = new IdType("Measure/" + MEASURE_ID);
        Reference measureRef = new Reference(measureRefId);

        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Measure().setId("foo"));
        bundle.addEntry().setResource(new Measure().setId("foo"));

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getWebserviceClientByReference(measureRefId))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(bundle);
        when(taskHelper.createOutput(CODESYSTEM_HIGHMED_BPMN, CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR,
                "Process null has fatal error in step null, reason: Returned search-set did not contain Library at index 1"))
                .thenReturn(taskOutputComponent);

        assertThrows(RuntimeException.class, () -> service.execute(execution));
        assertSame(FAILED, task.getStatus());
        assertEquals(taskOutputComponent, task.getOutputFirstRep());
    }

    @Test
    public void testDoExecute() throws Exception {
        IdType measureRefId = new IdType("Measure/" + MEASURE_ID);
        Reference measureRef = new Reference(measureRefId);

        Resource measure = new Measure().setId("foo");
        Resource library = new Library().setId("foo");
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(measure);
        bundle.addEntry().setResource(library);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE))
                .thenReturn(Optional.of(measureRef));
        when(clientProvider.getWebserviceClientByReference(measureRefId))
                .thenReturn(webserviceClient);
        when(webserviceClient.searchWithStrictHandling(Measure.class, createSearchQueryParts(MEASURE_ID)))
                .thenReturn(bundle);

        service.execute(execution);

        verify(execution).setVariable(VARIABLE_MEASURE, measure);
        verify(execution).setVariable(VARIABLE_LIBRARY, library);
    }
}
