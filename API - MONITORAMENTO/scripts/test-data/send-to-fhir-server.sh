#!/bin/bash

# Script para enviar hemogramas para o servidor FHIR local
# O servidor FHIR vai notificar automaticamente a aplicação via subscription

set -euo pipefail

FHIR_SERVER_URL="${FHIR_SERVER_URL:-http://localhost:8080/fhir}"
JSON_DIR="$(cd "$(dirname "$0")/json-examples" && pwd)"

echo "============================================================"
echo "📤 Enviando Bundles FHIR para o Servidor FHIR"
echo "============================================================"
echo ""
echo "→ Servidor FHIR: ${FHIR_SERVER_URL}"
echo "→ Diretório de JSONs: ${JSON_DIR}"
echo ""

# Verificar se o diretório existe
if [ ! -d "${JSON_DIR}" ]; then
  echo "❌ Diretório ${JSON_DIR} não encontrado."
  echo "   Rode primeiro: python3 scripts/test-data/generate_hemogram_bundles.py"
  exit 1
fi

# Verificar se há JSONs
shopt -s nullglob
JSON_FILES=("${JSON_DIR}"/*.json)
shopt -u nullglob

if [ ${#JSON_FILES[@]} -eq 0 ]; then
  echo "❌ Nenhum JSON encontrado em ${JSON_DIR}."
  echo "   Rode primeiro: python3 scripts/test-data/generate_hemogram_bundles.py"
  exit 1
fi

# Verificar se servidor FHIR está rodando
echo "📡 Verificando servidor FHIR em ${FHIR_SERVER_URL}..."
if ! curl -s --head "${FHIR_SERVER_URL}/metadata" >/dev/null 2>&1; then
  echo "❌ Servidor FHIR não está acessível!"
  echo "   Inicie o servidor FHIR primeiro:"
  echo "   bash scripts/start-with-fhir-server.sh"
  exit 1
fi
echo "✅ Servidor FHIR acessível!"
echo ""

SUCCESS=0
FAIL=0

for file in "${JSON_FILES[@]}"; do
  name=$(basename "${file}")
  echo "------------------------------------------------------------"
  echo "📦 Enviando ${name} para servidor FHIR..."
  
  # Enviar Bundle para o servidor FHIR
  # O servidor vai processar e notificar a aplicação via subscription
  RESPONSE=$(curl -s -w "HTTPSTATUS:%{http_code}" \
    -X POST "${FHIR_SERVER_URL}/Bundle" \
    -H "Content-Type: application/json" \
    --data-binary "@${file}")
  
  BODY=$(echo "${RESPONSE}" | sed -e 's/HTTPSTATUS\:.*//g')
  STATUS=$(echo "${RESPONSE}" | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
  
  if [ "${STATUS}" = "201" ] || [ "${STATUS}" = "200" ]; then
    echo "✅ Sucesso (${STATUS}) - ${name}"
    echo "   Servidor FHIR processou e vai notificar a aplicação"
    SUCCESS=$((SUCCESS + 1))
  else
    echo "❌ Falha (${STATUS}) - ${name}"
    echo "   Resposta: ${BODY}"
    FAIL=$((FAIL + 1))
  fi
  
  # Pequeno delay para não sobrecarregar
  sleep 0.5
done

echo ""
echo "============================================================"
echo "Resumo do envio"
echo "============================================================"
echo "✔️  Sucessos : ${SUCCESS}"
echo "❌  Falhas   : ${FAIL}"
echo "============================================================"
echo ""
echo "📋 O que aconteceu:"
echo "   1. Bundles foram enviados para o servidor FHIR"
echo "   2. Servidor FHIR processou e salvou"
echo "   3. Subscription ativa vai notificar sua aplicação automaticamente"
echo ""
echo "🔍 Verifique os logs da aplicação para ver as notificações chegando!"

