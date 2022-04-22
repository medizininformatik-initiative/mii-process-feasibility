package de.medizininformatik_initiative.feasibility_dsf_process.service;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_LEADING_TASK;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.highmed.fhir.client.PreferReturnMinimalWithRetry;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
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

	@Mock
	private ReadAccessHelper readAccessHelper;

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

		when(clientProvider.getLocalWebserviceClient()).thenReturn(localWebserviceClient);
		when(localWebserviceClient.withMinimalReturn()).thenReturn(returnMinimal);
		when(returnMinimal.create(measureReport)).thenReturn(new IdType("id-094601"));
		when(execution.getVariable(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);
		when(execution.getVariable(BPMN_EXECUTION_VARIABLE_LEADING_TASK)).thenReturn(task);
		when(readAccessHelper.addOrganization(measureReport, "requester-id")).thenReturn(
				new ReadAccessHelperImpl().addOrganization(measureReport, "requester-id"));

		service.execute(execution);

		verify(execution).setVariable(VARIABLE_MEASURE_REPORT_ID, "id-094601");
	}
}
