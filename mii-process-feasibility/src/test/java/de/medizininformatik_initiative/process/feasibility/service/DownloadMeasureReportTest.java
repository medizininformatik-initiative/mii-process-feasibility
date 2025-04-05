package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.CodeableConcept;
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

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DownloadMeasureReportTest {

    private static final String MEASURE_REPORT_ID = "id-144911";

    @Mock private DsfClientProvider clientProvider;
    @Mock private DsfClient client;
    @Mock private TaskHelper taskHelper;
    @Mock private Variables variables;
    @Mock private ProcessPluginApi api;

    @InjectMocks private DownloadMeasureReport service;

    private Task task;


    @BeforeEach
    public void setUp() {
        task = new Task();
        task.setStatus(TaskStatus.INPROGRESS);
        when(api.getTaskHelper()).thenReturn(taskHelper);
    }

    @Test
    public void testDoExecute_MissingMeasureReportReference() throws Exception {
        var errorMessage = "Missing measure report reference.";

        when(variables.getLatestTask()).thenReturn(task);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE, Reference.class))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(api, variables))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(errorMessage);
    }

    @Test
    public void testDoExecute() throws Exception {
        var requesterRef = new Reference().setReference("http://localhost");
        task.setRequester(requesterRef);
        var baseUrl = "https://foo.bar/fhir";
        var measureReportRef = new Reference().setReference(baseUrl + "/MeasureReport/" + MEASURE_REPORT_ID);
        var coding = new CodeableConcept();
        var measureReportGroup = new MeasureReport.MeasureReportGroupComponent()
                .setCode(coding);
        var measureReport = new MeasureReport()
                .setGroup(List.of(measureReportGroup));
        measureReport.setId(MEASURE_REPORT_ID);

        when(variables.getLatestTask()).thenReturn(task);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE, Reference.class))
                .thenReturn(Optional.of(measureReportRef));
        when(api.getDsfClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getDsfClient(baseUrl))
                .thenReturn(client);
        when(client.read(MeasureReport.class, MEASURE_REPORT_ID))
                .thenReturn(measureReport);

        service.execute(api, variables);

        verify(variables).setFhirResourceLocal(VARIABLE_MEASURE_REPORT, measureReport);
    }
}
