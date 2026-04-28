package com.example.assinador.API;

public record AssinadorResponse(
        String signature,
        boolean valid,
        String message
) {
}