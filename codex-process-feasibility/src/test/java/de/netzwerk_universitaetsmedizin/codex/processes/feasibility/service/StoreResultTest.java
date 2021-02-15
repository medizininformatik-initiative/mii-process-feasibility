package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_AGGREGATED_MEASURE_REPORT;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class StoreResultTest {

    @Mock
    private TaskHelper taskHelper;

    @Mock
    private Task task;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private StoreResult service;

    @Test
    public void testDoExecute() throws Exception {
        final MeasureReport measureReport = new MeasureReport();
        measureReport.setId("id-150316");

        final Task.TaskOutputComponent output = new Task.TaskOutputComponent();

        when(execution.getVariable(VARIABLE_AGGREGATED_MEASURE_REPORT)).thenReturn(measureReport);
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK)).thenReturn(task);
        when(taskHelper.createOutput(eq(CODESYSTEM_FEASIBILITY), eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE),
                any(Reference.class)))
                .thenReturn(output);

        service.execute(execution);
        verify(task).addOutput(output);
    }
}
