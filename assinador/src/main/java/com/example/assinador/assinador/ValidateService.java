package com.example.assinador.assinador;

import org.springframework.stereotype.Service;

import com.example.assinador.API.ValidateRequest;
import com.example.assinador.API.ValidateResponse;

@Service("validate")
public class ValidateService implements IValidateService {

    private static final String FAKE_SIGNATURE = "MOCKED_SIGNATURE_BASE64_==";

    @Override
    public ValidateResponse validate(ValidateRequest request) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            return new ValidateResponse(false, "Parâmetro 'content' inválido ou ausente");
        }
        if (request.signature() == null || request.signature().isBlank()) {
            return new ValidateResponse(false, "Parâmetro 'signature' inválido ou ausente");
        }

        boolean isValid = FAKE_SIGNATURE.equals(request.signature());
        String message = isValid ? "Assinatura é válida" : "Assinatura é inválida";

        return new ValidateResponse(isValid, message);
    }
}