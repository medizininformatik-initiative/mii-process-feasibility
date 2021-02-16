package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_SINGLE_RESULT;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.EXTENSION_DIC_URI;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DownloadMeasureReportTest {

    @Mock
    private FhirWebserviceClientProvider clientProvider;

    @Mock
    private FhirWebserviceClient webserviceClient;

    @Mock
    private TaskHelper taskHelper;

    @Mock
    private Task task;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private DownloadMeasureReport service;

    @Test
    public void testDoExecute_MissingMeasureReportReference() {
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE))
                .thenReturn(Optional.empty());
        when(task.getId())
                .thenReturn("foo");

        assertThrows(RuntimeException.class, () -> service.execute(execution));
    }

    @Test
    public void testDoExecuteLocal() throws Exception {
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);

        String measureReportId = "foo";
        Reference measureReportRef = new Reference().setReference("MeasureReport/" + measureReportId);
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
        when(webserviceClient.read(MeasureReport.class, measureReportId))
                .thenReturn(measureReport);
        Reference requesterRef = new Reference().setReference("http://localhost");
        when(task.getRequester())
                .thenReturn(requesterRef);

        service.execute(execution);
        verify(execution).setVariable(VARIABLE_MEASURE_REPORT, measureReport);
        assertEquals(1, coding.getCoding().size());
        assertEquals(CODESYSTEM_FEASIBILITY, coding.getCoding().get(0).getSystem());
        assertEquals(CODESYSTEM_FEASIBILITY_VALUE_SINGLE_RESULT, coding.getCoding().get(0).getCode());
        assertEquals(1, measureReportGroup.getExtension().size());
        assertEquals(EXTENSION_DIC_URI, measureReportGroup.getExtension().get(0).getUrl());
        assertEquals(requesterRef, measureReportGroup.getExtension().get(0).getValue());
    }

    @Test
    public void testDoExecuteRemote() throws Exception {
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);

        String measureReportId = "foo";
        Reference measureReportRef = new Reference().setReference("http://remote.host/MeasureReport/" + measureReportId);
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
        when(webserviceClient.read(MeasureReport.class, measureReportId))
                .thenReturn(measureReport);
        Reference requesterRef = new Reference().setReference("http://remote.host");
        when(task.getRequester())
                .thenReturn(requesterRef);

        service.execute(execution);
        verify(execution).setVariable(VARIABLE_MEASURE_REPORT, measureReport);
        assertEquals(1, coding.getCoding().size());
        assertEquals(CODESYSTEM_FEASIBILITY, coding.getCoding().get(0).getSystem());
        assertEquals(CODESYSTEM_FEASIBILITY_VALUE_SINGLE_RESULT, coding.getCoding().get(0).getCode());
        assertEquals(1, measureReportGroup.getExtension().size());
        assertEquals(EXTENSION_DIC_URI, measureReportGroup.getExtension().get(0).getUrl());
        assertEquals(requesterRef, measureReportGroup.getExtension().get(0).getValue());
    }
}
