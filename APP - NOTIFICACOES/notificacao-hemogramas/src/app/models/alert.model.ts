export interface DengueAlert {
  titulo: string;    // Ex: "Alerta de Surto"
  mensagem: string;  // Ex: "Focos de dengue detectados no seu bairro."
  regiao: string;    // Ex: "Zona Sul" ou "Bairro Centro"
  severidade: 'aviso' | 'perigo'; // Para mudar a cor (amarelo/vermelho)
  data: Date;
}
