#!/usr/bin/env sh

mvn -f ../codex-process-feasibility/pom.xml clean package
mvn -f ../codex-processes-tools/codex-processes-test-data-generator/pom.xml clean package
