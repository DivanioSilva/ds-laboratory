package com.example.demo.dto;

public record PersonImportResponse(
        String status,
        long read,
        long written,
        long ignored) {
}
