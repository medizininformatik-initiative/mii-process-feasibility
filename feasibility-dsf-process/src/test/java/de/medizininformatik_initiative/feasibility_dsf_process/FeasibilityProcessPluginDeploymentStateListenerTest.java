package de.medizininformatik_initiative.feasibility_dsf_process;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IFetchConformanceTyped;
import ca.uhn.fhir.rest.gclient.IFetchConformanceUntyped;
import de.medizininformatik_initiative.feasibility_dsf_process.client.flare.FlareWebserviceClient;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.FEASIBILITY_EXECUTE_PROCESS_ID;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.FEASIBILITY_REQUEST_PROCESS_ID;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FeasibilityProcessPluginDeploymentStateListenerTest {

    @Mock FlareWebserviceClient flareClient;
    @Mock IGenericClient storeClient;
    @Mock IFetchConformanceUntyped untypedFetch;
    @Mock IFetchConformanceTyped<CapabilityStatement> typedFetch;
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setup() {
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("connection test is not executed when no process is deployed.")
    void noConnectionTestIfNoProcessDeployed() throws Exception {
        FeasibilityProcessPluginDeploymentStateListener listener = new FeasibilityProcessPluginDeploymentStateListener(
                EvaluationStrategy.CQL, storeClient, flareClient);

        listener.onProcessesDeployed(List.of());

        verify(flareClient, never()).requestFeasibility(any(byte[].class));
    }

    @Test
    @DisplayName("connection test is not executed when 'feasibilityExecute' process is not deployed.")
    void noConnectionTestIfExecuteProcessIsNotDeployed() throws Exception {
        FeasibilityProcessPluginDeploymentStateListener listener = new FeasibilityProcessPluginDeploymentStateListener(
                EvaluationStrategy.CQL, storeClient, flareClient);

        listener.onProcessesDeployed(List.of("foo", FEASIBILITY_REQUEST_PROCESS_ID, "bar"));

        verify(flareClient, never()).requestFeasibility(any(byte[].class));
    }

    @Test
    @DisplayName("flare client connection test succeeds when evaluation strategy is 'structured-query' and no error occurs")
    void flareClientConnectionTestSucceeds() throws Exception {

            FeasibilityProcessPluginDeploymentStateListener listener = new FeasibilityProcessPluginDeploymentStateListener(
                    EvaluationStrategy.STRUCTURED_QUERY, storeClient, flareClient);

            listener.onProcessesDeployed(List.of(FEASIBILITY_EXECUTE_PROCESS_ID));

            verify(flareClient).testConnection();
            assertThat(out.toString(), containsString("Connection test OK (flare)"));
    }

    @Test
    @DisplayName("store client connection test succeeds when evaluation strategy is 'cql' and no error occurs")
    void storeClientConnectionTestSucceeds() throws Exception {
            String softwareName = "software-213030";
            String softwareVersion = "version-213136";
            CapabilityStatementSoftwareComponent software = new CapabilityStatementSoftwareComponent()
                    .setName(softwareName)
                    .setVersion(softwareVersion);
            CapabilityStatement statement = new CapabilityStatement().setSoftware(software);
            when(storeClient.capabilities()).thenReturn(untypedFetch);
            when(untypedFetch.ofType(CapabilityStatement.class)).thenReturn(typedFetch);
            when(typedFetch.execute()).thenReturn(statement);

            FeasibilityProcessPluginDeploymentStateListener listener = new FeasibilityProcessPluginDeploymentStateListener(
                    EvaluationStrategy.CQL, storeClient, flareClient);

            listener.onProcessesDeployed(List.of(FEASIBILITY_EXECUTE_PROCESS_ID));
            assertThat(out.toString(),
                    containsString(format("Connection test OK (%s - %s)", softwareName, softwareVersion)));
    }

    @Test
    @DisplayName("flare client connection test fails when evaluation strategy is 'structured-query' and error occurs")
    void flareClientConnectionTestFails() throws Exception {
        String errorMessage = "error-223236";
        IOException exception = new IOException(errorMessage);
        doThrow(exception).when(flareClient).testConnection();
        FeasibilityProcessPluginDeploymentStateListener listener = new FeasibilityProcessPluginDeploymentStateListener(
                EvaluationStrategy.STRUCTURED_QUERY, storeClient, flareClient);

        listener.onProcessesDeployed(List.of(FEASIBILITY_EXECUTE_PROCESS_ID));

        assertThat(out.toString(), containsString(format("Connection test FAILED (flare) - error: %s - %s",
                exception.getClass().getName(), errorMessage)));
    }

    @Test
    @DisplayName("store client connection test fails when evaluation strategy is 'cql' and error occurs")
    void storeClientConnectionTestFails() throws Exception {
        String errorMessage = "error-223622";
        RuntimeException exception = new RuntimeException(errorMessage);
        when(storeClient.capabilities()).thenReturn(untypedFetch);
        when(untypedFetch.ofType(CapabilityStatement.class)).thenReturn(typedFetch);
        doThrow(exception).when(typedFetch).execute();

        FeasibilityProcessPluginDeploymentStateListener listener = new FeasibilityProcessPluginDeploymentStateListener(
                EvaluationStrategy.CQL, storeClient, flareClient);

        listener.onProcessesDeployed(List.of(FEASIBILITY_EXECUTE_PROCESS_ID));
        assertThat(out.toString(),
                containsString(format("Connection test FAILED - error: %s - %s", exception.getClass().getName(),
                        errorMessage)));

    }
}
