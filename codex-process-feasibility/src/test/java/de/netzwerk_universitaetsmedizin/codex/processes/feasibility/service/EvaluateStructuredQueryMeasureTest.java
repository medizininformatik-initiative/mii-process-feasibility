package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.FlareWebserviceClient;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.List;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus.COMPLETE;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EvaluateStructuredQueryMeasureTest {

    @Captor
    ArgumentCaptor<MeasureReport> measureReportCaptor;

    @Mock
    private FlareWebserviceClient flareWebserviceClient;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private EvaluateStructuredQueryMeasure service;

    @Test
    public void testDoExecute_FailsIfStructuredQueryContentIsMissing() {
        Measure measure = new Measure();
        Library library = new Library();
        when(execution.getVariable(VARIABLE_MEASURE)).thenReturn(measure);
        when(execution.getVariable(VARIABLE_LIBRARY)).thenReturn(library);

        assertThrows(IllegalStateException.class, () -> service.doExecute(execution));
    }

    @Test
    public void testDoExecute_FailsIfFeasibilityCannotBeRequested() throws IOException, InterruptedException {
        byte[] structuredQuery = "foo".getBytes();

        Measure measure = new Measure();
        measure.setUrl("https://my-zars/Measure/a9d981ed-58dd-4213-9abf-cb86ab757162");
        Library library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("application/json")
                .setData(structuredQuery)));
        when(execution.getVariable(VARIABLE_MEASURE)).thenReturn(measure);
        when(execution.getVariable(VARIABLE_LIBRARY)).thenReturn(library);

        when(flareWebserviceClient.requestFeasibility(structuredQuery)).thenThrow(IOException.class);

        assertThrows(IOException.class, () -> service.doExecute(execution));
    }

    @Test
    public void testDoExecute_FailsIfLibraryDoesNotContainStructuredQuery() {
        byte[] structuredQuery = "foo".getBytes();

        Measure measure = new Measure();
        measure.setUrl("https://my-zars/Measure/a9d981ed-58dd-4213-9abf-cb86ab757162");
        Library library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("text/plain")
                .setData(structuredQuery)));

        when(execution.getVariable(VARIABLE_MEASURE)).thenReturn(measure);
        when(execution.getVariable(VARIABLE_LIBRARY)).thenReturn(library);

        assertThrows(IllegalStateException.class, () -> service.doExecute(execution));
    }

    @Test
    public void testDoExecute() throws IOException, InterruptedException {
        var structuredQuery = "foo".getBytes();
        var measureRef = "https://my-zars/Measure/a9d981ed-58dd-4213-9abf-cb86ab757162";

        Measure measure = new Measure();
        measure.setUrl(measureRef);
        Library library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("application/json")
                .setData(structuredQuery)));
        when(execution.getVariable(VARIABLE_MEASURE)).thenReturn(measure);
        when(execution.getVariable(VARIABLE_LIBRARY)).thenReturn(library);

        int feasibility = 10;
        when(flareWebserviceClient.requestFeasibility(structuredQuery)).thenReturn(feasibility);
        doNothing().when(execution).setVariable(eq(VARIABLE_MEASURE_REPORT), measureReportCaptor.capture());

        service.doExecute(execution);

        var measureReport = measureReportCaptor.getValue();
        assertEquals(COMPLETE, measureReport.getStatus());
        assertEquals(SUMMARY, measureReport.getType());
        assertEquals(measureRef, measureReport.getMeasure());
        assertEquals(feasibility, measureReport.getGroup().get(0).getPopulation().get(0).getCount());
    }
}
