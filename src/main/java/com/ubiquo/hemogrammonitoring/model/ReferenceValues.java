package com.ubiquo.hemogrammonitoring.model;

public class ReferenceValues {
    
    // Valores de referência para hemograma (conforme especificação do projeto)
    public static final double LEUCOCITOS_MIN = 4000.0; // /µL
    public static final double LEUCOCITOS_MAX = 11000.0; // /µL
    
    public static final double HEMOGLOBINA_MIN = 12.0; // g/dL
    public static final double HEMOGLOBINA_MAX = 17.5; // g/dL
    
    public static final double PLAQUETAS_MIN = 150000.0; // /µL
    public static final double PLAQUETAS_MAX = 450000.0; // /µL
    
    public static final double HEMATOCRITO_MIN = 36.0; // %
    public static final double HEMATOCRITO_MAX = 52.0; // %
    
    // Códigos LOINC para identificação dos parâmetros
    public static final String LEUCOCITOS_LOINC = "33747-0"; // Leukocytes [#/volume] in Blood (quantidade absoluta em /µL)
    public static final String HEMOGLOBINA_LOINC = "718-7"; // Hemoglobin [Mass/volume] in Blood
    public static final String PLAQUETAS_LOINC = "777-3"; // Platelets [#/volume] in Blood
    public static final String HEMATOCRITO_LOINC = "4544-3"; // Hematocrit [Volume Fraction] of Blood
    
    private ReferenceValues() {
        // Classe utilitária - não deve ser instanciada
    }
    
    /**
     * Verifica se o valor de leucócitos está dentro da faixa normal
     */
    public static boolean isLeucocitosNormal(double value) {
        return value >= LEUCOCITOS_MIN && value <= LEUCOCITOS_MAX;
    }
    
    /**
     * Verifica se o valor de hemoglobina está dentro da faixa normal
     */
    public static boolean isHemoglobinaNormal(double value) {
        return value >= HEMOGLOBINA_MIN && value <= HEMOGLOBINA_MAX;
    }
    
    /**
     * Verifica se o valor de plaquetas está dentro da faixa normal
     */
    public static boolean isPlaquetasNormal(double value) {
        return value >= PLAQUETAS_MIN && value <= PLAQUETAS_MAX;
    }
    
    /**
     * Verifica se o valor de hematócrito está dentro da faixa normal
     */
    public static boolean isHematocritoNormal(double value) {
        return value >= HEMATOCRITO_MIN && value <= HEMATOCRITO_MAX;
    }
    
    /**
     * Verifica se as plaquetas estão baixas (indicativo de dengue)
     */
    public static boolean isPlaquetasBaixas(double value) {
        return value < PLAQUETAS_MIN;
    }
}
