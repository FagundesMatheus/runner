package com.example.assinador.API;

public record ValidateRequest(
        String content,
        String signature,
        String politicaAssinatura
) {
}