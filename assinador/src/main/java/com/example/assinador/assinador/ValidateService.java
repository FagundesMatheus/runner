package com.example.assinador.assinador;

import org.springframework.stereotype.Service;

@Service("validate")
public class ValidateService implements IValidateService {

    @Override
    public String validate(
            String assinaturaPath,
            String politicaAssinatura
    ) {
        return "validate";
    }
}