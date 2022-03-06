package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.EXTENSION_DIC_URI;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_MAP;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_LEADING_TASK;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PrepareForFurtherEvaluationTest {

    @Captor
    private ArgumentCaptor<Reference> refCaptor;

    @Mock
    private DelegateExecution execution;

    @Mock
    private TaskHelper taskHelper;

    @InjectMocks
    private PrepareForFurtherEvaluation service;

    private Task task;

    @Before
    public void setUp() {
        task = new Task();
    }

    @Test
    public void testDoExecute_NoMeasureReports() throws Exception {
        when(execution.getVariable(VARIABLE_MEASURE_REPORT_MAP))
                .thenReturn(new HashMap<>());

        service.execute(execution);

        assertEquals(0, task.getOutput().size());
    }

    @Test
    public void testDoExecute_MultipleMeasureReports() throws Exception {
        Reference orga1Ref = new Reference().setReference("Organization 1");
        Reference orga2Ref = new Reference().setReference("Organization 2");
        String orga1ReportId = "06a13485-b3ef-413c-81e8-a53ab3f46e3d";
        MeasureReport orga1Report = new MeasureReport();
        orga1Report.setIdElement(new IdType(orga1ReportId));
        String orga2ReportId = "89620db8-6a1b-4223-8d35-2a52bbbfa461";
        MeasureReport orga2Report = new MeasureReport();
        orga2Report.setIdElement(new IdType(orga2ReportId));

        Map<Reference, MeasureReport> measureReportMapping = new LinkedHashMap<>();
        measureReportMapping.put(orga1Ref, orga1Report);
        measureReportMapping.put(orga2Ref, orga2Report);

        when(execution.getVariable(VARIABLE_MEASURE_REPORT_MAP))
                .thenReturn(measureReportMapping);
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_LEADING_TASK))
                .thenReturn(task);

        TaskOutputComponent orga1Output = new TaskOutputComponent();
        TaskOutputComponent orga2Output = new TaskOutputComponent();
        when(taskHelper.createOutput(eq(CODESYSTEM_FEASIBILITY), eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE),
                refCaptor.capture()))
                .thenReturn(orga1Output, orga2Output);

        service.execute(execution);

        assertEquals(2, task.getOutput().size());
        List<Reference> vals = refCaptor.getAllValues();
        assertEquals("MeasureReport/" + orga1ReportId, vals.get(0).getReference());
        assertEquals("MeasureReport/" + orga2ReportId, vals.get(1).getReference());
        assertEquals(EXTENSION_DIC_URI, task.getOutput().get(0).getExtensionFirstRep().getUrl());
        assertEquals(orga1Ref.getReference(), ((Reference) task.getOutput().get(0).getExtensionFirstRep().getValue()).getReference());
        assertEquals(EXTENSION_DIC_URI, task.getOutput().get(1).getExtensionFirstRep().getUrl());
        assertEquals(orga2Ref.getReference(), ((Reference) task.getOutput().get(1).getExtensionFirstRep().getValue()).getReference());
    }
}
