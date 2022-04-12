package de.medizininformatik_initiative.feasibility_dsf_process;

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
}
