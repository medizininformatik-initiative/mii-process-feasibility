FROM ghcr.io/highmed/fhir:0.5.0-m1

# /opt/fhir is set as a workdir by the base image
COPY --chown=root:java ./app/conf ./conf/
COPY --chown=root:java start.sh start.sh

USER root
RUN chmod 730 ./conf
USER java

ENV DB_LIQUIBASE_USER=liquibase_user
ENV DB_SERVER_USER_GROUP=fhir_users
ENV DB_SERVER_USER=fhir_server_user
ENV WEBSERVICE_REMOTE_READ_TIMEOUT=10000
ENV WEBSERVICE_REMOTE_CONNECT_TIMEOUT=2000
ENV WEBSERVICE_DEFAULT_PAGE_COUNT=20

ENTRYPOINT ["./start.sh"]
