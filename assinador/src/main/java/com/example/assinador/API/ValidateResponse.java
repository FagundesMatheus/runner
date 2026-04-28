package com.example.assinador.API;

public record ValidateResponse(
        boolean valid,
        String message
) {
}