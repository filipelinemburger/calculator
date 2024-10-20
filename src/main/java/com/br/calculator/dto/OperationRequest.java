package com.br.calculator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperationRequest {

    private String operationType;
    private Double value1;
    private Double value2;

    public OperationRequest(String operationType, Double value1) {
        this.operationType = operationType;
        this.value1 = value1;
    }


}
