# Codex-Processes-AP2 (FHIR part)

Docker files for the FHIR part of this project.

## Configuration

|EnvVar|Description|Default|
|------|-----------|-------|
|DB_URL|JDBC compliant database URL for a Postgres database. Has to be in the form of `jdbc:postgresql://<HOST>[:<PORT>]/<DATABASE>`||
|DB_LIQUIBASE_USER| |`liquibase_user`|
|DB_LIQUIBASE_USER_PASSWORD| ||
|DB_SERVER_USER_GROUP| |`fhir_users`|
|DB_SERVER_USER| |`fhir_server_user`|
|DB_SERVER_USER_PASSWORD| ||
|CORS_ORIGINS| ||
|ORGANIZATION_IDENTIFIER| Identifier of the organization that this container is deployed in. Must not contain whitespace. ||
|ORGANIZATION_TYPE| Describes the type of the organization the container is deployed in. ||
|WEBSERVICE_BASE_URL| Base URL for reaching the the FHIR server. ||
|WEBSERVICE_P12_CERTIFICATE| Certificate bundle in the PKCS12 format used for webservice requests. ||
|WEBSERVICE_P12_CERTIFICATE_PASSWORD| Password for the certificate bundle. ||
|WEBSERVICE_REMOTE_READ_TIMEOUT| Read timeout in `ms` when requesting a remote webservice. |`10000`|
|WEBSERVICE_REMOTE_CONNECT_TIMEOUT| Connection timeout in `ms` when connecting to a remote webservice. |`2000`|
|WEBSERVICE_DEFAULT_PAGE_COUNT| Default number of items per page returned by the FHIR server. |`20`|
|FHIR_INIT_BUNDLE| Path to a FHIR bundle for initialization purposes. Has to be in XML format. ||
|USER_THUMBPRINTS| Comma separated list of thumbprints of client certificates that are allowed to access the FHIR server. ||
