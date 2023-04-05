# Feasibility DSF Process

A distributed feasibility query runs based on the feasibility process. A feasibility query contains multiple criteria and returns one population count per organization. An organization (usually the Zentrale Antrags- und Registerstelle (ZARS)) requests a feasibility query to start off the process. Once started multiple organizations (usually Datenintegrationszentren (DIZ)) execute this query and report results back.

## High-Level Overview

![fig-1](./docs/feasibility-process-01.png)

As you can see in the figure above, the feasibility query process starts with a `request` message (1) that the ZARS or any other initiating organization sends to itself. By sending this message to the ZARS itself, the Business Process Engine (BPE) of the ZARS can be used to distribute the `execute` messages (2) to all DIZ'es in question. After receiving the `execute` messages, each DIZ will calculate the result and answer with a `result` message (3). The ZARS will accumulate the results and provide the requester with live updates.

## Request and Execute Process in Detail

### Distribute Query

Messages, queries and results are represented by FHIR resources. The following three figures show the entire FHIR resource flow. In contrast to the figure above, only one DIZ is represented, but both, the ZARS and the DIZ are divided into its individual components. On the ZARS side the components are the FHIR communication server and the BPE, were the DIZ contains an additional Blaze FHIR server.

![fig-1](./docs/feasibility-process-02.png)

1. Like in the high-level overview, the process starts with the `request` message. Beside the FHIR [Task][1] resource for the message, two other FHIR resources will be sent to the ZARS FHIR server. The first one is the [Measure][2] resource specifying the population criteria that are defined in the second resource, the [Library][3]. All three resources are put into a transaction [Bundle][4] in order to create all together in one transaction.

2. After the ZARS FHIR server receives the resources, it notifies the ZARS BPE via websocket subscription by transmitting the Task resource. The incoming Task resource starts the `request` process that implements the query distribution to all appropriate DIZ'es.

3. To each DIZ, an `execute` message is sent via a Task resource.

4. After arriving at the DIZ FHIR communication server, the `execute` Task resource is transferred to the DIZ BPE via websocket subscription, starting the `execute` process.

### Execute Query (CQL)

This describes the execute process in case `CQL` is specified as an evaluation strategy.

![fig-1](./docs/feasibility-process-03.png)

5. In each DIZ, the `execute` process starts by fetching the Measure and Library resource created at the ZARS FHIR communication server. The resources have to be fetched by the BPE because only Task resources are sent actively between organizations and message payload is only fetched in case a process really needs it. FHIR search is used in order to fetch both resources in one HTTP request by searching for the Measure resource and including the referenced Library resource.

6. In the next step, the `execute` process stores the Measure and Library resources to the Blaze FHIR server in order to be able to execute the [$evaluate-measure][5] operation.

7. The resulting [MeasureReport][6] resource is transferred back to the DIZ BPE.

8. After receiving the MeasureReport, the DIZ BPE obfuscates the population count within it unless disabled. Subsequently, it stores the MeasureReport on the DIZ FHIR communication server in order to make it available for the ZARS.

9. In its last step, the `execute` process sends a `result` message to the ZARS. The `result` message references the MeasureReport, so that it can be retrieved by the ZARS.

### Execute Query (Structured Query)

This describes the execute process in case `Structured Query` is specified as an evaluation strategy.

![fig-1](./docs/feasibility-process-04.png)

5. In each DIZ, the `execute` process starts by checking the current request rate against the configured rate limit and rejects the current and all future requests when the rate limit is exceeded until the BPE is restarted. If the rate limit has not been exceeded the process continues by fetching the Measure and Library resource created at the ZARS FHIR communication server. The resources have to be fetched by the BPE because only Task resources are sent actively between organizations and message payload is only fetched in case a process really needs it. FHIR search is used in order to fetch both resources in one HTTP request by searching for the Measure resource and including the referenced Library resource.

6. The DIZ BPE extracts the structured query from the Library resource and sends it to the Flare server.

7. The DIZ Flare server runs the evaluation by transforming the structured query into one or more requests compatible with the FHIR standard.

8. The DIZ Flare server collects all results from the FHIR server.

9. The DIZ Flare server sends back a single population count to the BPE.

10. Since the DIZ Flare server does not respond with a FHIR resource the BPE creates a MeasureReport resource based on the population count and the resources it downloaded in `5.`. Additionally, it obfuscates the population count unless disabled. The DIZ BPE stores this MeasureReport on the DIZ FHIR communication server in order to make it available for the ZARS.

11. In its last step, the `execute` process sends a `result` message to the ZARS. The `result` message references the MeasureReport, so that it can be retrieved by the ZARS.

### Retrieve Results

![fig-1](docs/feasibility-process-05.png)

10. After arrival, the ZARS FHIR communication server will send the `result` Task resource to the ZARS BPE via websocket subscription. The incoming `result` message will use its correlation ID to match the original `request` process to continue.

11. As part of this process, the MeasureReport resource is fetched from the DIZ.

12. The fetched MeasureReport resource is stored immediately on the ZARS FHIR communication server together with the updated Task resource. The Task resource references the MeasureReport resource in its output parameter, in order to make it available to the initial requester.

## Supported Query Types

This process supports the following query types within the transferred Library resource:

| Type              | Description                                                                                                                                              | Mime Type                  |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| CQL               | Standardized query format. See https://cql.hl7.org/ for more information.                                                                                | `text/cql`                 |
| Structured Query  | Internal query representation as defined in [this project](https://github.com/medizininformatik-initiative/feasibility-structured-query) in JSON format. | `application/sq+json`      |
| FHIR Search Query | Standardized FHIR search query. See https://www.hl7.org/fhir/search.html for more information.                                                           | `application/x-fhir-query` |

**Note**: _Although a FHIR search query can be transferred to the process no result will be calculated!_

## Result Obfuscation

The process ensures obfuscation of a DIZ's real evaluation numbers by rounding them to the nearest ten. Real numbers are solely present in an intermediary step for obfuscation purposes. None of these non obfuscated numbers get persisted unless obfuscation gets explicitly disabled.

## Request Process as BPMN Model

![fig-1](./docs/feasibilityRequest.png)

In the [Business Process Model and Notation][7] (BPMN) model of the `request` process, the start message is the `request` message. After selecting the request targets (the DIZ'es), the `execute` messages are send in the next step. After that a subprocess is started for each target, which will wait for the `result` message to arrive. After the result message of each DIZ is stored immediately, the results are aggregated. After Subprocess the task resource is prepared for further evaluation.

## Execute Process as BPMN Model

![fig-1](./docs/feasibilityExecute.png)

The BPMN model of the `execute` process is straightforward and already explained in detail above.

## Configuration

Besides the [common DSF settings controlled by different environment variables][8], there are some additional ones specific to this process.

**All of them share the same prefix `DE_MEDIZININFORMATIK_INITIATIVE_FEASIBILITY_DSF_PROCESS_`:**

| EnvVar                               | Description                                                                                                                                                                    | Default |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------|
| CLIENT_STORE_PROXY_HOST              | Forward proxy host.                                                                                                                                                            | `null`  |
| CLIENT_STORE_PROXY_PORT              | Forward proxy port.                                                                                                                                                            | ``      |
| CLIENT_STORE_PROXY_USERNAME          | Username for a forward proxy if it requires one.                                                                                                                               | `null`  |
| CLIENT_STORE_PROXY_PASSWORD          | Password for a forward proxy if it requires one.                                                                                                                               | `null`  |
| CLIENT_STORE_AUTH_BEARER_TOKEN       | Bearer token used for authentication against a client target. Do not prefix this with `Bearer `!                                                                               | `null`  |
| CLIENT_STORE_AUTH_BASIC_USERNAME     | Username for basic authentication against a FHIR server client target.                                                                                                         | `null`  |
| CLIENT_STORE_AUTH_BASIC_PASSWORD     | Password for basic authentication against a FHIR server client target.                                                                                                         | `null`  |
| CLIENT_STORE_TIMEOUT_CONNECT         | Timeout for establishing a connection to a FHIR server client target in `ms`.                                                                                                  | `2000`  |
| CLIENT_STORE_TIMEOUT_CONNECT_REQUEST | Timeout for requesting a connection to a FHIR server client target in `ms`.                                                                                                    | `20000` |
| CLIENT_STORE_TIMEOUT_SOCKET          | Timeout for blocking a read / write network operation to a FHIR server without failing in `ms`.                                                                                | `20000` |
| CLIENT_STORE_TRUST_STORE_PATH        | Path to a trust store used for connecting to a FHIR server. Necessary when using self-signed certificates.                                                                     | `null`  |
| CLIENT_STORE_TRUST_STORE_PASSWORD    | Password for opening the trust store used for connecting to a FHIR server.                                                                                                     | `null`  |
| CLIENT_STORE_KEY_STORE_PATH          | Path to a key store used for authenticating against a FHIR server or proxy using a client certificate.                                                                         | `null`  |
| CLIENT_STORE_KEY_STORE_PASSWORD      | Password for opening the key store used for authenticating against a FHIR server or proxy.                                                                                     | `null`  |
| CLIENT_STORE_BASE_URL                | Base URL to a FHIR server or proxy for feasibility evaluation. This can also be the base URL of a reverse proxy if used. Only required if evaluation strategy is set to `cql`. | ``      |
| CLIENT_FLARE_BASE_URL                | Base URL to a FLARE instance. Only required if evaluation strategy is set to `structured-query`.                                                                               | ``      |
| CLIENT_FLARE_TIMEOUT_CONNECT         | Timeout for establishing a connection to a FLARE client target in `ms`.                                                                                                        | `2000`  |
| EVALUATION_STRATEGY                  | Defines whether the feasibility shall be evaluated using `cql` or `structured-query`. Using the latter requires a FLARE instance.                                              | `cql`   |
| EVALUATION_OBFUSCATE                 | Defines whether the feasibility evaluation result shall be obfuscated.                                                                                                         | `true`  |
| EVALUATION_OBFUSCATION_SENSITIVITY   | Sets the sensitivity of the Laplace distribution function used for obfuscating the result.                                                                                     | `1.0`   |
| EVALUATION_OBFUSCATION_EPSILON       | Sets the epsilon value of the Laplace distribution function used for obfuscating the result.                                                                                   | `0.5`   |
| RATE_LIMIT_COUNT                     | Sets the hard limit for the maximum allowed number of requests during the configured rate limit interval after no further requests will be processed                           | `999`   |
| RATE_LIMIT_INTERVAL_DURATION         | Sets the size of the time window used for calculating the request rate. The value is required to be given in the [ISO 8601 format][10] (e.g. "PT1H30M10S").                    | `PT1H`  |

## Compatibility

This version of the process is compatible with the following components:

| Component | Compatible Version(s) |
|-----------|-----------------------|
| DSF FHIR  | `0.9.0`               |
| DSF BPE   | `0.9.0`               |
| Blaze     | `>= 0.12`             |
| Flare     | `1.0`                 |

**Note:** Flare got rewritten. Only the [new project][9] is supported.


[1]: <https://www.hl7.org/FHIR/task.html>
[2]: <https://www.hl7.org/fhir/measure.html>
[3]: <https://www.hl7.org/fhir/library.html>
[4]: <https://www.hl7.org/fhir/bundle.html>
[5]: <https://www.hl7.org/fhir/operation-measure-evaluate-measure.html>
[6]: <https://www.hl7.org/fhir/measurereport.html>
[7]: <https://en.wikipedia.org/wiki/Business_Process_Model_and_Notation>
[8]: <https://github.com/highmed/highmed-dsf/wiki/DSF-0.5.5-Configuration-Parameters#dsf-bpe>
[9]: <https://github.com/rwth-imi/flare-query>
[10]: <https://en.wikipedia.org/wiki/ISO_8601#Durations>
