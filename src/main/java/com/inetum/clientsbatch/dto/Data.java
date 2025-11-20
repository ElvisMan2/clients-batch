package com.inetum.clientsbatch.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@lombok.Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Data {
    private Long clientId;//generado por la API
    private String firstName;
    private String paternalLastName;
    private String maternalLastName;
    private String currencyOfIncome;
    private Double monthlyIncome;

    // Datos de la simulación
    private Long simulationId;//generado por la API
    private Double loanAmount;
    private String currency;
    private Double interestRate;
    private Integer term;
    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate disbursementDate;
    private Double monthlyPayment;//calculado por la API
    private Double totalPayment;//calculado por la API
    private Boolean approved;//calculado por la API

}
