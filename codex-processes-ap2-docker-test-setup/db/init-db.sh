#!/usr/bin/env bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE zars_fhir;
    GRANT ALL PRIVILEGES ON DATABASE zars_fhir TO liquibase_user;
    CREATE DATABASE zars_bpe;
    GRANT ALL PRIVILEGES ON DATABASE zars_bpe TO liquibase_user;
    CREATE DATABASE dic_1_fhir;
    GRANT ALL PRIVILEGES ON DATABASE dic_1_fhir TO liquibase_user;
    CREATE DATABASE dic_1_bpe;
    GRANT ALL PRIVILEGES ON DATABASE dic_1_bpe TO liquibase_user;
    CREATE DATABASE dic_2_fhir;
    GRANT ALL PRIVILEGES ON DATABASE dic_2_fhir TO liquibase_user;
    CREATE DATABASE dic_2_bpe;
    GRANT ALL PRIVILEGES ON DATABASE dic_2_bpe TO liquibase_user;
EOSQL
