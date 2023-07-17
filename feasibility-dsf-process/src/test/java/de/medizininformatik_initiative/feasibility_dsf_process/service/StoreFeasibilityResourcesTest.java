package de.medizininformatik_initiative.feasibility_dsf_process.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.variables.Variables;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StoreFeasibilityResourcesTest {

    public static final String ID = "foo";

    @Spy private ReadAccessHelperImpl readAccessHelper;

    @Captor ArgumentCaptor<Resource> resourceCaptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IGenericClient storeClient;

    @Mock private ProcessPluginApi api;
    @Mock private DelegateExecution execution;
    @Mock private Variables variables;

    @InjectMocks private StoreFeasibilityResources service;

    @Test
    public void testDoExecute() {
        Measure measure = new Measure();
        Library library = new Library();
        library.setContent(List.of(new Attachment()
                .setContentType("text/cql")
                .setData("foo".getBytes())));
        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);

        var libraryServerId = UUID.randomUUID();
        var libraryMethodOutcome = new MethodOutcome(new IdType(libraryServerId.toString()));
        var measureMethodOutcome = new MethodOutcome(new IdType(ID));

        when(storeClient.create().resource(any(Resource.class)).execute())
                .thenReturn(libraryMethodOutcome, measureMethodOutcome);

        service.doExecute(execution, variables);

        verify(variables).setString(VARIABLE_MEASURE_ID, ID);
    }

    @Test
    public void testDoExecute_ReadAccessTagsGetStripped() {
        var measure = new Measure();
        measure.getMeta().addTag("foo", "bar", "1234");
        measure = readAccessHelper.addLocal(measure);

        var library = new Library();
        library = readAccessHelper.addAll(library);
        library.setContent(List.of(new Attachment()
                .setContentType("text/cql")
                .setData("foo".getBytes())));
        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);

        var libraryServerId = UUID.randomUUID();
        var libraryMethodOutcome = new MethodOutcome(new IdType(libraryServerId.toString()));
        var measureMethodOutcome = new MethodOutcome(new IdType(ID));

        when(storeClient.create().resource(resourceCaptor.capture()).execute())
                .thenReturn(libraryMethodOutcome, measureMethodOutcome);

        service.doExecute(execution, variables);

        var capturedResources = resourceCaptor.getAllValues();
        var capturedLibrary = (Library) capturedResources.get(0);
        var capturedMeasure = (Measure) capturedResources.get(1);
        assertFalse(readAccessHelper.hasLocal(capturedLibrary));
        assertFalse(readAccessHelper.hasAll(capturedLibrary));
        assertFalse(readAccessHelper.hasLocal(capturedMeasure));
        assertFalse(readAccessHelper.hasAll(capturedMeasure));
        assertNotNull(capturedMeasure.getMeta().getTag("foo", "bar"));
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
        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);

        var libraryServerId = UUID.randomUUID();
        var libraryMethodOutcome = new MethodOutcome(new IdType(libraryServerId.toString()));

        var measureServerId = UUID.randomUUID();
        var measureMethodOutcome = new MethodOutcome(new IdType(measureServerId.toString()));

        when(storeClient.create().resource(resourceCaptor.capture()).execute())
                .thenReturn(libraryMethodOutcome, measureMethodOutcome);

        service.doExecute(execution, variables);

        var capturedLibrary = (Library) resourceCaptor.getAllValues().get(0);
        assertEquals("text/cql", capturedLibrary.getContent().get(0).getContentType());
    }

    @Test
    public void testDoExecute_FailsIfCqlContentIsMissing() {
        Measure measure = new Measure();
        Library library = new Library();
        when(variables.getResource(VARIABLE_MEASURE)).thenReturn(measure);
        when(variables.getResource(VARIABLE_LIBRARY)).thenReturn(library);

        assertThrows(IllegalStateException.class, () -> service.doExecute(execution, variables));
    }
}
