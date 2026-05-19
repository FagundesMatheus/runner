package com.example.assinador.API;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record AssinadorResponse(
        String signature,
        boolean valid,
        String message,
        @JsonIgnore Integer statusCode 
) {}