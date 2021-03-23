package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_MAP;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AggregateMeasureReportsTest {

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private AggregateMeasureReports service;

    @Test
    public void testDoExecute_FirstMeasureReport() throws Exception {
        Reference requester = new Reference("DIZ 1");
        Task task = new Task().setRequester(requester);

        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);

        Map<Reference, MeasureReport> measureReports = new HashMap<>();
        when(execution.getVariable(VARIABLE_MEASURE_REPORT_MAP))
                .thenReturn(measureReports);

        MeasureReport measureReport = new MeasureReport();
        when(execution.getVariable(VARIABLE_MEASURE_REPORT))
                .thenReturn(measureReport);

        service.execute(execution);

        assertEquals(1, measureReports.size());
        assertTrue(measureReports.containsKey(requester));
        assertTrue(measureReports.containsValue(measureReport));
    }

    @Test
    public void testDoExecute_AdditionalMeasureReport() throws Exception {
        Reference additionalRequester = new Reference("DIZ 2");
        Task additionalTask = new Task().setRequester(additionalRequester);
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(additionalTask);

        Reference initialRequester = new Reference("DIZ 1");
        MeasureReport initialMeasureReport = new MeasureReport();

        Map<Reference, MeasureReport> measureReports = new LinkedHashMap<>();
        measureReports.put(initialRequester, initialMeasureReport);
        when(execution.getVariable(VARIABLE_MEASURE_REPORT_MAP))
                .thenReturn(measureReports);

        MeasureReport additionalMeasureReport = new MeasureReport();
        when(execution.getVariable(VARIABLE_MEASURE_REPORT))
                .thenReturn(additionalMeasureReport);

        service.execute(execution);

        assertEquals(2, measureReports.size());
        assertTrue(measureReports.containsKey(additionalRequester));
        assertTrue(measureReports.containsValue(additionalMeasureReport));
    }
}
