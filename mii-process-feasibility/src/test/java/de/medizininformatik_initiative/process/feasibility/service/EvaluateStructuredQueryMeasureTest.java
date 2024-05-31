package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClient;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus.COMPLETE;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EvaluateStructuredQueryMeasureTest {

    @Captor ArgumentCaptor<MeasureReport> measureReportCaptor;

    @Mock private FlareWebserviceClient flareWebserviceClient;
    @Mock private DelegateExecution execution;
    @Mock private ProcessPluginApi api;
    @Mock private Variables variables;

    @InjectMocks private EvaluateStructuredQueryMeasure service;

    @Test
    public void testDoExecute_FailsIfStructuredQueryContentIsMissing() {
        var measure = new Measure();
        var library = new Library();

        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);

        assertThrows(IllegalStateException.class, () -> service.doExecute(execution, variables));
    }

    @Test
    public void testDoExecute_FailsIfFeasibilityCannotBeRequested() throws IOException, InterruptedException {
        var structuredQuery = "foo".getBytes();

        var measure = new Measure();
        measure.setUrl("https://my-zars/Measure/a9d981ed-58dd-4213-9abf-cb86ab757162");
        var library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("application/json")
                .setData(structuredQuery)));

        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);
        when(flareWebserviceClient.requestFeasibility(structuredQuery)).thenThrow(IOException.class);

        assertThrows(IOException.class, () -> service.doExecute(execution, variables));
    }

    @Test
    public void testDoExecute_FailsIfLibraryDoesNotContainStructuredQuery() {
        var structuredQuery = "foo".getBytes();

        var measure = new Measure();
        measure.setUrl("https://my-zars/Measure/a9d981ed-58dd-4213-9abf-cb86ab757162");
        var library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("text/plain")
                .setData(structuredQuery)));

        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);

        assertThrows(IllegalStateException.class, () -> service.doExecute(execution, variables));
    }

    @Test
    public void testDoExecute() throws IOException, InterruptedException {
        var structuredQuery = "foo".getBytes();
        var measureRef = "https://my-zars/Measure/a9d981ed-58dd-4213-9abf-cb86ab757162";

        var measure = new Measure();
        measure.setUrl(measureRef);
        var library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("application/json")
                .setData(structuredQuery)));
        var feasibility = 10;

        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);
        when(flareWebserviceClient.requestFeasibility(structuredQuery)).thenReturn(feasibility);

        service.doExecute(execution, variables);

        verify(variables).setResource(eq(VARIABLE_MEASURE_REPORT), measureReportCaptor.capture());
        var measureReport = measureReportCaptor.getValue();
        assertEquals(COMPLETE, measureReport.getStatus());
        assertEquals(SUMMARY, measureReport.getType());
        assertEquals(measureRef, measureReport.getMeasure());
        assertEquals(feasibility, measureReport.getGroup().get(0).getPopulation().get(0).getCount());
    }
}
