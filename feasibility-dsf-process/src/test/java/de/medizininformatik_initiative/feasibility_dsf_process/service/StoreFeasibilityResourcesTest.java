package de.medizininformatik_initiative.feasibility_dsf_process.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.*;
import static org.assertj.core.api.Assertions.assertThat;
import static de.medizininformatik_initiative.feasibility_dsf_process.Assertions.assertThat;
import static org.hl7.fhir.r4.model.Bundle.HTTPVerb.POST;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StoreFeasibilityResourcesTest {

    public static final String LIBRARY_ID = "library-id-173603";
    public static final String MEASURE_ID = "measure-id-173603";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IGenericClient storeClient;

    @Mock
    private FeasibilityResourceCleaner cleaner;

    @Captor
    private ArgumentCaptor<Bundle> transactionBundleCaptor;

    @Mock
    private DelegateExecution execution;

    @Mock
    private Variables variables;

    @InjectMocks
    private StoreFeasibilityResources service;

    @Test
    public void testDoExecute() {
        var measure = new Measure();
        var library = new Library();
        var transactionResponse = new Bundle();
        transactionResponse.addEntry().getResponse().setLocation("Measure/" + MEASURE_ID);
        library.getContentFirstRep().setContentType("text/cql");
        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);
        when(storeClient.transaction().withBundle(transactionBundleCaptor.capture()).execute()).thenReturn(transactionResponse);

        service.doExecute(execution, variables);

        verify(cleaner).cleanLibrary(library);
        verify(cleaner).cleanMeasure(measure);
        assertThat(transactionBundleCaptor.getValue()).hasType("transaction");
        assertThat(transactionBundleCaptor.getValue().getEntry()).hasSize(2);
        assertThat(transactionBundleCaptor.getValue().getEntry().get(0).getResource())
                .isDeepEqualTo(measure);
        assertThat(transactionBundleCaptor.getValue().getEntry().get(0).getRequest())
                .hasMethod(POST)
                .hasUrl("Measure");
        assertThat(transactionBundleCaptor.getValue().getEntry().get(1).getResource())
                .isDeepEqualTo(library);
        assertThat(transactionBundleCaptor.getValue().getEntry().get(1).getRequest())
                .hasMethod(POST)
                .hasUrl("Library");
        verify(variables).setString(VARIABLE_MEASURE_ID, MEASURE_ID);
    }
}
