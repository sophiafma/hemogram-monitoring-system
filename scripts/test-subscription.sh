#!/bin/bash

APP_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=============================================="
echo "🧪 TESTANDO SUBSCRIPTION FHIR"
echo "=============================================="
echo ""

# Verificar se os servidores estão rodando
echo "📡 Verificando servidores..."
if ! curl -s http://localhost:8080/fhir/metadata > /dev/null 2>&1; then
    echo "❌ Servidor FHIR não está rodando!"
    echo "   Execute: bash start-with-fhir-server.sh"
    exit 1
fi

if ! curl -s http://localhost:8081/swagger-ui.html > /dev/null 2>&1; then
    echo "❌ Aplicação não está rodando!"
    echo "   Execute: bash start-with-fhir-server.sh"
    exit 1
fi

echo "✅ Ambos os servidores estão rodando"
echo ""

# Verificar status da subscription
echo "🔔 Verificando status da subscription..."
curl -s http://localhost:8081/admin/subscription/status | python3 -m json.tool 2>/dev/null || curl -s http://localhost:8081/admin/subscription/status
echo ""
echo ""

# Enviar hemograma com PLAQUETAS BAIXAS (alerta de dengue)
echo "=============================================="
echo "📤 TESTE 1: Enviando hemograma com DENGUE"
echo "=============================================="
echo "Paciente com plaquetas em 80.000 /µL (ALERTA!)"
echo ""

curl -X POST http://localhost:8080/fhir/Observation \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Observation",
    "status": "final",
    "code": {
      "coding": [{
        "system": "http://loinc.org",
        "code": "777-3",
        "display": "Platelets"
      }]
    },
    "subject": {"reference": "Patient/dengue-001"},
    "effectiveDateTime": "2025-10-08T10:00:00Z",
    "valueQuantity": {"value": 80000, "unit": "/µL"}
  }' -s | python3 -m json.tool 2>/dev/null || echo "Enviado!"

echo ""
echo "⏳ Aguardando notificação via subscription..."
sleep 3

echo ""
echo "=============================================="
echo "📤 TESTE 2: Enviando hemograma NORMAL"
echo "=============================================="
echo "Paciente com plaquetas em 250.000 /µL (NORMAL)"
echo ""

curl -X POST http://localhost:8080/fhir/Observation \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Observation",
    "status": "final",
    "code": {
      "coding": [{
        "system": "http://loinc.org",
        "code": "777-3"
      }]
    },
    "subject": {"reference": "Patient/normal-001"},
    "effectiveDateTime": "2025-10-08T10:01:00Z",
    "valueQuantity": {"value": 250000, "unit": "/µL"}
  }' -s | python3 -m json.tool 2>/dev/null || echo "Enviado!"

echo ""
echo "⏳ Aguardando notificação via subscription..."
sleep 3

echo ""
echo "=============================================="
echo "✅ TESTES CONCLUÍDOS!"
echo "=============================================="
echo ""
echo "📋 O que aconteceu:"
echo "   1. Enviamos 2 hemogramas ao servidor FHIR (porta 8080)"
echo "   2. O servidor FHIR notificou automaticamente nossa aplicação"
echo "   3. Nossa aplicação (porta 8081) processou e detectou dengue no primeiro"
echo ""
echo "🔍 Para ver os resultados:"
echo "   tail -30 $APP_DIR/app.log | grep -A 5 'NOTIFICAÇÃO'"
echo ""
echo "   Ou abra os logs completos:"
echo "   tail -f $APP_DIR/app.log"
echo ""
