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

## Debugging

You can use the following env var and port mapping to enable debugging of one container:

```
ports:
- 5005:5005
environment:
  EXTRA_JVM_ARGS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
```

### Quick restart of the debugging the broker-dic-5-bpe-app stack:
```sh
docker-compose down -v 

docker-compose up -d zars-fhir-app 
until docker-compose exec zars-fhir-app sh -c 'exit $(docker inspect -f {{.State.Health.Status}} mii-process-feasibility-docker-test-setup-zars-fhir-app-1)' == "healthy"; do
    sleep 1
done

docker-compose up -d zars-bpe-app 
until docker-compose exec zars-bpe-app sh -c 'exit $(docker inspect -f {{.State.Health.Status}} mii-process-feasibility-docker-test-setup-zars-bpe-app-1)' == "healthy"; do
    sleep 1
done

docker-compose up -d broker-fhir-app
until docker-compose exec broker-fhir-app sh -c 'exit $(docker inspect -f {{.State.Health.Status}} mii-process-feasibility-docker-test-setup-broker-fhir-app-1)' == "healthy"; do
    sleep 1
done

docker-compose up -d broker-bpe-app
until docker-compose exec broker-bpe-app sh -c 'exit $(docker inspect -f {{.State.Health.Status}} mii-process-feasibility-docker-test-setup-broker-bpe-app-1)' == "healthy"; do
    sleep 1
done

docker-compose up -d broker-dic-5-fhir-app

```
#### Now you can start the debugger with `docker-compose up -d broker-dic-5-bpe-app`.

Set debug environment in IntelliJ:
1. Edit Configurations...
1. New Configuration: Docker - Docker Compose with a name like `broker-dic-5-bpe-app up`.
   1. Server: your local Docker
   1. Compose files: `/mii-process-feasibility-docker-test-setup/docker-compose.yml;`
   1. Services: `broker-dic-5-bpe-app,`
1. New Configuration: Remote JVM Debug with a name like `remote broker_dic_5_bpe debugging`
   1. Use module classpath: `mii-process-feasibility`
   1. Before launch: Run Another Configuration: `broker-dic-5-bpe-app up`

#### Start Debugging 'remote broker-dic-5-bpe-app debugging'

### Quick restart of the debugging the broker-bpe-app stack:
```sh
docker-compose down -v 

docker-compose up -d zars-fhir-app 
until docker-compose exec zars-fhir-app sh -c 'exit $(docker inspect -f {{.State.Health.Status}} mii-process-feasibility-docker-test-setup-zars-fhir-app-1)' == "healthy"; do
    sleep 1000
done

docker-compose up -d zars-bpe-app 

docker-compose up -d broker-fhir-app

docker-compose up -d broker-dic-5-fhir-app
until docker-compose exec broker-dic-5-fhir-app sh -c 'exit $(docker inspect -f {{.State.Health.Status}} mii-process-feasibility-docker-test-setup-broker-dic-5-fhir-app-1)' == "healthy"; do
    sleep 1000
done

docker-compose up -d broker-dic-5-bpe-app


```
#### Now you can start the debugger with `docker-compose up -d broker-bpe-app`.



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


[1]: <https://www.hl7.org/fhir/capabilitystatement.html>

[2]: <https://curl.se>
   