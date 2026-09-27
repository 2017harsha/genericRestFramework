#!/usr/bin/env bash
# Stores the prod API password in AWS SSM Parameter Store as an encrypted SecureString.
# Run once (by an admin) from a machine with the AWS CLI configured:
#   ./aws/create-prod-secret.sh
set -euo pipefail

PARAM_NAME="${PARAM_NAME:-/api-test-framework/prod/api-password}"
REGION="${AWS_REGION:-ap-south-1}"

read -r -s -p "Prod API password: " PASSWORD
echo

aws ssm put-parameter \
  --region "$REGION" \
  --name "$PARAM_NAME" \
  --type SecureString \
  --value "$PASSWORD" \
  --overwrite \
  --description "Prod API password used by the REST Assured test framework"

echo "Stored $PARAM_NAME in $REGION. Verify (masked) with:"
echo "  aws ssm get-parameter --region $REGION --name $PARAM_NAME --with-decryption --query Parameter.Name"
