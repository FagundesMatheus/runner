package com.example.assinador.API;

public record AssinadorRequest(
        String bundleEndereco,
        String provenanceTargetEndereco,
        String cadeiaCertificadosEndereco,
        String fonteTemporal,
        String politicaAssinaturaUrl,
        String tipoCriptografia,
        DadosCriptograficos dadosCriptograficos
) {
    public record DadosCriptograficos(
            String pin,
            String identificador,
            Integer slotId,
            String tokenLabel,
            String chavePrivada, // Para PEM
            String conteudo,     // Para PKCS#12
            String senha,        // Para PKCS#12
            String alias,        // Para PKCS#12
            String enderecoServico, // Para REMOTE
            String credencial       // Para REMOTE
    ) {}
}