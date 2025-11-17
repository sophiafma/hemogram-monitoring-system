#!/bin/bash

set -euo pipefail

APP_URL="${APP_URL:-http://localhost:8081}"
ENDPOINT="${ENDPOINT:-/fhir/direct-test}"
TARGET_URL="${APP_URL}${ENDPOINT}"
JSON_DIR="$(cd "$(dirname "$0")/../json-examples" && pwd)"

echo "============================================================"
echo "📤 Envio automático de Bundles FHIR gerados para testes"
echo "============================================================"
echo ""
echo "→ API alvo: ${TARGET_URL}"
echo "→ Diretório de JSONs: ${JSON_DIR}"
echo ""

if [ ! -d "${JSON_DIR}" ]; then
  echo "❌ Diretório ${JSON_DIR} não encontrado."
  echo "   Rode primeiro: test-data/scripts/generate_hemogram_bundles.py"
  exit 1
fi

shopt -s nullglob
JSON_FILES=("${JSON_DIR}"/*.json)
shopt -u nullglob

if [ ${#JSON_FILES[@]} -eq 0 ]; then
  echo "❌ Nenhum JSON encontrado em ${JSON_DIR}."
  echo "   Rode primeiro: test-data/scripts/generate_hemogram_bundles.py"
  exit 1
fi

echo "📡 Verificando aplicação em ${APP_URL}..."
if ! curl -s --head "${APP_URL}/swagger-ui.html" >/dev/null 2>&1; then
  echo "❌ Aplicação não está acessível. Inicie o Spring Boot antes."
  exit 1
fi
echo "✅ Aplicação acessível!"
echo ""

SUCCESS=0
FAIL=0

for file in "${JSON_FILES[@]}"; do
  name=$(basename "${file}")
  echo "------------------------------------------------------------"
  echo "📦 Enviando ${name} ..."

  RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" \
    -X POST "${TARGET_URL}" \
    -H "Content-Type: application/json" \
    --data-binary "@${file}")

  BODY=$(echo "${RESPONSE}" | sed -e 's/HTTPSTATUS\:.*//g')
  STATUS=$(echo "${RESPONSE}" | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')

  if [ "${STATUS}" = "200" ]; then
    echo "✅ Sucesso (${STATUS}) - ${name}"
    SUCCESS=$((SUCCESS + 1))
  else
    echo "❌ Falha (${STATUS}) - ${name}"
    echo "   Resposta: ${BODY}"
    FAIL=$((FAIL + 1))
  fi
done

echo ""
echo "============================================================"
echo "Resumo do envio"
echo "============================================================"
echo "✔️  Sucessos : ${SUCCESS}"
echo "❌  Falhas   : ${FAIL}"
echo "============================================================"

