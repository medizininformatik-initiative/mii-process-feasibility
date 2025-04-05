package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.client.dsf.PreferReturnMinimalWithRetry;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.ReadAccessHelper;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Ignore
public class StoreMeasureReportTest
{
    @Mock private DsfClientProvider clientProvider;
    @Mock private DsfClient localWebserviceClient;
    @Mock private PreferReturnMinimalWithRetry returnMinimal;
    @Mock private TaskHelper taskHelper;
    @Mock private ProcessPluginApi api;
    @Mock private Variables variables;

    @Mock private ReadAccessHelper readAccessHelper;

    @Captor private ArgumentCaptor<MeasureReport> measureReportCaptor;
    @Captor private ArgumentCaptor<Reference> referenceCaptor;

    @InjectMocks private StoreMeasureReport service;

    @Test
    public void testDoExecute() throws Exception
    {
        var measureId = UUID.randomUUID();
        var initialMeasureFromZars = new Measure();
        initialMeasureFromZars.setId(new IdType(measureId.toString()));
        initialMeasureFromZars.setUrl("http://some.domain/fhir/Measure/" + measureId);

        var measureReport = new MeasureReport()
                .setEvaluatedResource(List.of(new Reference(new Patient().setId("foo"))));
        var taskOutput = new TaskOutputComponent();
        var measureReportId = new IdType("id-094601");
        var task = new Task();
        var requesterId = new Identifier().setSystem("http://localhost/systems/sample-system").setValue("requester-id");
        task.setRequester(new Reference().setIdentifier(requesterId));

        when(variables.getFhirResource(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);
        when(variables.getStartTask()).thenReturn(task);
        when(api.getReadAccessHelper()).thenReturn(readAccessHelper);
        when(api.getDsfClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getLocalDsfClient()).thenReturn(localWebserviceClient);
        when(localWebserviceClient.withMinimalReturn()).thenReturn(returnMinimal);
        when(returnMinimal.create(measureReportCaptor.capture())).thenReturn(measureReportId);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(taskHelper.createOutput(referenceCaptor.capture(), eq(CODESYSTEM_FEASIBILITY),
                eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE))).thenReturn(taskOutput);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class))
                        .thenReturn(Optional.of(new Reference().setReference(measureId.toString())));

        service.execute(api, variables);

        verify(variables).setString(VARIABLE_MEASURE_REPORT_ID, "id-094601");
        verify(readAccessHelper).addOrganization(measureReport, requesterId.getValue());
        verify(variables).updateTask(task);

        var capturedMeasureReport = measureReportCaptor.getValue();
        assertThat(capturedMeasureReport.getMeasure()).isEqualTo(measureId.toString());
        assertThat(capturedMeasureReport.getEvaluatedResource()).isEmpty();
        assertThat(referenceCaptor.getValue()).isNotNull();
        assertThat(referenceCaptor.getValue().getReference()).isEqualTo(measureReportId.getValue());
        assertThat(task.getOutput()).hasSize(1);
        assertThat(task.getOutputFirstRep()).isEqualTo(taskOutput);
    }
}
