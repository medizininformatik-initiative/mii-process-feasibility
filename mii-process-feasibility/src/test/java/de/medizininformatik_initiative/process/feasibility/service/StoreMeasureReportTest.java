package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.PreferReturnMinimalWithRetry;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StoreMeasureReportTest
{
    @Mock private FhirWebserviceClientProvider clientProvider;
    @Mock private FhirWebserviceClient localWebserviceClient;
    @Mock private PreferReturnMinimalWithRetry returnMinimal;
    @Mock private TaskHelper taskHelper;
    @Mock private DelegateExecution execution;
    @Mock private ProcessPluginApi api;
    @Mock private Variables variables;

    @Spy private ReadAccessHelper readAccessHelper = new ReadAccessHelperImpl();

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

        when(api.getVariables(execution)).thenReturn(variables);
        when(variables.getResource(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);
        when(variables.getStartTask()).thenReturn(task);
        when(api.getReadAccessHelper()).thenReturn(readAccessHelper);
        when(api.getFhirWebserviceClientProvider()).thenReturn(clientProvider);
        when(api.getFhirContext()).thenReturn(FhirContext.forR4());
        when(clientProvider.getLocalWebserviceClient()).thenReturn(localWebserviceClient);
        when(localWebserviceClient.withMinimalReturn()).thenReturn(returnMinimal);
        when(returnMinimal.create(measureReportCaptor.capture())).thenReturn(measureReportId);
        when(api.getTaskHelper()).thenReturn(taskHelper);
        when(taskHelper.createOutput(referenceCaptor.capture(), eq(CODESYSTEM_FEASIBILITY),
                eq(CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE))).thenReturn(taskOutput);
        when(taskHelper.getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class))
                        .thenReturn(Optional.of(new Reference().setReference(measureId.toString())));

        service.execute(execution);

        verify(variables).setString(VARIABLE_MEASURE_REPORT_ID, "id-094601");
        verify(readAccessHelper).addOrganization(measureReport, requesterId.getValue());
        verify(variables).updateTask(task);

        var capturedMeasureReport = measureReportCaptor.getValue();
        assertThat(capturedMeasureReport.getMeasure()).isEqualTo(measureId.toString());
        assertThat(capturedMeasureReport.getEvaluatedResource()).isEmpty();

        var tags = capturedMeasureReport.getMeta().getTag().stream()
                .filter(c -> "http://dsf.dev/fhir/CodeSystem/read-access-tag".equals(c.getSystem())).collect(toList());
        assertThat(tags).hasSize(2);
        assertThat(tags.stream().filter(c -> "LOCAL".equals(c.getCode()))).hasSize(1);

        var organizationTags = tags.stream().filter(c -> "ORGANIZATION".equals(c.getCode())).collect(toList());
        assertThat(organizationTags).hasSize(1);

        var organizationExtensions = organizationTags.stream().flatMap(c -> c.getExtension().stream())
                .filter(e -> "http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization".equals(
                        e.getUrl())).collect(toList());
        assertThat(organizationExtensions).hasSize(1);
        assertThat(((Identifier) organizationExtensions.get(0).getValue()).getValue()).isEqualTo("requester-id");

        assertThat(referenceCaptor.getValue()).isNotNull();
        assertThat(referenceCaptor.getValue().getReference()).isEqualTo(measureReportId.getValue());
        assertThat(task.getOutput()).hasSize(1);
        assertThat(task.getOutputFirstRep()).isEqualTo(taskOutput);
    }
}
