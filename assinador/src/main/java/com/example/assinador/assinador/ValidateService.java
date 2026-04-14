package com.example.assinador.assinador;

import org.springframework.stereotype.Service;

@Service("validate")
public class validate implements IValidateService {

    @Override
    public String getNomeServico() {
        return "validate";
    }
}