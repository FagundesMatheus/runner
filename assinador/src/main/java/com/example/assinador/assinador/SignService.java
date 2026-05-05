package com.example.assinador.assinador;

import org.springframework.stereotype.Service;

import com.example.assinador.API.AssinadorRequest;
import com.example.assinador.API.AssinadorResponse;

@Service("SignService")
public class SignService implements ISignService {

    private static final String FAKE_SIGNATURE = "MOCKED_SIGNATURE_BASE64_==";

    @Override
    public AssinadorResponse sign(AssinadorRequest request) {
        if (request == null || isBlank(request.bundleEndereco())) {
            return new AssinadorResponse(null, false, "Erro: 'bundleEndereco' (arquivo) é obrigatório.");
        }

        if (isBlank(request.pkcs11Endereco())) {
            return new AssinadorResponse(null, false, "Erro: Driver do Token (pkcs11Endereco) não fornecido.");
        }

        if (isBlank(request.cadeiaCertificadosEndereco())) {
            return new AssinadorResponse(null, false, "Erro: Cadeia de certificados ausente.");
        }
        
        if (isBlank(request.politicaAssinaturaUrl())) {
            return new AssinadorResponse(null, false, "Erro: Política de assinatura não definida.");
        }

        if (isBlank(request.fonteTemporal())) {
            System.out.println("Aviso: Assinatura sendo gerada sem Carimbo de Tempo.");
        }

        return new AssinadorResponse(FAKE_SIGNATURE, true, "Assinatura criada com sucesso utilizando hardware e políticas fornecidas.");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}