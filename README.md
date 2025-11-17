# Sistema de Monitoramento de Hemogramas - Detecção de Dengue

Sistema para detecção precoce de surtos de dengue através do monitoramento automático de plaquetas em hemogramas, utilizando o padrão HL7 FHIR.

## Como Funciona

Dengue causa **queda de plaquetas**. Este sistema monitora hemogramas em tempo real e detecta automaticamente quando as plaquetas estão baixas (< 150.000/µL), emitindo alertas para áreas de maior risco.

**Fluxo:**
1. Laboratórios enviam hemogramas para um **Servidor FHIR** (central de dados)
2. Nossa aplicação **"assina"** esse servidor (cria uma subscription)
3. Quando chega hemograma novo, o servidor **notifica automaticamente** nossa aplicação via webhook
4. Nossa aplicação **processa** e **detecta** se há indício de dengue
5. **Alerta** é gerado para regiões com mais possibilidade de dengue para população tomar os cuidados. 

---

## Arquitetura

```
Servidor FHIR (Porta 8080)
    ↓ [Webhook quando chega hemograma novo]
FhirController (recebe notificação)
    ↓
FhirParserService (extrai dados + detecta dengue)
    ↓
HemogramData (armazena resultado)
```

**Estrutura do Código:**
```
controller/
├── FhirController.java           # Recebe webhooks do servidor
└── SubscriptionController.java   # Gerencia subscription

service/
├── FhirParserService.java        # Processa JSON e detecta dengue
└── FhirSubscriptionService.java  # Cria/gerencia subscription

model/
├── HemogramData.java             # Dados do hemograma
└── ReferenceValues.java          # Valores normais
```

---

## Marco 1 ✅ - Recepção FHIR (Implementado)

- ✅ **Subscription FHIR**: criação automática ao iniciar
- ✅ **Webhook**: recebe notificações do servidor FHIR
- ✅ **Parser FHIR**: extrai valores usando códigos LOINC
- ✅ **Detecção de Dengue**: alerta quando plaquetas < 150.000 /µL
- ✅ **API REST**: documentada com Swagger

**Parâmetros Monitorados:**

| Parâmetro | Código LOINC | Valor Normal | Alerta Dengue |
|-----------|--------------|--------------|---------------|
| Leucócitos | 33747-0 | 4.000-11.000 /µL | - |
| Hemoglobina | 718-7 | 12,0-17,5 g/dL | - |
| **Plaquetas** | **777-3** | **150.000-450.000 /µL** | **< 150.000** |
| Hematócrito | 4544-3 | 36-52% | - |

---

## Como Executar

### Pré-requisitos
- Java 17+
- Maven 3.6+

### Rodar
```bash
mvn spring-boot:run
```

A aplicação sobe na porta **8081** e automaticamente:
- Conecta no servidor FHIR configurado
- Cria uma subscription
- Aguarda notificações de novos hemogramas

**Acessar:**
- Swagger: http://localhost:8081/swagger-ui.html
- Status da Subscription: http://localhost:8081/admin/subscription/status

---

## Como Testar

### 1. Teste Rápido (Swagger)
Acesse http://localhost:8081/swagger-ui.html e teste o endpoint `GET /fhir/test-hemograma`

### 2. Via Terminal
```bash
# Teste com dados mockados
curl http://localhost:8081/fhir/test-hemograma

# Enviar hemograma com DENGUE (plaquetas 80.000)
curl -X POST http://localhost:8081/fhir/subscription \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "Observation",
    "subject": {"reference": "Patient/123"},
    "component": [{
      "code": {"coding": [{"system": "http://loinc.org", "code": "777-3"}]},
      "valueQuantity": {"value": 80000, "unit": "/µL"}
    }]
  }'

# Verificar status da subscription
curl http://localhost:8081/admin/subscription/status
```

---

## Scripts de Teste com Bundles FHIR

Para demonstrações mais realistas, há scripts que geram Bundles completos (formato SES-GO) e os enviam para a aplicação.

### Gerar os JSONs
```bash
python test-data/scripts/generate_hemogram_bundles.py
```
- Gera 20 arquivos em `test-data/json-examples/` (leucócitos absolutos, plaquetas, hemoglobina, hematócrito e bairros de Goiânia).
- Os arquivos são **ignorados pelo Git** (`test-data/json-examples/*.json` no `.gitignore`), evitando versões antigas no repositório.

### Enviar automaticamente para a API
```bash
bash test-data/scripts/send_hemogram_bundles.sh
```
- Requer a aplicação rodando em `http://localhost:8081`.
- Faz POST em `/fhir/direct-test` para cada arquivo gerado e mostra um resumo de sucessos/falhas.

Use esses scripts para popular rapidamente o banco antes de acessar os dashboards analíticos.

---

## Configuração do Servidor FHIR

A aplicação precisa se conectar a um **Servidor FHIR**. Configure em `src/main/resources/application.yml`:

### Opção 1: Servidor Local (Recomendado)
```yaml
hemogram:
  monitoring:
    fhir:
      server-url: "http://localhost:8080/fhir"
      subscription:
        callback-url: "http://localhost:8081/fhir/subscription"
```

1. Baixe o HAPI FHIR Server: https://github.com/hapifhir/hapi-fhir-jpaserver-starter
2. Execute em outra porta (8080)
3. Use os scripts prontos:
```bash
bash scripts/start-with-fhir-server.sh    # Inicia servidor + aplicação
bash scripts/test-subscription.sh         # Envia hemogramas de teste
```
*Ajuste os caminhos no início dos scripts antes de usar*

### Opção 2: Servidor Público
```yaml
hemogram:
  monitoring:
    fhir:
      server-url: "https://hapi.fhir.org/baseR4"
      subscription:
        callback-url: "http://SEU-IP-PUBLICO:8081/fhir/subscription"
```
⚠️ O `callback-url` precisa ser acessível pela internet (use ngrok/cloudflare tunnel)

---

## Tecnologias

- **Spring Boot 3.2.0** - Framework
- **HAPI FHIR 6.8.0** - Cliente FHIR
- **SpringDoc OpenAPI 2.2.0** - Swagger
- **H2 Database** - Banco em memória
- **Maven** - Gerenciamento de dependências

---

## Licença

Projeto acadêmico - Universidade Federal de Goiás (UFG)
