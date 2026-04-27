package com.example.assinador.API;

import java.util.Map;

public record AssinadorRequest(
        Map<String, Object> dados
) {
}