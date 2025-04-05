package de.medizininformatik_initiative.process.feasibility;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IFetchConformanceTyped;
import ca.uhn.fhir.rest.gclient.IFetchConformanceUntyped;
import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClient;
import org.hl7.fhir.r4.model.CapabilityStatement;
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

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.FEASIBILITY_EXECUTE_PROCESS_ID;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.FEASIBILITY_REQUEST_PROCESS_ID;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
        var listener = new FeasibilityProcessPluginDeploymentStateListener(
                EvaluationStrategy.CQL, flareClient);

        listener.onProcessesDeployed(List.of());

        verify(flareClient, never()).requestFeasibility(any(byte[].class));
    }

    @Test
    @DisplayName("connection test is not executed when 'feasibilityExecute' process is not deployed.")
    void noConnectionTestIfExecuteProcessIsNotDeployed() throws Exception {
        var listener = new FeasibilityProcessPluginDeploymentStateListener(
                EvaluationStrategy.CQL, flareClient);

        listener.onProcessesDeployed(List.of("foo", FEASIBILITY_REQUEST_PROCESS_ID, "bar"));

        verify(flareClient, never()).requestFeasibility(any(byte[].class));
    }

    @Test
    @DisplayName("flare client connection test succeeds when evaluation strategy is 'structured-query' and no error occurs")
    void flareClientConnectionTestSucceeds() throws Exception {

            var listener = new FeasibilityProcessPluginDeploymentStateListener(
                    EvaluationStrategy.STRUCTURED_QUERY, flareClient);

            listener.onProcessesDeployed(List.of(FEASIBILITY_EXECUTE_PROCESS_ID));

            verify(flareClient).testConnection();
            assertThat(out.toString(), containsString("Feasibility plugin connection test to flare SUCCEEDED."));
    }

    @Test
    @DisplayName("flare client connection test fails when evaluation strategy is 'structured-query' and error occurs")
    void flareClientConnectionTestFails() throws Exception {
        var errorMessage = "error-223236";
        var exception = new IOException(errorMessage);
        doThrow(exception).when(flareClient).testConnection();
        var listener = new FeasibilityProcessPluginDeploymentStateListener(
                EvaluationStrategy.STRUCTURED_QUERY, flareClient);

        listener.onProcessesDeployed(List.of(FEASIBILITY_EXECUTE_PROCESS_ID));

        assertThat(out.toString(),
                containsString(format("Feasibility plugin connection test to flare FAILED. Error: %s - %s",
                exception.getClass().getName(), errorMessage)));
    }
}
