package de.medizininformatik_initiative.feasibility_dsf_process.service;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;
import static java.util.stream.Collectors.toList;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_LEADING_TASK;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.highmed.fhir.client.PreferReturnMinimalWithRetry;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StoreMeasureReportTest
{
	@Mock
	private FhirWebserviceClientProvider clientProvider;

	@Mock
	private FhirWebserviceClient localWebserviceClient;

	@Mock
	private PreferReturnMinimalWithRetry returnMinimal;

	@Mock
	private DelegateExecution execution;

	@Spy
	private ReadAccessHelper readAccessHelper = new ReadAccessHelperImpl();

	@Captor
	private ArgumentCaptor<MeasureReport> measureReportCaptor;

	@InjectMocks
	private StoreMeasureReport service;

	@Test
	public void testDoExecute() throws Exception
	{
		MeasureReport measureReport = new MeasureReport();

		Task task = new Task();
		Reference requesterReference = new Reference().setIdentifier(
				new Identifier().setSystem("http://localhost/systems/sample-system").setValue("requester-id"));
		task.setRequester(requesterReference);

		when(execution.getVariable(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);
		when(execution.getVariable(BPMN_EXECUTION_VARIABLE_LEADING_TASK)).thenReturn(task);
		when(clientProvider.getLocalWebserviceClient()).thenReturn(localWebserviceClient);
		when(localWebserviceClient.withMinimalReturn()).thenReturn(returnMinimal);
		when(returnMinimal.create(measureReportCaptor.capture())).thenReturn(new IdType("id-094601"));

		service.execute(execution);

		verify(execution).setVariable(VARIABLE_MEASURE_REPORT_ID, "id-094601");

		List<Coding> tags =  measureReportCaptor.getValue().getMeta().getTag().stream().filter(c -> "http://highmed.org/fhir/CodeSystem/read-access-tag".equals(c.getSystem())).collect(toList());
		assertEquals(2, tags.size());
		assertEquals(1 , tags.stream().filter(c -> "LOCAL".equals(c.getCode())).count());

		List<Coding> organizationTags = tags.stream().filter(c -> "ORGANIZATION".equals(c.getCode())).collect(toList());
		assertEquals(1 , organizationTags.size());

		List<Extension> organizationExtensions = organizationTags.stream().flatMap(c -> c.getExtension().stream())
				.filter(e -> "http://highmed.org/fhir/StructureDefinition/extension-read-access-organization".equals(
						e.getUrl())).collect(toList());
		assertEquals(1, organizationExtensions.size());
		assertEquals("requester-id", ((Identifier)organizationExtensions.get(0).getValue()).getValue());
	}
}
