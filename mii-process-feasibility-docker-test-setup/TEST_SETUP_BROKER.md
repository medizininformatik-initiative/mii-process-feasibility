
# Docker Test Setup Broker


If you use Linux or Mac, you have to set some rights on directories where containers should write into:
```sh
./set-rights.sh
```

Add the following entries to `/etc/hosts`:
```
127.0.0.1       zars, broker-dic-5, broker-dic-6, broker
```

Before you can start any site, you have to run maven in order to build plugins, certificates and test data:
```sh
mvn -f ../pom.xml clean install -DskipTests=true 
```

Copy the built jar to the necessary Docker directories:
```sh
./broker-copy-process.sh
```

Quick restart of debugging for broker-bpe-app and broker-dic-5-bpe-app
```sh
docker-compose down -v  
sleep 5

# ZARS:
docker-compose -f docker-compose.yml up -d zars-bpe-app 

# Broker:
docker-compose -f docker-compose.yml -f docker-compose.debug.broker.yml up -d broker-bpe-app

# dic-5-fhir:
docker-compose -f docker-compose.yml -f docker-compose.debug.broker-dic-5.yml up -d broker-dic-5-bpe-app

# dic-6-fhir:
docker-compose -f docker-compose.yml -f docker-compose.debug.broker-dic-6.yml up -d broker-dic-6-bpe-app
```


After that we can POST the first Task to the ZARS

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

The task should be completed and contain an output with the reference of the MeasureReport created.

## Debugging

You can use the following env var and port mapping to enable debugging of one container:

```
ports:
- 5005:5005
environment:
  EXTRA_JVM_ARGS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

[1]: <https://www.hl7.org/fhir/capabilitystatement.html>

[2]: <https://curl.se>
