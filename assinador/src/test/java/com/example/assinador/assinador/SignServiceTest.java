package com.example.assinador.assinador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.assinador.API.AssinadorRequest;
import com.example.assinador.API.AssinadorResponse;

class SignServiceTest {

    private final SignService signService = new SignService();

    // Métodos Auxiliares
    private AssinadorRequest criarRequisicao(String tipo, AssinadorRequest.DadosCriptograficos cripto) {
        return new AssinadorRequest("conteudo", "autor", "cert", "http://time", "http://pol", tipo, cripto);
    }

    private AssinadorRequest.DadosCriptograficos criarCriptoCompleto(String pin) {
        return new AssinadorRequest.DadosCriptograficos(
                pin, "id-1", 1, "label", "chave", "conteudo", "senha", "alias", "http://api", "credencial"
        );
    }

    // Testes de Validações Base (HTTP 400 e 422)
    @Test
    void deveRetornar400QuandoRequisicaoNula() {
        assertEquals(400, signService.assinar(null).statusCode());
    }

    @Test
    void deveRetornar422QuandoBundleEnderecoVazio() {
        AssinadorRequest req = new AssinadorRequest("", "autor", "cert", "time", "pol", "PEM", criarCriptoCompleto("1234"));
        assertEquals(422, signService.assinar(req).statusCode());
    }

    @Test
    void deveRetornar422QuandoDadosCriptograficosNulos() {
        AssinadorRequest req = criarRequisicao("PEM", null);
        assertEquals(422, signService.assinar(req).statusCode());
    }

    // Testes - Tipo: PEM
    @Test
    void deveAssinarPemComSucesso_200() {
        AssinadorRequest req = criarRequisicao("PEM", criarCriptoCompleto("1234"));
        AssinadorResponse res = signService.assinar(req);
        assertEquals(200, res.statusCode());
        assertTrue(res.valid());
    }

    @Test
    void deveFalharPemSemChavePrivada_422() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos(null, null, null, null, "", null, null, null, null, null);
        assertEquals(422, signService.assinar(criarRequisicao("PEM", cripto)).statusCode());
    }

    // Testes - Tipo: PKCS#12
    @Test
    void deveAssinarPkcs12ComSucesso_200() {
        assertEquals(200, signService.assinar(criarRequisicao("PKCS#12", criarCriptoCompleto("1234"))).statusCode());
    }

    @Test
    void deveFalharPkcs12SemConteudo_422() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos(null, null, null, null, null, "", "senha", "alias", null, null);
        assertEquals(422, signService.assinar(criarRequisicao("PKCS#12", cripto)).statusCode());
    }

    @Test
    void deveFalharPkcs12SemSenha_422() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos(null, null, null, null, null, "conteudo", "", "alias", null, null);
        assertEquals(422, signService.assinar(criarRequisicao("PKCS#12", cripto)).statusCode());
    }

    // Testes - Integração Real com Hardware (Token e Smartcard)
    @Test
    void deveAssinarTokenComPinCorreto_200() {
        // Usa o PIN 1234 para validar na .dll do Windows
        assertEquals(200, signService.assinar(criarRequisicao("TOKEN", criarCriptoCompleto("1234"))).statusCode());
    }

    @Test
    void deveFalharTokenSemPin_422() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos("", "id", null, null, null, null, null, null, null, null);
        assertEquals(422, signService.assinar(criarRequisicao("TOKEN", cripto)).statusCode());
    }

    // Testes - Tipo: Remote
    @Test
    void deveAssinarRemoteComSucesso_200() {
        assertEquals(200, signService.assinar(criarRequisicao("REMOTE", criarCriptoCompleto("1234"))).statusCode());
    }

    @Test
    void deveFalharRemoteSemCredencial_422() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos(null, null, null, null, null, null, null, null, "http", "");
        assertEquals(422, signService.assinar(criarRequisicao("REMOTE", cripto)).statusCode());
    }

    // Tipo Inválido
    @Test
    void deveRetornar400QuandoTipoNaoSuportado() {
        assertEquals(400, signService.assinar(criarRequisicao("TIPO_FALSO", criarCriptoCompleto("1234"))).statusCode());
    }
}