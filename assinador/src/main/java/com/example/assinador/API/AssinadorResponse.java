package com.example.assinador.API;

import java.util.Map;

public record AssinadorResponse(
        String endpoint,
        String servico,
        String resultado,
        Map<String, Object> dadosRecebidos
) {
}