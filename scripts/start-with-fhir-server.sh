#!/bin/bash

# ANTES DE EXECUTAR: Ajuste as variáveis abaixo para o seu ambiente
FHIR_SERVER_DIR="C:\Users\Computador\Desktop\Projetos\UBIQUO\hapi-fhir-jpaserver-starter"  # Ajuste para onde está o servidor HAPI FHIR
APP_DIR="$(cd "$(dirname "$0")" && pwd)"  # Diretório deste script (automático)

echo "=============================================="
echo "🚀 INICIANDO SISTEMA COM SERVIDOR FHIR LOCAL"
echo "=============================================="
echo ""

# Verificar se o servidor FHIR já está rodando
if curl -s http://localhost:8080/fhir/metadata > /dev/null 2>&1; then
    echo "✅ Servidor FHIR já está rodando em http://localhost:8080/fhir"
else
    echo "⏳ Iniciando servidor FHIR local..."
    echo "   (Isso pode levar 1-2 minutos na primeira vez)"
    
    if [ ! -d "$FHIR_SERVER_DIR" ]; then
        echo "❌ ERRO: Diretório do servidor FHIR não encontrado: $FHIR_SERVER_DIR"
        echo "   Ajuste a variável FHIR_SERVER_DIR no início deste script"
        exit 1
    fi
    
    cd "$FHIR_SERVER_DIR"
    
    # Iniciar servidor FHIR em background
    mvn spring-boot:run > hapi-server.log 2>&1 &
    FHIR_PID=$!
    echo "   PID do servidor FHIR: $FHIR_PID"
    
    # Aguardar servidor iniciar
    echo "   Aguardando servidor FHIR iniciar..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/fhir/metadata > /dev/null 2>&1; then
            echo "✅ Servidor FHIR iniciado com sucesso!"
            break
        fi
        echo -n "."
        sleep 2
    done
    echo ""
fi

echo ""
echo "⏳ Iniciando aplicação de monitoramento de hemogramas..."
cd "$APP_DIR"

# Parar aplicação se já estiver rodando
pkill -f "hemogram-monitoring" 2>/dev/null

# Iniciar aplicação
mvn spring-boot:run > app.log 2>&1 &
APP_PID=$!
echo "   PID da aplicação: $APP_PID"

# Aguardar aplicação iniciar
echo "   Aguardando aplicação iniciar..."
for i in {1..30}; do
    if curl -s http://localhost:8081/swagger-ui.html > /dev/null 2>&1; then
        echo "✅ Aplicação iniciada com sucesso!"
        break
    fi
    echo -n "."
    sleep 2
done
echo ""

echo ""
echo "=============================================="
echo "✅ SISTEMA COMPLETO FUNCIONANDO!"
echo "=============================================="
echo ""
echo "🌐 URLs disponíveis:"
echo "   • Aplicação (Swagger): http://localhost:8081/swagger-ui.html"
echo "   • Status Subscription:  http://localhost:8081/admin/subscription/status"
echo "   • Servidor FHIR:        http://localhost:8080/fhir"
echo ""
echo "📝 Logs:"
echo "   • Aplicação: tail -f $APP_DIR/app.log"
echo "   • Servidor FHIR: tail -f $FHIR_SERVER_DIR/hapi-server.log"
echo ""
echo "🧪 Para testar, abra outro terminal e execute:"
echo "   bash $APP_DIR/test-subscription.sh"
echo ""
echo "🛑 Para parar tudo:"
echo "   pkill -f 'spring-boot:run'"
echo ""
