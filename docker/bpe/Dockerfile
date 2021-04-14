FROM ghcr.io/highmed/bpe:0.5.0-m1

# /opt/bpe is set as a workdir by the base image
COPY --chown=root:java ./app/conf ./conf/
COPY --chown=root:java ./plugin/*.jar ./plugin/
COPY --chown=root:java ./process/*.jar ./process/
COPY --chown=root:java start.sh start.sh

USER root
RUN chmod 730 ./conf && \
    chmod 750 ./process ./plugin
USER java

ENV DB_LIQUIBASE_USER=liquibase_user
ENV DB_SERVER_USER_GROUP=bpe_users
ENV DB_SERVER_USER=bpe_server_user
ENV DB_CAMUNDA_USER_GROUP=camunda_users
ENV DB_CAMUNDA_USER=camunda_server_user
ENV WEBSERVICE_READ_TIMEOUT=20000
ENV WEBSERVICE_CONNECT_TIMEOUT=2000
ENV WEBSERVICE_REMOTE_READ_TIMEOUT=20000
ENV WEBSERVICE_REMOTE_CONNECT_TIMEOUT=2000

ENTRYPOINT ["./start.sh"]
