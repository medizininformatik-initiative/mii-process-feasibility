package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import org.apache.http.client.ClientProtocolException;

import java.io.IOException;

/**
 * Describes a client for communicating with a Flare instance.
 */
public interface FlareWebserviceClient {

    /**
     * Given a structured query sends this query to a Flare instance in order to request the corresponding
     * feasibility (population count).
     *
     * @param structuredQuery The query that shall be evaluated.
     * @return Feasibility (population count) corresponding to the evaluated query.
     * @throws IOException If an I/O error occurs when sending or receiving.
     * @throws InterruptedException If the operation is interrupted.
     */
    int requestFeasibility(byte[] structuredQuery) throws IOException, InterruptedException;

    /**
     * Tests the connection to the flare server, and flare server only, using the connection settings defined by spring
     * configuration.
     * <p>
     * Throws an exception if the connection test fails, otherwise the connection test was successful.
     * </p>
     *
     * @throws IOException in case of a problem or the connection was aborted
     * @throws ClientProtocolException in case of an http protocol error
     */
    void testConnection() throws ClientProtocolException, IOException;
}
