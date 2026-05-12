package com.example.assinador.API;

public record AssinadorRequest(
        String caminhoArquivoJson
) {
    // Representa o conteúdo principal do arquivo JSON
    public record DadosAssinatura(
            String bundleEndereco,
            String provenanceTargetEndereco,
            DadosCriptograficos dadosCriptograficos,
            String cadeiaCertificadosEndereco,
            String fonteTemporal,
            String politicaAssinaturaUrl,
            String tipoCriptografia
    ) {}

    // Representa o objeto "dadosCriptograficos" de dentro do JSON
    public record DadosCriptograficos(
            // PEM / PKCS#12
            String chavePrivada,
            String conteudo,
            String senha,
            String alias,

            // SMARTCARD / TOKEN (PKCS#11)
            String pin,
            String identificador,
            Integer slotId,
            String tokenLabel,

            // REMOTE
            String enderecoServico,
            String credencial
    ) {}
}