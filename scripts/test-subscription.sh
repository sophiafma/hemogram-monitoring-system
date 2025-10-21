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

    # Gerar valor de plaquetas aleatório com distribuição
    # 1/3 de chance de ser DENGUE, 1/3 NORMAL, 1/3 ALTO
    RAND_CASE=$((RANDOM % 3))
    if [ $RAND_CASE -eq 0 ]; then
        # CASO DENGUE
        PLAQUETAS=$(shuf -i 20000-149000 -n 1)
        STATUS="⚠️ DENGUE"
    elif [ $RAND_CASE -eq 1 ]; then
        # CASO NORMAL
        PLAQUETAS=$(shuf -i 150000-450000 -n 1)
        STATUS="✅ NORMAL"
    else
        # CASO ALTO (apenas para variar os dados)
        PLAQUETAS=$(shuf -i 451000-600000 -n 1)
        STATUS="⬆️ ALTO"
    fi

    echo "   Paciente: ${PATIENT_NAME} (CPF: ${PATIENT_CPF})"
    echo "   Plaquetas: ${PLAQUETAS} /µL (${STATUS})"

    # Construir o JSON FHIR com o recurso Patient contido
    JSON_PAYLOAD=$(cat <<EOF
{
    "resourceType": "Observation",
    "id": "hemograma-sim-${i}",
    "status": "final",
    "contained": [
        {
            "resourceType": "Patient",
            "id": "${PATIENT_ID}",
            "name": [{"text": "${PATIENT_NAME}"}],
            "identifier": [{
                "system": "urn:oid:2.16.840.1.113883.4.642.3.1",
                "value": "${PATIENT_CPF}"
            }],
            "telecom": [{
                "system": "phone",
                "value": "${PATIENT_PHONE}"
            }]
        }
    ],
    "code": {
        "coding": [{
            "system": "http://loinc.org",
            "code": "777-3",
            "display": "Platelets"
        }]
    },
    "subject": {"reference": "#${PATIENT_ID}"},
    "effectiveDateTime": "${TIMESTAMP}",
    "valueQuantity": {"value": ${PLAQUETAS}, "unit": "/µL"}
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

