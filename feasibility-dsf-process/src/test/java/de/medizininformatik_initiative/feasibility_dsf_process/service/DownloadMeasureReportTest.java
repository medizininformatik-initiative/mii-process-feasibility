package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.EnhancedFhirWebserviceClientProvider;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.PreferReturnMinimalWithRetry;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hl7.fhir.r4.model.Task.TaskStatus.FAILED;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DownloadMeasureReportTest {

    private static final String MEASURE_REPORT_ID = "id-144911";

    @Mock private EnhancedFhirWebserviceClientProvider clientProvider;
    @Mock private FhirWebserviceClient webserviceClient;
    @Mock private TaskHelper taskHelper;
    @Mock private DelegateExecution execution;
    @Mock private PreferReturnMinimalWithRetry retry;
    @Mock private Variables variables;
    @Mock private ProcessPluginApi api;
    @Mock private ProcessEngine processEngine;
    @Mock private RuntimeService runtimeService;

    @InjectMocks private DownloadMeasureReport service;

    private Task task;


    @BeforeEach
    public void setUp() {
        task = new Task();
        task.setStatus(TaskStatus.INPROGRESS);
        when(api.getVariables(execution)).thenReturn(variables);
        when(api.getTaskHelper()).thenReturn(taskHelper);
    }

    @Test
    public void testDoExecute_MissingMeasureReportReference() throws Exception {
        Task.TaskOutputComponent taskOutputComponent = new Task.TaskOutputComponent();
        String instanceId = "instanceId-241153";
        String errorMessage = "Missing measure report reference.";

        when(variables.getLatestTask()).thenReturn(task);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE, Reference.class))
                .thenReturn(Optional.empty());
        when(variables.getTasks()).thenReturn(List.of(task));
        when(api.getFhirWebserviceClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getLocalWebserviceClient()).thenReturn(webserviceClient);
        when(webserviceClient.withMinimalReturn()).thenReturn(retry);
        when(execution.getProcessInstanceId()).thenReturn(instanceId);
        when(execution.getProcessEngine()).thenReturn(processEngine);
        when(processEngine.getRuntimeService()).thenReturn(runtimeService);

        service.execute(execution);

        verify(retry).update(task);
        verify(runtimeService).deleteProcessInstance(instanceId, errorMessage);
        assertSame(FAILED, task.getStatus());
        assertThat(task.getOutputFirstRep().getValue().toString(), containsString(errorMessage));
    }

    @Test
    public void testDoExecute() throws Exception {
        Reference requesterRef = new Reference().setReference("http://localhost");
        task.setRequester(requesterRef);
        Reference measureReportRef = new Reference().setReference("MeasureReport/" + MEASURE_REPORT_ID);
        CodeableConcept coding = new CodeableConcept();
        MeasureReport.MeasureReportGroupComponent measureReportGroup = new MeasureReport.MeasureReportGroupComponent()
                .setCode(coding);
        MeasureReport measureReport = new MeasureReport()
                .setGroup(List.of(measureReportGroup));

        when(variables.getLatestTask()).thenReturn(task);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE, Reference.class))
                .thenReturn(Optional.of(measureReportRef));
        when(clientProvider.getWebserviceClientByReference(new IdType(measureReportRef.getReference())))
                .thenReturn(webserviceClient);
        when(webserviceClient.read(MeasureReport.class, MEASURE_REPORT_ID))
                .thenReturn(measureReport);

        service.execute(execution);

        verify(execution).setVariableLocal(VARIABLE_MEASURE_REPORT, measureReport);
    }
}
