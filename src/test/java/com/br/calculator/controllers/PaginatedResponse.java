package com.br.calculator.controllers;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> content;
    private int totalPages;

    // Constructors, getters, and setters
    public PaginatedResponse(List<T> content, int totalPages) {
        this.content = content;
        this.totalPages = totalPages;
    }

    public List<T> getContent() {
        return content;
    }

    public int getTotalPages() {
        return totalPages;
    }
}

