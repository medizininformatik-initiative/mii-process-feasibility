package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.client.FhirWebserviceClient;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StoreLiveResultTest {

    private static final String MEASURE_REPORT_ID = "4adfdef6-fc5b-4650-bdf5-80258a61e732";

    @Captor private ArgumentCaptor<Reference> refCaptor;
    @Captor private ArgumentCaptor<MeasureReport> measureReportCaptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FhirWebserviceClientProvider clientProvider;

    @Mock private FhirWebserviceClient client;
    @Mock private DelegateExecution execution;
    @Mock private Variables variables;
    @Mock private TaskHelper taskHelper;
    @Mock private ProcessPluginApi api;

    @Spy private ReadAccessHelper readAccessHelper = new ReadAccessHelperImpl();

    @InjectMocks private StoreLiveResult service;

    private Task task;
    private MeasureReport measureReport;

    @BeforeEach
    public void setUp() {
        task = new Task();

        measureReport = new MeasureReport();
        measureReport.setIdElement(new IdType(MEASURE_REPORT_ID));

        when(api.getVariables(execution)).thenReturn(variables);
        when(variables.getLatestTask()).thenReturn(task);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(api.getReadAccessHelper()).thenReturn(readAccessHelper);
        when(api.getFhirWebserviceClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getLocalWebserviceClient()).thenReturn(client);
    }

    @Test
    public void testDoExecute_MeasureReportReferenceIsAddedToTask() throws Exception {
        var report = new MeasureReport();
        var measureReportId = new IdType("e26daf2d-2d55-4f23-a7c8-4b994e3a319e");
        report.setIdElement(measureReportId);
        var taskOutputComponent = new TaskOutputComponent();
        when(execution.getVariableLocal(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);
        when(client.create(any(MeasureReport.class))).thenReturn(report);
        when(taskHelper.createOutput(refCaptor.capture(), eq(CODESYSTEM_FEASIBILITY), eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE)))
                .thenReturn(taskOutputComponent);

        service.execute(execution);

        assertEquals(taskOutputComponent, task.getOutputFirstRep());
        assertEquals("MeasureReport/" + measureReportId, refCaptor.getValue().getReference());
    }

    @Test
    public void testDoExecute_MeasureReportIsStored() throws Exception {
        when(execution.getVariableLocal(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);
        when(taskHelper.createOutput(refCaptor.capture(), eq(CODESYSTEM_FEASIBILITY), eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE)))
                .thenReturn(new TaskOutputComponent());
        when(client.create(measureReportCaptor.capture())).thenReturn(measureReport);

        service.execute(execution);

        assertEquals(MEASURE_REPORT_ID, measureReportCaptor.getValue().getIdElement().getIdPart());
        assertEquals(1, measureReportCaptor.getValue().getMeta().getTag().size());
        assertEquals("http://dsf.dev/fhir/CodeSystem/read-access-tag", measureReportCaptor.getValue().getMeta().getTagFirstRep().getSystem());
        assertEquals("LOCAL", measureReportCaptor.getValue().getMeta().getTagFirstRep().getCode());
    }
}
