# Docker Test Setup

This directory contains a `docker-compose.yml` describing a ZARS part and multiple different example setups regarding a
DIC. These example setups shall illustrate different supported functionalities of this plugin and their corresponding
configuration settings.

### Custom FHIR Server Image

Before you can start any site, you have to run maven in order to build plugins, certificates and test data:

```sh
./rebuild.sh
```

If you use Linux, you have to set some rights on directories where containers should write into:

```sh
./set-rights.sh
```

# Debugging

## Set debug environment in IntelliJ:
1. Edit Configurations...
1. New Configuration: Remote JVM Debug with a name like `remote broker_dic_5_bpe debugging`
   1. Use module classpath: `mii-process-feasibility`

#### Quick restart of the debugging only broker-bpe-app (change stack):
```sh
docker-compose down -v broker-bpe-app

docker-compose -f docker-compose.yml -f docker-compose.debug.broker-bpe.yml up -d broker-bpe-app
```

### Quick restart of the debugging the broker-bpe-app stack:
```sh
docker-compose down -v  

docker-compose -f docker-compose.yml up -d zars-fhir-app 
sleep 3

docker-compose -f docker-compose.yml up -d zars-bpe-app 

docker-compose -f docker-compose.yml up -d broker-fhir-app
sleep 3

docker-compose -f docker-compose.yml -f docker-compose.debug.broker-bpe.yml up -d broker-bpe-app
```

#### After that we can POST the first Task to the ZARS:
```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -H content-type:application/fhir+json \
  -d @data/feasibility-bundle.json \
  -s https://zars/fhir/ |\
  jq .
```

#### Start Debugging 'remote broker-bpe-app debugging'

Check replacement de.medizininformatik_initiative.feasibility_dsf_process.feasibility.parent.organization_identifier_value with "distributed-org.de"

```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker/fhir/ActivityDefinition" | jq .
```

```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker/fhir/Measure" | jq .
```

```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker/fhir/Library" | jq .
```

### Now you can start the debugger with `docker-compose up -d broker-dic-5-bpe-app`.

#### Start Debugging 'remote broker-dic-5-bpe-app debugging'

_______

### Quick restart of the debugging the broker-bpe-app and broker-dic-5-bpe-app stack:
```sh
docker-compose down -v  

docker-compose -f docker-compose.yml up -d zars-fhir-app 
sleep 3

docker-compose -f docker-compose.yml up -d zars-bpe-app 

docker-compose -f docker-compose.yml -f docker-compose.debug.broker-bpe.yml up -d broker-fhir-app
sleep 3

docker-compose -f docker-compose.yml -f docker-compose.debug.broker-bpe.yml up -d broker-bpe-app

docker-compose up -d broker-dic-5-fhir-app 
sleep 2

docker-compose -f docker-compose.yml -f docker-compose.debug.broker-dic-5-bpe.yml up -d broker-dic-5-bpe-app

docker-compose -f docker-compose.yml up -d broker-dic-6-fhir-app
sleep 2

docker-compose -f docker-compose.yml up -d broker-dic-6-bpe-app
```
#### Now you can start the debugger with `docker-compose up -d broker-dic-5-bpe-app`.

### After that we can POST the first Task to the ZARS:

```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -H content-type:application/fhir+json \
  -d @data/feasibility-bundle.json \
  -s https://zars/fhir/ |\
  jq .
```

Add Testdata:
```sh
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-001.json  http://broker-dic-6-store:8086/fhir
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-002.json  http://broker-dic-6-store:8086/fhir 
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-003.json  http://broker-dic-6-store:8086/fhir
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-004.json  http://broker-dic-6-store:8086/fhir 
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-005.json  http://broker-dic-6-store:8086/fhir
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-006.json  http://broker-dic-6-store:8086/fhir 
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-007.json  http://broker-dic-6-store:8086/fhir
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-008.json  http://broker-dic-6-store:8086/fhir 

curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-009.json  http://broker-dic-5-store:8085/fhir
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-010.json  http://broker-dic-5-store:8085/fhir 
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-011.json  http://broker-dic-5-store:8085/fhir
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-012.json  http://broker-dic-5-store:8085/fhir 
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-013.json  http://broker-dic-5-store:8085/fhir
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-014.json  http://broker-dic-5-store:8085/fhir 
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-015.json  http://broker-dic-5-store:8085/fhir
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-016.json  http://broker-dic-5-store:8085/fhir 
curl -H accept:application/fhir+json  -H content-type:application/fhir+json  -d @data/POLAR_TestData-017.json  http://broker-dic-5-store:8085/fhir 

```

```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://zars/fhir/Task/${TASK_ID}" |\
  jq .
```
```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker/fhir/Task/${TASK_ID}" |\
  jq .
```
```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker-dic-5/fhir/Task/${TASK_ID}" |\
  jq .
```
```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker/fhir/Organization" |\
  jq .
```
```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker-dic-5/fhir/MeasureReport/66460b2e-642d-4a74-ae50-97cab28b8562/_history/1" |\
  jq .
```
```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker-dic-6-store:8086/fhir/Patient" |\
  jq .
```
```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker-dic-5-store:8085/fhir/Patient" |\
  jq .
```
```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker/fhir/ActivityDefinition" |\
  jq .
```
```sh
curl \
  --cacert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/ca/testca_certificate.pem \
  --cert-type P12 \
  --cert ../mii-process-feasibility-tools/mii-process-feasibility-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.p12:password \
  -H accept:application/fhir+json \
  -s "https://broker/fhir/Task/712ab360-213a-442a-8013-f1dcb41d6355" |\
  jq .
```


WARN pool-4-thread-3 - FhirWebserviceClientJersey.handleError(101) | 
OperationOutcome: ERROR PROCESSING Value is 'http://medizininformatik-initiative.de/bpe/Process/feasibilityExecute|0.0' but must be 'http://medizininformatik-initiative.de/bpe/Process/feasibilityRequest|0.0'
2024-08-08T16:12:58.725422341Z

WARN pool-4-thread-3 - AbstractTaskMessageSend.doExecute(212) | 
Task http://medizininformatik-initiative.de/bpe/Process/feasibilityExecute|0.0 send failed [recipient: Test_Broker, endpoint: Test_Broker_Endpoint, businessKey: 600db7d6-ff7c-4545-b21e-2f6a371bddc1, correlationKey: e94d1925-1f22-49eb-8718-fae29a4a89cd, message: feasibilitySingleDicResultMessage, error: jakarta.ws.rs.WebApplicationException - ERROR PROCESSING Value is 'http://medizininformatik-initiative.de/bpe/Process/feasibilityExecute|0.0' but must be 'http://medizininformatik-initiative.de/bpe/Process/feasibilityRequest|0.0']
2024-08-08T16:12:58.730677216Z

ERROR pool-4-thread-3 - AbstractTaskMessageSend.handleEndEventError(246) | 
Process medizininformatik-initiativede_feasibilityExecute:1:10 has fatal error in step sendDicResponse:153 for task https://broker-dic-5/fhir/Task/b02bef1f-b97f-423b-8a63-b4fe46447b9c, 
reason: jakarta.ws.rs.WebApplicationException - ERROR PROCESSING Value is 'http://medizininformatik-initiative.de/bpe/Process/feasibilityExecute|0.0' but must be 'http://medizininformatik-initiative.de/bpe/Process/feasibilityRequest|0.0'


[1]: <https://www.hl7.org/fhir/capabilitystatement.html>

[2]: <https://curl.se>
   