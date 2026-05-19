package com.example.assinador.assinador;

import org.springframework.stereotype.Service;

import com.example.assinador.API.ValidateRequest;
import com.example.assinador.API.ValidateResponse;

@Service("validate")
public class ValidateService implements IValidateService {

    @Override
    public ValidateResponse validar(ValidateRequest dados) {
        if (dados == null) {
            return new ValidateResponse(false, "Erro: Corpo da requisição de validação vazio.", 400);
        }

        if (estaEmBranco(dados.conteudo())) return new ValidateResponse(false, "Erro: 'conteudo' do documento ausente para validação.", 422);
        if (estaEmBranco(dados.assinatura())) return new ValidateResponse(false, "Erro: 'assinatura' ausente para validação.", 422);
        if (estaEmBranco(dados.politicaAssinaturaUrl())) return new ValidateResponse(false, "Erro: 'politicaAssinaturaUrl' ausente para validação.", 422);

        // Se a assinatura for a esperada, devolve 200 
        if ("MOCKED_SIGNATURE_BASE64_==".equals(dados.assinatura())) {
            return new ValidateResponse(true, "Assinatura é válida e o documento está íntegro.", 200);
        } else {
            // Se a assinatura não bater, consideramos 401 Unauthorized
            return new ValidateResponse(false, "Assinatura inválida: O conteúdo ou o material criptográfico divergem do original.", 401);
        }
    }

    private boolean estaEmBranco(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}