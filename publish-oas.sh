#!/usr/bin/env bash

# Local publish url & auth-key
URL=http://localhost:11114/integration-catalogue-admin-api
AUTH_KEY=dGVzdC1hdXRoLWtleQ==

# DEV publish url & auth-key
# URL=https://admin.development.tax.service.gov.uk/integration-catalogue-admin-api
# AUTH_KEY=

# QA publish url & auth-key
# URL=https://admin.qa.tax.service.gov.uk/integration-catalogue-admin-api
# AUTH_KEY=

# Staging publish url & auth-key
# URL=https://admin.staging.tax.service.gov.uk/integration-catalogue-admin-api
# AUTH_KEY=

# ExternalTest publish url & auth-key
# URL=https://test-admin.tax.service.gov.uk/integration-catalogue-admin-api
# AUTH_KEY=

# Production publish url & auth-key
# URL=https://admin.tax.service.gov.uk/integration-catalogue-admin-api
# AUTH_KEY=

echo $URL

# Generated from swagger-ui
curl -X 'PUT' \
  $URL'/services/apis/publish' \
  -H 'accept: application/json' \
  -H 'x-specification-type: OAS_V3' \
  -H 'x-platform-type: API_PLATFORM' \
  -H 'Authorization: '$AUTH_KEY \
  -H 'Content-Type: multipart/form-data' \
  -F 'selectedFile=@publish-oas.yaml;type=application/x-yaml'

