package com.example.assinador.assinador;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.example.assinador.API.ValidateRequest;
import com.example.assinador.API.ValidateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("validate")
public class ValidateService implements IValidateService {

    private static final String FAKE_SIGNATURE = "MOCKED_SIGNATURE_BASE64_==";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ValidateResponse validar(ValidateRequest request) {
        if (request == null || estaEmBranco(request.caminhoArquivoJson())) {
            return new ValidateResponse(false, "Erro: Caminho do arquivo JSON de validação não fornecido.");
        }

        File arquivo = new File(request.caminhoArquivoJson());
        if (!arquivo.exists() || !arquivo.isFile()) {
            return new ValidateResponse(false, "Erro: Arquivo JSON não encontrado no caminho: " + request.caminhoArquivoJson());
        }

        ValidateRequest.DadosValidacao dados;
        try {
            dados = objectMapper.readValue(arquivo, ValidateRequest.DadosValidacao.class);
        } catch (IOException e) {
            return new ValidateResponse(false, "Erro ao processar o JSON de validação: " + e.getMessage());
        }

        // Validações dos parâmetros do arquivo
        if (estaEmBranco(dados.conteudo())) {
            return new ValidateResponse(false, "Erro: Parâmetro 'conteudo' inválido ou ausente no JSON.");
        }
        if (estaEmBranco(dados.assinatura())) {
            return new ValidateResponse(false, "Erro: Parâmetro 'assinatura' inválido ou ausente no JSON.");
        }
        if (estaEmBranco(dados.politicaAssinaturaUrl())) {
            return new ValidateResponse(false, "Erro: Parâmetro 'politicaAssinaturaUrl' ausente. Política é necessária para validação.");
        }

        // Lógica de simulação da validação
        boolean isValid = FAKE_SIGNATURE.equals(dados.assinatura());
        String message = isValid 
                ? "Assinatura é válida e o documento está íntegro." 
                : "Assinatura é inválida ou não corresponde ao conteúdo fornecido.";

        return new ValidateResponse(isValid, message);
    }

    private boolean estaEmBranco(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}