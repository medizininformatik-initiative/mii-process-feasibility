#!/usr/bin/env sh
set -euo pipefail

BASE_DIR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

cp "${BASE_DIR}/../mii-process-feasibility/target/mii-process-feasibility-0.0.0.0.jar" "${BASE_DIR}/zars/bpe/process/"
cp "${BASE_DIR}/../mii-process-feasibility/target/mii-process-feasibility-0.0.0.0.jar" "${BASE_DIR}/broker/bpe/process/"
cp "${BASE_DIR}/../mii-process-feasibility/target/mii-process-feasibility-0.0.0.0.jar" "${BASE_DIR}/broker-dic-5/bpe/process/"
cp "${BASE_DIR}/../mii-process-feasibility/target/mii-process-feasibility-0.0.0.0.jar" "${BASE_DIR}/broker-dic-6/bpe/process/"
