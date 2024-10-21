package com.br.calculator.exceptions;

public class OperationException extends RuntimeException {
    public OperationException() {
    }

    public OperationException(String message) {
        super(message);
    }
}
