package com.example.assinador.assinador;

import org.springframework.stereotype.Service;

@Service("SignService")
public class SignService implements ISignService {

    @Override
    public String sign(
            String bundleEndereco,
            String provenanceTargetEndereco,
            String pkcs11Endereco,
            String cadeiaCertificadosEndereco,
            String fonteTemporal,
            String politicaAssinaturaUrl
    ) {
        return "SignService";
    }
}


/*
 1. String originaria do Bundle (endereço do jason Bundle);
 2. String originaria do Provenance.target  (endereço do Provenance.target);
 3. String originaria do PKCS# 11 (endereço do PKCS# 11, podendo ser um token ou um smartcard);
 4. String originaria da Cadeia de Certificados (endereço da Cadeia de Certificados);
 5. - String que ira receber o valor inteiro Unix UTC (Timestamp) do momento da assinatura (data e hora da assinatura);
 6. String que determina a fonte temporal da assinatura;
 7. String da identificação da política de assinaturra (Uma URL); 
 */
