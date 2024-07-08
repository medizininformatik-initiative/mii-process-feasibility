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

docker-compose -f docker-compose.yml up -d broker-fhir-app
sleep 3

docker-compose -f docker-compose.yml -f docker-compose.debug.broker-bpe.yml up -d broker-bpe-app

docker-compose -f docker-compose.yml up -d broker-dic-6-fhir-app
sleep 2

docker-compose -f docker-compose.yml up -d broker-dic-6-bpe-app

docker-compose up -d broker-dic-5-fhir-app
sleep 2

docker-compose -f docker-compose.yml -f docker-compose.debug.broker-dic-5-bpe.yml up -d broker-dic-5-bpe-app
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

After exporting the Task ID to $TASK_ID, you can fetch the task:

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


[1]: <https://www.hl7.org/fhir/capabilitystatement.html>

[2]: <https://curl.se>
   