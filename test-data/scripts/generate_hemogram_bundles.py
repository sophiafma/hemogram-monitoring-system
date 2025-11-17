#!/usr/bin/env python3
"""
Script para gerar 20 exemplos de FHIR Bundle para hemogramas
Seguindo o formato SES-GO (Bundle completo com múltiplas Observations)
"""

import json
import uuid
from datetime import datetime, timedelta
import os

# Bairros de Goiânia para variar nos exemplos
BAIRROS_GOIANIA = [
    "Setor Bueno", "Setor Oeste", "Setor Sul", "Setor Marista",
    "Jardim Goiás", "Centro", "Setor Aeroporto", "Vila Nova",
    "Jardim América", "Parque Amazônia", "Setor Campinas",
    "Setor Leste Vila Nova", "Jardim Novo Mundo", "Setor Coimbra",
    "Parque Industrial", "Setor Universitário", "Jardim Bela Vista",
    "Setor Pedro Ludovico", "Setor Criméia", "Setor Faiçalville"
]

# Cenários de teste
CENARIOS = [
    {"nome": "normal", "leucocitos": (5000, 9000), "plaquetas": (200000, 350000), "hemoglobina": (13.0, 16.0), "hematocrito": (38.0, 48.0)},
    {"nome": "dengue", "leucocitos": (2000, 4000), "plaquetas": (30000, 140000), "hemoglobina": (12.0, 15.0), "hematocrito": (36.0, 45.0)},
    {"nome": "infeccao", "leucocitos": (12000, 20000), "plaquetas": (150000, 300000), "hemoglobina": (12.5, 16.5), "hematocrito": (37.0, 49.0)},
    {"nome": "anemia", "leucocitos": (4000, 8000), "plaquetas": (180000, 300000), "hemoglobina": (8.0, 11.5), "hematocrito": (28.0, 35.0)},
    {"nome": "leucopenia", "leucocitos": (1500, 3500), "plaquetas": (160000, 280000), "hemoglobina": (13.0, 16.0), "hematocrito": (38.0, 48.0)},
]

def gerar_uuid():
    """Gera um UUID no formato urn:uuid:..."""
    return f"urn:uuid:{uuid.uuid4()}"

def gerar_cpf():
    """Gera um CPF aleatório (apenas para teste)"""
    import random
    return f"{random.randint(10000000000, 99999999999)}"

def gerar_data_coleta(dias_atras=0):
    """Gera data de coleta (padrão: hoje, ou dias atrás)"""
    data = datetime.now() - timedelta(days=dias_atras)
    return data.strftime("%Y-%m-%dT%H:%M:%S-03:00")

def gerar_data_emissao(dias_atras=0):
    """Gera data de emissão (geralmente 1-2 dias após coleta)"""
    data = datetime.now() - timedelta(days=dias_atras) + timedelta(days=1)
    return data.strftime("%Y-%m-%dT%H:%M:%S-03:00")

def criar_observation_simples(obs_id, loinc_code, display, value, unit, system="http://unitsofmeasure.org", 
                              cpf_paciente="01234567891", data_coleta=None, data_emissao=None, cnes="2337991", bairro=None):
    """Cria uma Observation simples (exame individual)"""
    if data_coleta is None:
        data_coleta = gerar_data_coleta()
    if data_emissao is None:
        data_emissao = gerar_data_emissao()
    
    uuid_obs = gerar_uuid()
    
    # Determinar valores de referência baseado no código LOINC
    reference_range = []
    if loinc_code == "33747-0":  # Leucócitos
        reference_range = [{
            "low": {"value": 4000, "system": system, "code": "/uL"},
            "high": {"value": 11000, "system": system, "code": "/uL"},
            "type": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/referencerange-meaning", "code": "normal"}]}
        }]
    elif loinc_code == "718-7":  # Hemoglobina
        reference_range = [{
            "low": {"value": 12.0, "system": system, "code": "g/dL"},
            "high": {"value": 17.5, "system": system, "code": "g/dL"},
            "type": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/referencerange-meaning", "code": "normal"}]}
        }]
    elif loinc_code == "777-3":  # Plaquetas
        reference_range = [{
            "low": {"value": 150000, "system": system, "code": "/uL"},
            "high": {"value": 450000, "system": system, "code": "/uL"},
            "type": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/referencerange-meaning", "code": "normal"}]}
        }]
    elif loinc_code == "4544-3":  # Hematócrito
        reference_range = [{
            "low": {"value": 36.0, "system": system, "code": "%"},
            "high": {"value": 52.0, "system": system, "code": "%"},
            "type": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/referencerange-meaning", "code": "normal"}]}
        }]
    
    obs = {
        "fullUrl": uuid_obs,
        "resource": {
            "resourceType": "Observation",
            "id": obs_id,
            "meta": {
                "profile": ["https://fhir.saude.go.gov.br/r4/core/StructureDefinition/exame-simples"]
            },
            "contained": [{
                "resourceType": "Specimen",
                "id": "amostra",
                "type": {
                    "coding": [{
                        "system": "http://terminology.hl7.org/CodeSystem/v2-0487",
                        "code": "BLD"
                    }]
                },
                "collection": {
                    "collectedDateTime": data_coleta
                }
            }],
            "status": "final",
            "category": [{
                "coding": [{
                    "system": "http://www.saude.gov.br/fhir/r4/CodeSystem/BRSubgrupoTabelaSUS",
                    "code": "0202"
                }]
            }],
            "code": {
                "coding": [{
                    "system": "http://loinc.org",
                    "code": loinc_code,
                    "display": display
                }]
            },
            "subject": {
                "identifier": {
                    "system": "https://fhir.saude.go.gov.br/sid/cpf",
                    "value": cpf_paciente
                }
            },
            "issued": data_emissao,
            "performer": [{
                "id": "laboratorio",
                "identifier": {
                    "system": "https://fhir.saude.go.gov.br/sid/cnes",
                    "value": cnes
                }
            }],
            "valueQuantity": {
                "value": value,
                "system": system,
                "code": unit
            },
            "method": {
                "text": "Automatizado – Cell-Dyn Ruby, Abbott e Microscopia"
            },
            "specimen": {
                "reference": "#amostra"
            },
            "referenceRange": reference_range
        }
    }
    
    # Adicionar extensão com bairro se fornecido
    if bairro:
        if "extension" not in obs["resource"]:
            obs["resource"]["extension"] = []
        obs["resource"]["extension"].append({
            "url": "https://fhir.saude.go.gov.br/r4/core/StructureDefinition/bairro",
            "valueString": bairro
        })
    
    return obs, uuid_obs

def criar_bundle_hemograma(numero, cenario, bairro, valores):
    """Cria um Bundle completo de hemograma"""
    import random
    
    # Gerar identificadores únicos
    bundle_uuid = str(uuid.uuid4())
    cpf_paciente = gerar_cpf()
    data_coleta = gerar_data_coleta(dias_atras=random.randint(0, 7))
    data_emissao = gerar_data_emissao(dias_atras=random.randint(0, 7))
    
    # UUIDs para as Observations
    uuid_composto = gerar_uuid()
    uuid_leucocitos = gerar_uuid()
    uuid_hemoglobina = gerar_uuid()
    uuid_plaquetas = gerar_uuid()
    uuid_hematocrito = gerar_uuid()
    
    # Criar Observations individuais
    obs_leucocitos, _ = criar_observation_simples(
        "leucocitos", "33747-0", "Leukocytes [#/volume] in Blood",
        valores["leucocitos"], "/uL", cpf_paciente=cpf_paciente,
        data_coleta=data_coleta, data_emissao=data_emissao, bairro=bairro
    )
    obs_leucocitos["fullUrl"] = uuid_leucocitos
    
    obs_hemoglobina, _ = criar_observation_simples(
        "hemoglobina", "718-7", "Hemoglobin [Mass/volume] in Blood",
        valores["hemoglobina"], "g/dL", cpf_paciente=cpf_paciente,
        data_coleta=data_coleta, data_emissao=data_emissao, bairro=bairro
    )
    obs_hemoglobina["fullUrl"] = uuid_hemoglobina
    
    obs_plaquetas, _ = criar_observation_simples(
        "plaquetas", "777-3", "Platelets [#/volume] in Blood",
        valores["plaquetas"], "/uL", cpf_paciente=cpf_paciente,
        data_coleta=data_coleta, data_emissao=data_emissao, bairro=bairro
    )
    obs_plaquetas["fullUrl"] = uuid_plaquetas
    
    obs_hematocrito, _ = criar_observation_simples(
        "hematocrito", "4544-3", "Hematocrit [Volume Fraction] of Blood",
        valores["hematocrito"], "%", cpf_paciente=cpf_paciente,
        data_coleta=data_coleta, data_emissao=data_emissao, bairro=bairro
    )
    obs_hematocrito["fullUrl"] = uuid_hematocrito
    
    # Observation composta (exame-composto)
    obs_composto = {
        "fullUrl": uuid_composto,
        "resource": {
            "resourceType": "Observation",
            "id": "exame-composto",
            "meta": {
                "profile": ["https://fhir.saude.go.gov.br/r4/core/StructureDefinition/exame-composto"]
            },
            "contained": [{
                "resourceType": "Specimen",
                "id": "amostra",
                "type": {
                    "coding": [{
                        "system": "http://terminology.hl7.org/CodeSystem/v2-0487",
                        "code": "BLD"
                    }]
                },
                "collection": {
                    "collectedDateTime": data_coleta
                }
            }],
            "status": "final",
            "category": [{
                "coding": [{
                    "system": "http://www.saude.gov.br/fhir/r4/CodeSystem/BRSubgrupoTabelaSUS",
                    "code": "0202"
                }]
            }],
            "code": {
                "coding": [{
                    "system": "http://loinc.org",
                    "code": "58410-2",
                    "display": "Complete blood count (CBC) panel - Blood by Automated count"
                }]
            },
            "subject": {
                "identifier": {
                    "system": "https://fhir.saude.go.gov.br/sid/cpf",
                    "value": cpf_paciente
                }
            },
            "issued": data_emissao,
            "performer": [{
                "id": "laboratorio",
                "identifier": {
                    "system": "https://fhir.saude.go.gov.br/sid/cnes",
                    "value": "2337991"
                }
            }],
            "specimen": {
                "reference": "#amostra"
            },
            "hasMember": [
                {"reference": uuid_leucocitos},
                {"reference": uuid_hemoglobina},
                {"reference": uuid_plaquetas},
                {"reference": uuid_hematocrito}
            ]
        }
    }
    
    # Criar Bundle
    bundle = {
        "resourceType": "Bundle",
        "meta": {
            "profile": ["https://fhir.saude.go.gov.br/r4/exame/StructureDefinition/hemograma"]
        },
        "identifier": {
            "system": "https://fhir.go.gov.br/sid/romulo-rocha",
            "value": bundle_uuid
        },
        "type": "collection",
        "entry": [
            obs_composto,
            obs_leucocitos,
            obs_hemoglobina,
            obs_plaquetas,
            obs_hematocrito
        ]
    }
    
    return bundle

def main():
    """Gera 20 JSONs de Bundle de hemogramas"""
    import random
    
    # Criar diretório de saída
    output_dir = os.path.join(os.path.dirname(__file__), "..", "json-examples")
    os.makedirs(output_dir, exist_ok=True)
    
    print("=" * 60)
    print("Gerando 20 exemplos de FHIR Bundle para hemogramas")
    print("=" * 60)
    print()
    
    for i in range(1, 21):
        # Selecionar cenário e bairro
        cenario = random.choice(CENARIOS)
        bairro = random.choice(BAIRROS_GOIANIA)
        
        # Gerar valores aleatórios dentro do cenário
        valores = {
            "leucocitos": round(random.uniform(*cenario["leucocitos"]), 0),
            "plaquetas": round(random.uniform(*cenario["plaquetas"]), 0),
            "hemoglobina": round(random.uniform(*cenario["hemoglobina"]), 1),
            "hematocrito": round(random.uniform(*cenario["hematocrito"]), 1)
        }
        
        # Criar Bundle
        bundle = criar_bundle_hemograma(i, cenario["nome"], bairro, valores)
        
        # Salvar JSON
        filename = f"hemograma-{i:02d}-{cenario['nome']}-{bairro.replace(' ', '_')}.json"
        filepath = os.path.join(output_dir, filename)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(bundle, f, indent=2, ensure_ascii=False)
        
        print(f"✅ Gerado: {filename}")
        print(f"   Cenário: {cenario['nome']} | Bairro: {bairro}")
        print(f"   Valores: Leuc={valores['leucocitos']:.0f}, Plaq={valores['plaquetas']:.0f}, "
              f"Hb={valores['hemoglobina']:.1f}, Ht={valores['hematocrito']:.1f}")
        print()
    
    print("=" * 60)
    print(f"✅ Todos os 20 JSONs foram gerados em: {output_dir}")
    print("=" * 60)

if __name__ == "__main__":
    main()

