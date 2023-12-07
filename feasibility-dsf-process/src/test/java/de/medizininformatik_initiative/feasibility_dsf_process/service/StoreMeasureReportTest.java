package de.medizininformatik_initiative.feasibility_dsf_process.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @InjectMocks private StoreMeasureReport service;

    @Test
    public void testDoExecute() throws Exception
    {
        var initialMeasureFromZars = new Measure();
        var measureId = UUID.randomUUID();
        initialMeasureFromZars.setId(new IdType(measureId.toString()));
        initialMeasureFromZars.setUrl("http://some.domain/fhir/Measure/" + measureId);

        var measureReport = new MeasureReport();
        var patient = new Patient();
        patient.setId("foo");
        var patientRef = new Reference(patient);
        measureReport = measureReport.setEvaluatedResource(List.of(patientRef));

        var task = new Task();
        Identifier requesterId = new Identifier().setSystem("http://localhost/systems/sample-system").setValue("requester-id");
        var requesterReference = new Reference().setIdentifier(                requesterId);
        task.setRequester(requesterReference);

        when(api.getVariables(execution)).thenReturn(variables);
        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(initialMeasureFromZars);
        when(variables.getResource(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);
        when(variables.getStartTask()).thenReturn(task);
        when(api.getReadAccessHelper()).thenReturn(readAccessHelper);
        when(api.getFhirWebserviceClientProvider()).thenReturn(clientProvider);
        when(clientProvider.getLocalWebserviceClient()).thenReturn(localWebserviceClient);
        when(localWebserviceClient.withMinimalReturn()).thenReturn(returnMinimal);
        when(returnMinimal.create(measureReportCaptor.capture())).thenReturn(new IdType("id-094601"));

        service.execute(execution);

        verify(variables).setString(VARIABLE_MEASURE_REPORT_ID, "id-094601");
        verify(readAccessHelper).addOrganization(measureReport, requesterId.getValue());


        var capturedMeasureReport = measureReportCaptor.getValue();
        assertEquals("http://some.domain/fhir/Measure/" + measureId, capturedMeasureReport.getMeasure());
        assertTrue(capturedMeasureReport.getEvaluatedResource().isEmpty());

        var tags = capturedMeasureReport.getMeta().getTag().stream()
                .filter(c -> "http://dsf.dev/fhir/CodeSystem/read-access-tag".equals(c.getSystem())).collect(toList());
        assertEquals(2, tags.size());
        assertEquals(1 , tags.stream().filter(c -> "LOCAL".equals(c.getCode())).count());

        var organizationTags = tags.stream().filter(c -> "ORGANIZATION".equals(c.getCode())).collect(toList());
        assertEquals(1 , organizationTags.size());

        var organizationExtensions = organizationTags.stream().flatMap(c -> c.getExtension().stream())
                .filter(e -> "http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization".equals(
                        e.getUrl())).collect(toList());
        assertEquals(1, organizationExtensions.size());
        assertEquals("requester-id", ((Identifier)organizationExtensions.get(0).getValue()).getValue());
    }
}
