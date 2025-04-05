package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.ReadAccessHelper;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Ignore
public class StoreLiveResultTest {

    private static final String MEASURE_REPORT_ID = "4adfdef6-fc5b-4650-bdf5-80258a61e732";

    @Captor private ArgumentCaptor<Reference> refCaptor;
    @Captor private ArgumentCaptor<MeasureReport> measureReportCaptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DsfClientProvider clientProvider;

    @Mock private DsfClient client;
    @Mock private Variables variables;
    @Mock private TaskHelper taskHelper;
    @Mock private ProcessPluginApi api;
    @Mock private ReadAccessHelper readAccessHelper;

    @InjectMocks private StoreLiveResult service;

    private Task task;
    private MeasureReport measureReport;


    @BeforeEach
    public void setUp() {
        task = new Task();

        measureReport = new MeasureReport();
        measureReport.setIdElement(new IdType(MEASURE_REPORT_ID));

        when(variables.getLatestTask()).thenReturn(task);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(api.getDsfClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getLocalDsfClient()).thenReturn(client);
    }

    @Test
    public void testDoExecute_MeasureReportReferenceIsAddedToTask() throws Exception {
        var report = new MeasureReport();
        var measureReportId = new IdType("e26daf2d-2d55-4f23-a7c8-4b994e3a319e");
        report.setIdElement(measureReportId);
        var taskOutputComponent = new TaskOutputComponent();
        when(variables.getFhirResourceLocal(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);
        when(client.create(any(MeasureReport.class))).thenReturn(report);
        when(taskHelper.createOutput(refCaptor.capture(), eq(CODESYSTEM_FEASIBILITY), eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE)))
                .thenReturn(taskOutputComponent);

        service.execute(api, variables);

        assertEquals(taskOutputComponent, task.getOutputFirstRep());
        assertEquals("MeasureReport/" + measureReportId, refCaptor.getValue().getReference());
    }

    @Test
    public void testDoExecute_MeasureReportIsStored() throws Exception {
        when(variables.getFhirResourceLocal(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);
        when(taskHelper.createOutput(refCaptor.capture(), eq(CODESYSTEM_FEASIBILITY), eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE)))
                .thenReturn(new TaskOutputComponent());
        when(client.create(measureReportCaptor.capture())).thenReturn(measureReport);

        service.execute(api, variables);

        assertEquals(MEASURE_REPORT_ID, measureReportCaptor.getValue().getIdElement().getIdPart());
    }
}
