package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
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
public class DownloadMeasureReportTest {

    private static final String MEASURE_REPORT_ID = "id-144911";

    @Mock
    private FhirWebserviceClientProvider clientProvider;

    @Mock
    private FhirWebserviceClient webserviceClient;

    @Mock
    private TaskHelper taskHelper;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private DownloadMeasureReport service;

    private Task task;

    @Before
    public void setUp() {
        task = new Task();
    }

    @Test
    public void testDoExecute_MissingMeasureReportReference() {
        Task.TaskOutputComponent taskOutputComponent = new Task.TaskOutputComponent();
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE))
                .thenReturn(Optional.empty());
        when(taskHelper.createOutput(CODESYSTEM_HIGHMED_BPMN, CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR,
                "Process null has fatal error in step null, reason: Missing measure report reference."))
                .thenReturn(taskOutputComponent);

        assertThrows(RuntimeException.class, () -> service.execute(execution));

        assertSame(FAILED, task.getStatus());
        assertEquals(taskOutputComponent, task.getOutputFirstRep());
    }

    @Test
    public void testDoExecuteLocal() throws Exception {
        Reference requesterRef = new Reference().setReference("http://localhost");
        task.setRequester(requesterRef);
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);

        Reference measureReportRef = new Reference().setReference("MeasureReport/" + MEASURE_REPORT_ID);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE))
                .thenReturn(Optional.of(measureReportRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(webserviceClient);

        CodeableConcept coding = new CodeableConcept();
        MeasureReport.MeasureReportGroupComponent measureReportGroup = new MeasureReport.MeasureReportGroupComponent()
                .setCode(coding);
        MeasureReport measureReport = new MeasureReport()
                .setGroup(List.of(measureReportGroup));
        when(webserviceClient.read(MeasureReport.class, MEASURE_REPORT_ID))
                .thenReturn(measureReport);

        service.execute(execution);

        verify(execution).setVariable(VARIABLE_MEASURE_REPORT, measureReport);
    }

    @Test
    public void testDoExecuteRemote() throws Exception {
        Reference requesterRef = new Reference().setReference("http://remote.host");
        task.setRequester(requesterRef);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);

        Reference measureReportRef = new Reference().setReference("http://remote.host/MeasureReport/" + MEASURE_REPORT_ID);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE))
                .thenReturn(Optional.of(measureReportRef));
        when(clientProvider.getRemoteWebserviceClient("http://remote.host"))
                .thenReturn(webserviceClient);

        CodeableConcept coding = new CodeableConcept();
        MeasureReport.MeasureReportGroupComponent measureReportGroup = new MeasureReport.MeasureReportGroupComponent()
                .setCode(coding);
        MeasureReport measureReport = new MeasureReport()
                .setGroup(List.of(measureReportGroup));
        when(webserviceClient.read(MeasureReport.class, MEASURE_REPORT_ID))
                .thenReturn(measureReport);

        service.execute(execution);

        verify(execution).setVariable(VARIABLE_MEASURE_REPORT, measureReport);
    }
}
