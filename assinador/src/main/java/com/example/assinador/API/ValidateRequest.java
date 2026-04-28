package com.example.assinador.API;

public record ValidateRequest(
        String assinaturaPath,
        String politicaAssinatura
) {
}
