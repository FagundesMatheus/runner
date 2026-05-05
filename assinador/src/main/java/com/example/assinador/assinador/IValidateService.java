package com.example.assinador.assinador;

import com.example.assinador.API.ValidateRequest;
import com.example.assinador.API.ValidateResponse;

public interface IValidateService {
    ValidateResponse validar(ValidateRequest request);
}