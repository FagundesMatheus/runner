package com.example.assinador.API;

public record AssinadorRequest(
        String bundleEndereco,
        String provenanceTargetEndereco,
        String pkcs11Endereco,
        String cadeiaCertificadosEndereco,
        String fonteTemporal,
        String politicaAssinaturaUrl
) {
}
