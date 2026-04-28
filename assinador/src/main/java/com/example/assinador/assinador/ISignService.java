package com.example.assinador.assinador;

public interface ISignService {

    String sign(
            String bundleEndereco,
            String provenanceTargetEndereco,
            String pkcs11Endereco,
            String cadeiaCertificadosEndereco,
            String fonteTemporal,
            String politicaAssinaturaUrl
    );
}