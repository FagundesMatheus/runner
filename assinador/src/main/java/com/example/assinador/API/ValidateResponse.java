package com.example.assinador.API;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record ValidateResponse(
        boolean valid,
        String message,
        @JsonIgnore Integer statusCode
) {}