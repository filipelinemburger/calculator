package com.br.calculator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RecordResponse {
    private Long id;
    private Long operationId;
    private String operationType;
    private Integer operationCost;
    private Integer userBalance;
    private LocalDateTime date;
}
