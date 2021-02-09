# NUM CODEX Feasibility Process

The feasibility process is used to run a distributed feasibility query. A feasibility query contains multiple criteria and returns one population count per organization. The feasibility query is requested by one organization (usually the Zentrale Antrags- und Registerstelle (ZARS)) and executed in multiple organizations (usually Datenintegrationszentren (DIZ)).

## High-Level Overview

![fig-1](./docs/codex-feasibility-process-01.png)

As you can see in the figure above, the feasibility query process starts with a `request` message (1) that the ZARS or any other initiating organization sends to itself. By sending this message to the ZARS itself, the business process engine (BPE) of the ZARS can be used to distribute the `execute` messages (2) to all DIZ'es in question. After receiving the `execute` messages, each DIZ will calculate the result and answer with a `result` message (3). The ZARS will accumulate the results and provide the requester with live updates.

## Request and Execute Process in Detail

Messages, queries and results are represented by FHIR resources. The following three figures show the entire FHIR resource flow. In difference to the figure above, only one DIZ is represented, but both, the ZARS and the DIZ are divided into its individual components. On the ZARS side the components are the FHIR communication server and the BPE, were the DIZ contains an additional Blaze FHIR server.

![fig-1](./docs/codex-feasibility-process-02.png)

Like in the high-level overview, the process starts with the `request` message (1). Beside the FHIR [Task][1] resource for the message, two other FHIR resources will be sent to the ZARS FHIR server. The first one is the [Measure][2] resource specifying the population criteria that are defined in the second resource, the [Library][3]. All three resources are put into a transaction [Bundle][4] in order to create all together in one transaction.

After the ZARS FHIR server receives the resources, it notifies the ZARS BPE via websocket subscription by transmitting the Task resource (2). The incoming Task resource starts the `request` process that implements the query distribution to all appropriate DIZ'es. To each DIZ, an `execute` message (3) is sent via a Task resource.

After arriving at the DIZ FHIR communication server, the `execure` Task resource is transferred to the DIZ BPE via  websocket subscription (4), starting the `execute` prozess.

![fig-1](./docs/codex-feasibility-process-03.png)

In each DIZ, the `execute` process starts by fetching the Measure and Library resource (5) created at the ZARS FHIR communication server. The resources have to be fetched by the BPE because only Task resources are sent actively between organizations and message payload is only fetched in case a process really needs it. FHIR search is used in order to fetch both resources in one HTTP request by searching for the Measure resource and including the referenced Library resource.

In the next step, the `execute` process stores the Measure and Library resources (6) to the Blaze FHIR server in order to be able to execute the [$evaluate-measure][5] operation. The resulting [MeasureReport][6] resource is transferred back to the DIZ BPE (7).

After receiving the MeasureReport, the DIZ BPE stores it on the DIZ FHIR communication server (8) in order to make it available for the ZARS.

In its last step, the `execute` process sends a `result` message (9) to the ZARS. The `result` message references the MeasureReport, so that it can be retrieved by the ZARS.

![fig-1](./docs/codex-feasibility-process-04.png)

After arrival, the ZARS FHIR communication server will send the `result` Task resource to the ZARS BPE via websocket subscription (10). The incoming `result` message will use its correlation ID to match the original `request` process to continue. As part of this process, the MeasureReport resource will be fetched from the DIZ (11) and stored on the ZARS FHIR communication server (12). 

As last step, the original `request` Task is updated with a reference to the MeasureReport in its output parameter, so that the requester can obtain it.

## Request Process as BPMN Model

![fig-1](./docs/requestSimpleFeasibility.png)

In the BPMN model of the `request` process, the start message it the `request` message. After selecting the request targets (the DIZ'es), the `execute` messages are send in the next step. After that a subprocess is started for each target, which will wait for the `result` messages to arrive.

__TODO:__ [Support Live Result Updates](https://github.com/num-codex/codex-processes-ap2/issues/6)

## Execute Process as BPMN Model

![fig-1](./docs/executeSimpleFeasibility.png)

The BPMN model of the `execute` process is strait forward an already explained in detail above.

[1]: <https://www.hl7.org/FHIR/task.html>
[2]: <https://www.hl7.org/fhir/measure.html>
[3]: <https://www.hl7.org/fhir/library.html>
[4]: <https://www.hl7.org/fhir/bundle.html>
[5]: <https://www.hl7.org/fhir/operation-measure-evaluate-measure.html>
[6]: <https://www.hl7.org/fhir/measurereport.html>
