package com.stefanini.ceptracker.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CepResponse implements Serializable {

    private String cep;

    @JsonProperty("logradouro")
    private String street;

    @JsonProperty("complemento")
    private String complement;

    @JsonProperty("bairro")
    private String neighborhood;

    @JsonProperty("localidade")
    private String city;

    @JsonProperty("uf")
    private String state;

    @JsonProperty("ibge")
    private String ibgeCode;

    @JsonProperty("gia")
    private String giaCode;

    @JsonProperty("ddd")
    private String areaCode;

    @JsonProperty("siafi")
    private String siafiCode;

    private Boolean erro;
}