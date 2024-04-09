#!/usr/bin/env sh

#mvn -f mii-process-feasibility/pom.xml clean package -DskipTests=true
#docker-compose -f mii-process-feasibility-docker-test-setup/docker-compose.yml down -v

docker-compose -f mii-process-feasibility-docker-test-setup/docker-compose.yml up -d zars-fhir-app
# Wait for the successful health check from service zars-fhir-app

until docker-compose -f mii-process-feasibility-docker-test-setup/docker-compose.yml exec zars-fhir-app \
                  sh -c 'exit $( \
                    docker inspect -f {{.State.Health.Status}} mii-process-feasibility-docker-test-setup-zars-fhir-app-1 \
                  )' == "healthy"; do
    sleep 1
done

docker-compose -f mii-process-feasibility-docker-test-setup/docker-compose.yml up -d zars-bpe-app
# Wait for the successful health check from service zars-bpe-app
until docker-compose -f mii-process-feasibility-docker-test-setup/docker-compose.yml exec zars-bpe-app sh -c 'exit $(docker inspect -f {{.State.Health.Status}} zars-bpe-app)' == "healthy"; do
    sleep 1
done

docker-compose -f mii-process-feasibility-docker-test-setup/docker-compose.yml up -d dic-1-fhir-app
# Wait for the successful health check from service dic-1-fhir-app
until docker-compose -f mii-process-feasibility-docker-test-setup/docker-compose.yml exec dic-1-fhir-app sh -c 'exit $(docker inspect -f {{.State.Health.Status}} dic-1-fhir-app)' == "healthy"; do
    sleep 1
done

docker-compose -f mii-process-feasibility-docker-test-setup/docker-compose.yml up -d dic-1-bpe-app
# Wait for the successful health check from service dic-1-bpe-app
until docker-compose -f mii-process-feasibility-docker-test-setup/docker-compose.yml exec dic-1-bpe-app sh -c 'exit $(docker inspect -f {{.State.Health.Status}} dic-1-bpe-app)' == "healthy"; do
    sleep 1
done