package de.medizininformatik_initiative.feasibility_dsf_process.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.UUID;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class StoreFeasibilityResourcesTest {

    public static final String ID = "foo";

    @Captor
    ArgumentCaptor<Resource> resourceCaptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IGenericClient storeClient;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private StoreFeasibilityResources service;

    @Test
    public void testDoExecute() {
        Measure measure = new Measure();
        Library library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("text/cql")
                .setData("foo".getBytes())));
        when(execution.getVariable(VARIABLE_MEASURE)).thenReturn(measure);
        when(execution.getVariable(VARIABLE_LIBRARY)).thenReturn(library);

        var libraryServerId = UUID.randomUUID();
        var libraryMethodOutcome = new MethodOutcome(new IdType(libraryServerId.toString()));
        var measureMethodOutcome = new MethodOutcome(new IdType(ID));

        when(storeClient.create().resource(any(Resource.class)).execute())
                .thenReturn(libraryMethodOutcome, measureMethodOutcome);

        service.doExecute(execution);

        verify(execution).setVariable(VARIABLE_MEASURE_ID, ID);
    }

    @Test
    public void testDoExecute_OnlyCqlContentGetsStored() {
        Measure measure = new Measure();
        Library library = new Library();

        var libraryAttachments = List.of(new Attachment()
                        .setContentType("application/json")
                        .setData("foo".getBytes()),
                new Attachment()
                        .setContentType("text/cql")
                        .setData("bar".getBytes()));
        library.setContent(libraryAttachments);
        when(execution.getVariable(VARIABLE_MEASURE)).thenReturn(measure);
        when(execution.getVariable(VARIABLE_LIBRARY)).thenReturn(library);

        var libraryServerId = UUID.randomUUID();
        var libraryMethodOutcome = new MethodOutcome(new IdType(libraryServerId.toString()));

        var measureServerId = UUID.randomUUID();
        var measureMethodOutcome = new MethodOutcome(new IdType(measureServerId.toString()));

        when(storeClient.create().resource(resourceCaptor.capture()).execute())
                .thenReturn(libraryMethodOutcome, measureMethodOutcome);

        service.doExecute(execution);

        var capturedLibrary = (Library) resourceCaptor.getAllValues().get(0);
        assertEquals("text/cql", capturedLibrary.getContent().get(0).getContentType());
    }

    @Test
    public void testDoExecute_FailsIfCqlContentIsMissing() {
        Measure measure = new Measure();
        Library library = new Library();
        when(execution.getVariable(VARIABLE_MEASURE)).thenReturn(measure);
        when(execution.getVariable(VARIABLE_LIBRARY)).thenReturn(library);

        assertThrows(IllegalStateException.class, () -> service.doExecute(execution));
    }
}
