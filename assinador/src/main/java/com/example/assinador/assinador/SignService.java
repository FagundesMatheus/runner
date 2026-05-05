package com.example.assinador.assinador;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Service;

import com.example.assinador.API.AssinadorRequest;
import com.example.assinador.API.AssinadorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("SignService")
public class SignService implements ISignService {

    private static final String FAKE_SIGNATURE = "MOCKED_SIGNATURE_BASE64_==";
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        // Validações Base
        if (estaEmBranco(dados.bundleEndereco())) return new AssinadorResponse(null, false, "Erro: 'bundleEndereco' ausente no JSON.");
        if (estaEmBranco(dados.provenanceTargetEndereco())) return new AssinadorResponse(null, false, "Erro: 'provenanceTargetEndereco' ausente.");
        if (dados.dadosCriptograficos() == null) return new AssinadorResponse(null, false, "Erro: Objeto 'dadosCriptograficos' não fornecido.");
        if (estaEmBranco(dados.cadeiaCertificadosEndereco())) return new AssinadorResponse(null, false, "Erro: 'cadeiaCertificadosEndereco' ausente.");
        if (estaEmBranco(dados.politicaAssinaturaUrl())) return new AssinadorResponse(null, false, "Erro: 'politicaAssinaturaUrl' não definida.");
        if (estaEmBranco(dados.tipoCriptografia())) return new AssinadorResponse(null, false, "Erro: 'tipoCriptografia' é obrigatória.");

        String mensagemSucesso = "Assinatura criada com sucesso. Operação: " + dados.tipoCriptografia();
        AssinadorRequest.DadosCriptograficos cripto = dados.dadosCriptograficos();

        // Validações Específicas por Tipo de Criptografia
        switch (dados.tipoCriptografia().toUpperCase()) {
            case "PEM":
                if (estaEmBranco(cripto.chavePrivada())) return new AssinadorResponse(null, false, "Erro (PEM): 'chavePrivada' é obrigatória.");
                mensagemSucesso += " (Acesso via PEM).";
                break;

            case "PKCS#12":
                if (estaEmBranco(cripto.conteudo())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'conteudo' em base64 é obrigatório.");
                if (estaEmBranco(cripto.senha())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'senha' é obrigatória.");
                if (estaEmBranco(cripto.alias())) return new AssinadorResponse(null, false, "Erro (PKCS#12): 'alias' é obrigatório.");
                mensagemSucesso += " (Acesso via PKCS#12).";
                break;

            case "SMARTCARD":
            case "TOKEN":
                if (estaEmBranco(cripto.pin())) return new AssinadorResponse(null, false, String.format("Erro (%s): 'pin' é obrigatório.", dados.tipoCriptografia()));
                if (estaEmBranco(cripto.identificador())) return new AssinadorResponse(null, false, String.format("Erro (%s): 'identificador' é obrigatório.", dados.tipoCriptografia()));
                if (cripto.tokenLabel() != null && cripto.tokenLabel().length() > 32) return new AssinadorResponse(null, false, String.format("Erro (%s): 'tokenLabel' excede 32 caracteres.", dados.tipoCriptografia()));
                mensagemSucesso += " (Operação PKCS#11).";
                break;

            case "REMOTE":
                if (estaEmBranco(cripto.enderecoServico())) return new AssinadorResponse(null, false, "Erro (REMOTE): 'enderecoServico' é obrigatório.");
                if (estaEmBranco(cripto.credencial())) return new AssinadorResponse(null, false, "Erro (REMOTE): 'credencial' é obrigatória.");
                mensagemSucesso += " (Operação via serviço remoto).";
                break;

            default:
                return new AssinadorResponse(null, false, "Erro: Tipo de criptografia '" + dados.tipoCriptografia() + "' não suportado.");
        }

        return new AssinadorResponse(FAKE_SIGNATURE, true, mensagemSucesso);
    }

    private boolean estaEmBranco(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}