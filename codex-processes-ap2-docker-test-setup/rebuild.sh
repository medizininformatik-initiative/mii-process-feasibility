#!/usr/bin/env sh

mvn -f ../codex-process-feasibility/pom.xml clean package
mvn -f ../codex-processes-ap2-tools/codex-processes-ap2-test-data-generator/pom.xml clean package
