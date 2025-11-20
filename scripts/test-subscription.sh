#!/bin/bash

# =============================================================================
# SCRIPT DE SIMULAÇÃO DE ENVIO EM MASSA DE HEMOGRAMAS (VERSÃO DIRETA)
# -----------------------------------------------------------------------------
# Este script envia múltiplos hemogramas diretamente para o endpoint de teste
# da aplicação (/fhir/direct-test), ignorando o servidor FHIR.
# Isso é útil para testar a lógica de processamento e análise da aplicação
# de forma isolada.
# =============================================================================

# --- CONFIGURAÇÕES ---
APP_URL="http://localhost:8081"
ENDPOINT="/fhir/direct-test"
TARGET_URL="${APP_URL}${ENDPOINT}"
CONTENT_TYPE="application/json"

# Número de hemogramas a serem enviados
NUM_HEMOGRAMS=10

# Atraso entre os envios (em segundos)
DELAY=1

# --- INÍCIO DO SCRIPT ---
echo "======================================================"
echo "💉 SIMULADOR DE ENVIO DIRETO DE HEMOGRAMAS"
echo "======================================================"
echo ""

# 1. Verificar se a aplicação está rodando
echo "📡 Verificando se a aplicação está rodando em ${APP_URL}..."
# Tenta um endpoint comum do Spring Actuator, se não existir, usa o swagger como fallback
if ! curl -s --head ${APP_URL}/actuator/health > /dev/null 2>&1 && ! curl -s ${APP_URL}/swagger-ui.html > /dev/null 2>&1; then
    echo "❌ Aplicação não está rodando ou não está acessível!"
    echo "   Por favor, inicie a aplicação Spring Boot antes de rodar este script."
    exit 1
fi
echo "✅ Aplicação está rodando!"
echo ""
echo "🎯 Alvo do teste: ${TARGET_URL}"
echo "📦 Total de hemogramas a enviar: ${NUM_HEMOGRAMS}"
echo ""

# 2. Loop para enviar os hemogramas
for i in $(seq 1 $NUM_HEMOGRAMS)
do
    echo "------------------------------------------------------"
    echo "📤 Enviando Hemograma #${i} de ${NUM_HEMOGRAMS}..."

    # Gerar dados dinâmicos
    PATIENT_ID="sim-patient-$(printf "%03d" $i)"
    PATIENT_NAME="Paciente Simulado $(printf "%03d" $i)"
    PATIENT_CPF=$(shuf -i 10000000000-99999999999 -n 1)
    PATIENT_PHONE="629$(shuf -i 80000000-99999999 -n 1)"
    TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    # Gerar região aleatória (bairros/regiões de Goiânia)
    REGIOES=("Setor Bueno" "Setor Oeste" "Setor Sul" "Setor Marista" "Jardim Goiás" "Centro" "Setor Aeroporto" "Vila Nova" "Jardim América" "Parque Amazônia")
    REGIAO_INDEX=$((RANDOM % ${#REGIOES[@]}))
    REGIAO="${REGIOES[$REGIAO_INDEX]}"

    # Gerar valor de plaquetas E LEUCÓCITOS aleatório
    # 1/3 de chance de ser DENGUE, 1/3 NORMAL, 1/3 ALTO
    RAND_CASE=$((RANDOM % 3))
    if [ $RAND_CASE -eq 0 ]; then
        # CASO DENGUE (Ambos baixos)
        PLAQUETAS=$(shuf -i 20000-149000 -n 1)
        LEUCOCITOS=$(shuf -i 1500-3900 -n 1)
        STATUS="⚠️ DENGUE"
    elif [ $RAND_CASE -eq 1 ]; then
        # CASO NORMAL
        PLAQUETAS=$(shuf -i 150000-450000 -n 1)
        LEUCOCITOS=$(shuf -i 4500-10500 -n 1)
        STATUS="✅ NORMAL"
    else
        # CASO ALTO (apenas para variar os dados)
        PLAQUETAS=$(shuf -i 451000-600000 -n 1)
        LEUCOCITOS=$(shuf -i 12000-18000 -n 1)
        STATUS="⬆️ ALTO"
    fi

    echo "   Paciente: ${PATIENT_NAME} (CPF: ${PATIENT_CPF})"
    echo "   Região: ${REGIAO}"
    echo "   Plaquetas: ${PLAQUETAS} /µL | Leucócitos: ${LEUCOCITOS} /µL (${STATUS})"

    # Construir o JSON FHIR do tipo Bundle (Pacote)
    # Motivo: Precisamos enviar DUAS Observations (Plaquetas + Leucócitos) juntas.
    JSON_PAYLOAD=$(cat <<EOF
{
  "resourceType": "Bundle",
  "type": "collection",
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "${PATIENT_ID}",
        "name": [{"text": "${PATIENT_NAME}"}],
        "identifier": [{ "system": "https://fhir.saude.go.gov.br/sid/cpf", "value": "${PATIENT_CPF}" }],
        "telecom": [{ "system": "phone", "value": "${PATIENT_PHONE}" }],
        "address": [{ "city": "${REGIAO}", "state": "GO", "country": "BR" }]
      }
    },
    {
      "resource": {
        "resourceType": "Observation",
        "status": "final",
        "code": { "coding": [{ "system": "http://loinc.org", "code": "777-3", "display": "Platelets" }] },
        "subject": { "reference": "Patient/${PATIENT_ID}" },
        "effectiveDateTime": "${TIMESTAMP}",
        "valueQuantity": { "value": ${PLAQUETAS}, "unit": "/µL" }
      }
    },
    {
      "resource": {
        "resourceType": "Observation",
        "status": "final",
        "code": { "coding": [{ "system": "http://loinc.org", "code": "33747-0", "display": "Leukocytes" }] },
        "subject": { "reference": "Patient/${PATIENT_ID}" },
        "effectiveDateTime": "${TIMESTAMP}",
        "valueQuantity": { "value": ${LEUCOCITOS}, "unit": "/µL" }
      }
    }
  ]
}
EOF
)

    # Enviar a requisição POST diretamente para a aplicação
    curl -s -X POST "${TARGET_URL}" \
         -H "Content-Type: ${CONTENT_TYPE}" \
         -d "${JSON_PAYLOAD}" > /dev/null

    echo "   Enviado com sucesso!"
    sleep $DELAY
done

echo ""
echo "======================================================"
echo "✅ TESTES CONCLUÍDOS!"
echo "======================================================"
echo ""
echo "📋 O que aconteceu:"
echo "   1. Este script enviou ${NUM_HEMOGRAMS} hemogramas com dados variados."
echo "   2. Os dados foram enviados DIRETAMENTE para a sua aplicação na porta 8081."
echo "   3. O servidor FHIR não foi utilizado neste teste."
echo ""
echo "🔍 Para ver os resultados, verifique os logs da sua aplicação Spring Boot."
echo ""