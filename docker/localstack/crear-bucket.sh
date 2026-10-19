#!/bin/bash
# LocalStack ejecuta este script cuando está listo (montado en /etc/localstack/init/ready.d)
set -euo pipefail
awslocal s3 mb "s3://${APP_BUCKET:-facturacion-local}" || true
