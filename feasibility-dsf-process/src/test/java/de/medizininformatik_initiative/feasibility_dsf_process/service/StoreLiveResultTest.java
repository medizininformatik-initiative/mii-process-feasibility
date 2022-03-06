package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.service.StoreLiveResult;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TASK;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class StoreLiveResultTest {

    private static final String MEASURE_REPORT_ID = "4adfdef6-fc5b-4650-bdf5-80258a61e732";

    @Captor
    private ArgumentCaptor<Reference> refCaptor;

    @Captor
    private ArgumentCaptor<MeasureReport> measureReportCaptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FhirWebserviceClientProvider clientProvider;

    @Mock
    private FhirWebserviceClient client;

    @Mock
    private DelegateExecution execution;

    @Mock
    private TaskHelper taskHelper;

    @InjectMocks
    private StoreLiveResult service;

    private Task task;
    private MeasureReport measureReport;

    @Before
    public void setUp() {
        task = new Task();
        measureReport = new MeasureReport();
        measureReport.setIdElement(new IdType(MEASURE_REPORT_ID));
    }

    @Test
    public void testDoExecute_MeasureReportReferenceIsAddedToTask() throws Exception {
        when(execution.getVariable(VARIABLE_MEASURE_REPORT))
                .thenReturn(measureReport);
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);

        TaskOutputComponent taskOutputComponent = new TaskOutputComponent();
        when(taskHelper.createOutput(eq(CODESYSTEM_FEASIBILITY), eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE),
                refCaptor.capture()))
                .thenReturn(taskOutputComponent);

        when(clientProvider.getLocalWebserviceClient()).thenReturn(client);

        MeasureReport report = new MeasureReport();
        IdType measureReportId = new IdType("e26daf2d-2d55-4f23-a7c8-4b994e3a319e");
        report.setIdElement(measureReportId);
        when(client.create(any(MeasureReport.class))).thenReturn(report);

        service.execute(execution);

        assertEquals(taskOutputComponent, task.getOutputFirstRep());
        assertEquals("MeasureReport/" + measureReportId, refCaptor.getValue().getReference());
    }

    @Test
    public void testDoExecute_MeasureReportIsStored() throws Exception {
        when(execution.getVariable(VARIABLE_MEASURE_REPORT))
                .thenReturn(measureReport);
        when(execution.getVariable(BPMN_EXECUTION_VARIABLE_TASK))
                .thenReturn(task);

        when(taskHelper.createOutput(eq(CODESYSTEM_FEASIBILITY), eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE),
                refCaptor.capture()))
                .thenReturn(new TaskOutputComponent());

        when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
        when(client.create(measureReportCaptor.capture())).thenReturn(measureReport);

        service.execute(execution);

        assertEquals(MEASURE_REPORT_ID, measureReportCaptor.getValue().getIdElement().getIdPart());
        assertEquals("http://highmed.org/fhir/CodeSystem/read-access-tag", measureReportCaptor.getValue().getMeta().getTagFirstRep().getSystem());
        assertEquals("ALL", measureReportCaptor.getValue().getMeta().getTagFirstRep().getCode());
    }
}
