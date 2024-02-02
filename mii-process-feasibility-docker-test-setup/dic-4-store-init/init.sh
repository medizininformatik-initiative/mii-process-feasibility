#!/usr/bin/env sh

echo -n "Waiting for FHIR server to be online and healthy..."
status_code=0
while true; do
  status_code=$(curl -s -o /dev/null -w "%{http_code}" "http://dic-4-store:8080/fhir/metadata")
  if [ "$status_code" -eq 200 ]; then
    break
  fi
done
echo "DONE"

echo -n "Adding library model definition to FHIR server..."
status_code=$(curl -X PUT -s -o /dev/null -w "%{http_code}" -H "Content-Type: application/fhir+json" -d @/tmp/library-fhir-model-definition.json "http://dic-4-store:8080/fhir/Library/fhir-model-definition")
if [ "$status_code" -ne 201 ]; then
  echo "FAILED"
  exit 1
else
  echo "DONE"
fi

echo -n "Adding library FHIR helpers to FHIR server..."
status_code=$(curl -s -o /dev/null -w "%{http_code}" -H "Content-Type: application/fhir+json" -d @/tmp/library-fhir-helpers.json "http://dic-4-store:8080/fhir/Library")
if [ "$status_code" -ne 201 ]; then
  echo "FAILED"
  exit 1
else
  echo "DONE"
fi
