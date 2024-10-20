package com.br.calculator.enums;

public enum OperationTypeEnum {
    ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, SQUARE_ROOT, RANDOM_STRING;

    public static OperationTypeEnum fromString(String value) {

        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        try {
            return OperationTypeEnum.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown operation type: " + value);
        }
    }

}
