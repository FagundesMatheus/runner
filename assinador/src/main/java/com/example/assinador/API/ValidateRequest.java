package com.example.assinador.API;

public record ValidateRequest(
        String conteudo,
        String assinatura,
        String politicaAssinaturaUrl
) {}