package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.*;
import static org.mockito.ArgumentMatchers.any;
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
        library.getContentFirstRep().setContentType("text/cql");
        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);

        var libraryMethodOutcome = new MethodOutcome(new IdType(LIBRARY_ID));
        var measureMethodOutcome = new MethodOutcome(new IdType(MEASURE_ID));

        when(storeClient.create().resource(any(Resource.class)).execute())
                .thenReturn(libraryMethodOutcome, measureMethodOutcome);

        service.doExecute(execution, variables);

        verify(cleaner).cleanLibrary(library);
        verify(cleaner).cleanMeasure(measure);
        verify(variables).setString(VARIABLE_MEASURE_ID, MEASURE_ID);
    }
}
