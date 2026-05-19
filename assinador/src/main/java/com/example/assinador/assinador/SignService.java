package com.example.assinador.assinador;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.assinador.API.AssinadorRequest;
import com.example.assinador.API.AssinadorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("SignService")
public class SignService implements ISignService {

    private static final String FAKE_SIGNATURE = "MOCKED_SIGNATURE_BASE64_==";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Simulador do Smartcard
    @Autowired
    private SmartcardApduSimulator smartcardSimulator;

    @Override
    public AssinadorResponse assinar(AssinadorRequest request) {
        if (request == null || estaEmBranco(request.caminhoArquivoJson())) {
            return new AssinadorResponse(null, false, "Erro: Caminho do arquivo JSON não fornecido na requisição.");
        }

        File arquivo = new File(request.caminhoArquivoJson());
        if (!arquivo.exists() || !arquivo.isFile()) {
            return new AssinadorResponse(null, false, "Erro: Arquivo JSON não encontrado no caminho: " + request.caminhoArquivoJson());
        }

        AssinadorRequest.DadosAssinatura dados;
        try {
            dados = objectMapper.readValue(arquivo, AssinadorRequest.DadosAssinatura.class);
        } catch (IOException e) {
            return new AssinadorResponse(null, false, "Erro ao processar o JSON: " + e.getMessage());
        }

        // Validações Base do conteudo do JSON
        if (estaEmBranco(dados.bundleEndereco())) return new AssinadorResponse(null, false, "Erro: 'bundleEndereco' ausente no JSON.");
        if (estaEmBranco(dados.provenanceTargetEndereco())) return new AssinadorResponse(null, false, "Erro: 'provenanceTargetEndereco' ausente.");
        if (dados.dadosCriptograficos() == null) return new AssinadorResponse(null, false, "Erro: Objeto 'dadosCriptograficos' não fornecido.");
        if (estaEmBranco(dados.cadeiaCertificadosEndereco())) return new AssinadorResponse(null, false, "Erro: 'cadeiaCertificadosEndereco' ausente.");
        if (estaEmBranco(dados.politicaAssinaturaUrl())) return new AssinadorResponse(null, false, "Erro: 'politicaAssinaturaUrl' não definida.");
        if (estaEmBranco(dados.tipoCriptografia())) return new AssinadorResponse(null, false, "Erro: 'tipoCriptografia' é obrigatória.");

        String mensagemSucesso = "Assinatura criada com sucesso. Operação: " + dados.tipoCriptografia();
        AssinadorRequest.DadosCriptograficos cripto = dados.dadosCriptograficos();

        // Validações do tipo de criptografia escolhido
        switch (dados.tipoCriptografia().toUpperCase()) {
            case "PEM" -> {
                if (estaEmBranco(cripto.chavePrivada())) return new AssinadorResponse(null, false, "Erro (PEM): 'chavePrivada' é obrigatória.");
                mensagemSucesso += " (Acesso via PEM).";
            }

            case "PKCS#12" -> {
                if (estaEmBranco(cripto.conteudo())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'conteudo' em base64 é obrigatório.");
                if (estaEmBranco(cripto.senha())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'senha' é obrigatória.");
                if (estaEmBranco(cripto.alias())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'alias' é obrigatório.");
                mensagemSucesso += " (Acesso via PKCS#12).";
            }

            case "TOKEN" -> {
                if (estaEmBranco(cripto.pin())) return new AssinadorResponse(null, false, "Erro (TOKEN): 'pin' é obrigatório.");
                if (estaEmBranco(cripto.identificador())) return new AssinadorResponse(null, false, "Erro (TOKEN): 'identificador' é obrigatório.");
                if (cripto.tokenLabel() != null && cripto.tokenLabel().length() > 32) return new AssinadorResponse(null, false, "Erro (TOKEN): 'tokenLabel' excede 32 caracteres.");
                
                mensagemSucesso += " (Operação PKCS#11 validada via Token Local).";
            }

            case "SMARTCARD" -> {
                if (estaEmBranco(cripto.pin())) return new AssinadorResponse(null, false, "Erro (SMARTCARD): 'pin' é obrigatório.");
                if (estaEmBranco(cripto.identificador())) return new AssinadorResponse(null, false, "Erro (SMARTCARD): 'identificador' é obrigatório.");
                
                System.out.println("\nIniciando conexão com a Leitora de Smartcard");
                
                String respSelect = smartcardSimulator.enviarComando("00A4040000");
                if (!respSelect.endsWith("9000")) return new AssinadorResponse(null, false, "Erro APDU: Falha ao selecionar o chip do Smartcard.");

                String comandoPin = "0020000004" + converterParaHex(cripto.pin());
                String respPin = smartcardSimulator.enviarComando(comandoPin);
                if (!respPin.endsWith("9000")) return new AssinadorResponse(null, false, "Erro APDU: PIN incorreto. O Smartcard recusou a operação.");

                String respSign = smartcardSimulator.enviarComando("002A9E9A00");
                if (!respSign.endsWith("9000")) return new AssinadorResponse(null, false, "Erro APDU: Falha ao gerar a assinatura no hardware.");
                
                System.out.println("Assinatura via Smartcard Concluída\n");
                mensagemSucesso += " (Operação validada via comunicação APDU).";
            }

            case "REMOTE" -> {
                if (estaEmBranco(cripto.enderecoServico())) return new AssinadorResponse(null, false, "Erro (REMOTE): 'enderecoServico' é obrigatório.");
                if (estaEmBranco(cripto.credencial())) return new AssinadorResponse(null, false, "Erro (REMOTE): 'credencial' é obrigatória.");
                mensagemSucesso += " (Operação via serviço remoto).";
            }

            default -> {
                return new AssinadorResponse(null, false, "Erro: Tipo de criptografia '" + dados.tipoCriptografia() + "' não suportado.");
            }
        }

        return new AssinadorResponse(FAKE_SIGNATURE, true, mensagemSucesso);
    }

    private boolean estaEmBranco(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    // Método que transforma a senha do JSON em Hexadecimal para enviar ao "cartão"
    private String converterParaHex(String texto) {
        StringBuilder hex = new StringBuilder();
        for (char ch : texto.toCharArray()) {
            hex.append(Integer.toHexString(ch));
        }
        return hex.toString().toUpperCase();
    }
}