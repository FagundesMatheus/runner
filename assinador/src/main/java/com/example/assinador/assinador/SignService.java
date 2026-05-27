package com.example.assinador.assinador;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;

import org.springframework.stereotype.Service;

import com.example.assinador.API.AssinadorRequest;
import com.example.assinador.API.AssinadorResponse;

@Service("SignService")
public class SignService implements ISignService {

    private static final String FAKE_SIGNATURE = "MOCKED_SIGNATURE_BASE64_==";

    @Override
    public AssinadorResponse assinar(AssinadorRequest dados) {
        if (dados == null) {
            return new AssinadorResponse(null, false, "Erro: Corpo da requisição vazio.", 400);
        }
        if (estaEmBranco(dados.bundleEndereco())) return new AssinadorResponse(null, false, "Erro: 'bundleEndereco' ausente no JSON.", 422);
        if (estaEmBranco(dados.provenanceTargetEndereco())) return new AssinadorResponse(null, false, "Erro: 'provenanceTargetEndereco' ausente.", 422);
        if (dados.dadosCriptograficos() == null) return new AssinadorResponse(null, false, "Erro: Objeto 'dadosCriptograficos' não fornecido.", 422);
        if (estaEmBranco(dados.cadeiaCertificadosEndereco())) return new AssinadorResponse(null, false, "Erro: 'cadeiaCertificadosEndereco' ausente.", 422);
        if (estaEmBranco(dados.politicaAssinaturaUrl())) return new AssinadorResponse(null, false, "Erro: 'politicaAssinaturaUrl' não definida.", 422);
        if (estaEmBranco(dados.tipoCriptografia())) return new AssinadorResponse(null, false, "Erro: 'tipoCriptografia' é obrigatória.", 422);

        String mensagemSucesso = "Assinatura criada com sucesso. Operação: " + dados.tipoCriptografia();
        AssinadorRequest.DadosCriptograficos cripto = dados.dadosCriptograficos();

        switch (dados.tipoCriptografia().toUpperCase()) {
            case "PEM" -> {
                if (estaEmBranco(cripto.chavePrivada())) return new AssinadorResponse(null, false, "Erro (PEM): 'chavePrivada' é obrigatória.", 422);
                mensagemSucesso += " (Acesso via PEM).";
                return new AssinadorResponse(FAKE_SIGNATURE, true, mensagemSucesso, 200);
            }
            case "PKCS#12" -> {
                if (estaEmBranco(cripto.conteudo())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'conteudo' em base64 é obrigatório.", 422);
                if (estaEmBranco(cripto.senha())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'senha' é obrigatória.", 422);
                if (estaEmBranco(cripto.alias())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'alias' é obrigatório.", 422);
                mensagemSucesso += " (Acesso via PKCS#12).";
                return new AssinadorResponse(FAKE_SIGNATURE, true, mensagemSucesso, 200);
            }
            case "TOKEN", "SMARTCARD" -> {
                if (estaEmBranco(cripto.pin())) return new AssinadorResponse(null, false, "Erro: 'pin' é obrigatório para esta operação.", 422);
                if (estaEmBranco(cripto.identificador())) return new AssinadorResponse(null, false, "Erro: 'identificador' é obrigatório.", 422);
                
                System.out.println("\n[PKCS#11] Iniciando conexão com o dispositivo criptográfico via SunPKCS11...");
                
                try {
                    Provider provedorSunPKCS11 = Security.getProvider("SunPKCS11-SoftHSM2"); 
                    
                    if (provedorSunPKCS11 == null) {
                        Provider provedorBase = Security.getProvider("SunPKCS11");
                        if (provedorBase == null) {
                            return new AssinadorResponse(null, false, "Erro: Provedor SunPKCS11 não suportado.", 500);
                        }
                        provedorSunPKCS11 = provedorBase.configure("softhsm2.cfg");
                        Security.addProvider(provedorSunPKCS11);
                    }

                    KeyStore keyStore = KeyStore.getInstance("PKCS11", provedorSunPKCS11);

                    char[] pinPassword = cripto.pin().toCharArray();
                    keyStore.load(null, pinPassword);

                    System.out.println("[PKCS#11] Login efetuado com sucesso no dispositivo!");
                    System.out.println("[PKCS#11] Autenticação validada via hardware. Retornando assinatura simulada.\n");
                    
                    mensagemSucesso += " (Autenticação real via PKCS#11).";
                    return new AssinadorResponse(FAKE_SIGNATURE, true, mensagemSucesso, 200);

                } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
                    String mensagemErro = e.getMessage() != null ? e.getMessage() : "";
                    System.err.println("[PKCS#11] Falha na operação de hardware: " + mensagemErro);

                    if (mensagemErro.contains("CKR_PIN_INCORRECT")) {
                        return new AssinadorResponse(null, false, "Erro APDU / PKCS#11: PIN incorreto. O dispositivo recusou a operação.", 401);
                    } else if (mensagemErro.contains("CKR_PIN_LOCKED")) {
                        return new AssinadorResponse(null, false, "Erro APDU / PKCS#11 (6983): Dispositivo bloqueado. Limite de tentativas físicas excedido.", 401);
                    } else if (mensagemErro.contains("CKR_DEVICE_REMOVED") || mensagemErro.contains("init failed")) {
                        return new AssinadorResponse(null, false, "Erro de infraestrutura: Falha ao carregar a DLL do SoftHSM2 ou dispositivo ausente.", 500);
                    }

                    return new AssinadorResponse(null, false, "Erro interno de criptografia: " + mensagemErro, 500);
                }
            }
            case "REMOTE" -> {
                if (estaEmBranco(cripto.enderecoServico())) return new AssinadorResponse(null, false, "Erro (REMOTE): 'enderecoServico' é obrigatório.", 422);
                if (estaEmBranco(cripto.credencial())) return new AssinadorResponse(null, false, "Erro (REMOTE): 'credencial' é obrigatória.", 422);
                mensagemSucesso += " (Operação via serviço remoto).";
                return new AssinadorResponse(FAKE_SIGNATURE, true, mensagemSucesso, 200);
            }
            default -> {
                // Tipo inexistente = 400 Bad Request
                return new AssinadorResponse(null, false, "Erro: Tipo de criptografia '" + dados.tipoCriptografia() + "' não suportado.", 400);
            }
        }
    }

    private boolean estaEmBranco(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}