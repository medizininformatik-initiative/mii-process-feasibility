package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.highmed.fhir.client.PreferReturnMinimalWithRetry;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class StoreMeasureReportTest {

    @Mock
    private FhirWebserviceClientProvider clientProvider;

    @Mock
    private FhirWebserviceClient localWebserviceClient;

    @Mock
    private PreferReturnMinimalWithRetry returnMinimal;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private StoreMeasureReport service;

    @Test
    public void testDoExecute() {
        when(clientProvider.getLocalWebserviceClient()).thenReturn(localWebserviceClient);
        when(localWebserviceClient.withMinimalReturn()).thenReturn(returnMinimal);
        MeasureReport measureReport = new MeasureReport();
        when(returnMinimal.create(measureReport)).thenReturn(new IdType("foo"));
        when(execution.getVariable(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);

        service.doExecute(execution);

        verify(execution).setVariable(VARIABLE_MEASURE_REPORT_ID, "foo");
    }
}
