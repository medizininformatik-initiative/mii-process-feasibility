package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_SINGLE_RESULT;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.EXTENSION_DIC_URI;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DownloadMeasureReportTest {

    @Mock
    private FhirWebserviceClientProvider clientProvider;

    @Mock
    private FhirWebserviceClient localWebserviceClient;

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
        when(execution.getVariable(Mockito.eq("task"))).thenReturn(task);
        when(taskHelper.getFirstInputParameterReferenceValue(Mockito.eq(task), Mockito.eq(CODESYSTEM_FEASIBILITY),
                Mockito.eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE)))
                .thenReturn(Optional.empty());

        Assert.assertThrows(RuntimeException.class, () -> service.execute(execution));
    }

    @Test
    public void testDoExecute() throws Exception {
        when(execution.getVariable(Mockito.eq("task"))).thenReturn(task);

        final String measureReportId = "12345";
        final Reference measureReportRef = new Reference().setReference("http://localhost/MeasureReport/" + measureReportId);
        when(taskHelper.getFirstInputParameterReferenceValue(Mockito.eq(task), Mockito.eq(CODESYSTEM_FEASIBILITY),
                Mockito.eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE)))
                .thenReturn(Optional.of(measureReportRef));
        when(clientProvider.getLocalWebserviceClient())
                .thenReturn(localWebserviceClient);
        when(clientProvider.getRemoteWebserviceClient(Mockito.anyString()))
                .thenReturn(localWebserviceClient);

        final CodeableConcept coding = new CodeableConcept();
        final MeasureReport.MeasureReportGroupComponent measureReportGroup = new MeasureReport.MeasureReportGroupComponent()
                .setCode(coding);
        final MeasureReport measureReport = new MeasureReport()
                .setGroup(List.of(measureReportGroup));
        when(localWebserviceClient.read(ArgumentMatchers.<Class<MeasureReport>>any(), Mockito.eq(measureReportId)))
                .thenReturn(measureReport);
        final Reference requesterRef = new Reference().setReference("http://localhost.requester/");
        when(task.getRequester()).thenReturn(requesterRef);

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
