package com.example.assinador.assinador;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.assinador.API.AssinadorRequest;
import com.example.assinador.API.AssinadorResponse;

@Service("SignService")
public class SignService implements ISignService {

    private static final String FAKE_SIGNATURE = "MOCKED_SIGNATURE_BASE64_==";

    @Autowired
    private SmartcardApduSimulator smartcardSimulator;

    @Override
    public AssinadorResponse assinar(AssinadorRequest dados) {
        if (dados == null) {
            return new AssinadorResponse(null, false, "Erro: Corpo da requisição vazio.", 400);
        }

        // Faltam campos obrigatórios = 422 Unprocessable Entity
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
            }
            case "PKCS#12" -> {
                if (estaEmBranco(cripto.conteudo())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'conteudo' em base64 é obrigatório.", 422);
                if (estaEmBranco(cripto.senha())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'senha' é obrigatória.", 422);
                if (estaEmBranco(cripto.alias())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'alias' é obrigatório.", 422);
                mensagemSucesso += " (Acesso via PKCS#12).";
            }
            case "TOKEN" -> {
                if (estaEmBranco(cripto.pin())) return new AssinadorResponse(null, false, "Erro (TOKEN): 'pin' é obrigatório.", 422);
                if (estaEmBranco(cripto.identificador())) return new AssinadorResponse(null, false, "Erro (TOKEN): 'identificador' é obrigatório.", 422);
                mensagemSucesso += " (Operação PKCS#11 validada via Token Local).";
            }
            case "SMARTCARD" -> {
                if (estaEmBranco(cripto.pin())) return new AssinadorResponse(null, false, "Erro (SMARTCARD): 'pin' é obrigatório.", 422);
                if (estaEmBranco(cripto.identificador())) return new AssinadorResponse(null, false, "Erro (SMARTCARD): 'identificador' é obrigatório.", 422);
                
                System.out.println("\nIniciando conexão com a Leitora de Smartcard");
                
                String respSelect = smartcardSimulator.enviarComando("00A4040000");
                if (!respSelect.endsWith("9000")) return new AssinadorResponse(null, false, "Erro APDU: Falha ao selecionar o chip do Smartcard.", 500);

                String comandoPin = "0020000004" + converterParaHex(cripto.pin());
                String respPin = smartcardSimulator.enviarComando(comandoPin);
                
                if (respPin.equals("6983")) {
                    return new AssinadorResponse(null, false, "Erro APDU (6983): Cartão bloqueado. Limite de tentativas de PIN excedido.", 401);
                } else if (!respPin.endsWith("9000")) {
                    return new AssinadorResponse(null, false, "Erro APDU: PIN incorreto. O Smartcard recusou a operação.", 401);
                }

                String respSign = smartcardSimulator.enviarComando("002A9E9A00");
                
                if (respSign.equals("6983")) {
                    return new AssinadorResponse(null, false, "Erro APDU (6983): Cartão bloqueado. Não é possível assinar.", 401);
                } else if (respSign.equals("6982")) {
                    return new AssinadorResponse(null, false, "Erro APDU (6982): Status de segurança não satisfeito. Faça a verificação do PIN primeiro.", 401);
                } else if (!respSign.endsWith("9000")) {
                    return new AssinadorResponse(null, false, "Erro APDU: Falha ao gerar a assinatura no hardware.", 500);
                }
                
                System.out.println("Assinatura via Smartcard Concluída\n");
                mensagemSucesso += " (Operação validada via comunicação APDU).";
            }
            case "REMOTE" -> {
                if (estaEmBranco(cripto.enderecoServico())) return new AssinadorResponse(null, false, "Erro (REMOTE): 'enderecoServico' é obrigatório.", 422);
                if (estaEmBranco(cripto.credencial())) return new AssinadorResponse(null, false, "Erro (REMOTE): 'credencial' é obrigatória.", 422);
                mensagemSucesso += " (Operação via serviço remoto).";
            }
            default -> {
                // Tipo inexistente = 400 Bad Request
                return new AssinadorResponse(null, false, "Erro: Tipo de criptografia '" + dados.tipoCriptografia() + "' não suportado.", 400);
            }
        }

        // Sucesso = 200 OK
        return new AssinadorResponse(FAKE_SIGNATURE, true, mensagemSucesso, 200);
    }

    private boolean estaEmBranco(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String converterParaHex(String texto) {
        StringBuilder hex = new StringBuilder();
        for (char ch : texto.toCharArray()) {
            hex.append(Integer.toHexString(ch));
        }
        return hex.toString().toUpperCase();
    }
}