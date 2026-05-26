package com.example.assinador.assinador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.doReturn;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.assinador.API.AssinadorRequest;
import com.example.assinador.API.AssinadorResponse;

@ExtendWith(MockitoExtension.class)
class SignServiceTest {

    @Spy
    private SmartcardApduSimulator smartcardSimulator;

    @InjectMocks
    private SignService signService;

    // Limpeza de Memória (MUITO IMPORTANTE)
    @BeforeEach
    public void prepararCartaoLimpo() {
        // Antes de CADA teste, o Spring Boot reseta a RAM do simulador.
        // Assim, um teste que erra a senha não quebra o próximo teste que acerta.
        smartcardSimulator.resetarCartaoVirtual();
    }

    // Métodos Auxiliares
    private AssinadorRequest criarRequisicao(String tipo, AssinadorRequest.DadosCriptograficos cripto) {
        return new AssinadorRequest("conteudo", "autor", "cert", "http://time", "http://pol", tipo, cripto);
    }

    private AssinadorRequest.DadosCriptograficos criarCriptoCompleto() {
        return new AssinadorRequest.DadosCriptograficos(
                "1234", "id-1", 1, "label", "chave", "conteudo", "senha", "alias", "http://api", "credencial"
        );
    }

    // Testes de Validações Base (HTTP 400 e 422)
    @Test
    void deveRetornar400QuandoRequisicaoNula() {
        assertEquals(400, signService.assinar(null).statusCode());
    }

    @Test
    void deveRetornar422QuandoBundleEnderecoVazio() {
        AssinadorRequest req = new AssinadorRequest("", "autor", "cert", "time", "pol", "PEM", criarCriptoCompleto());
        assertEquals(422, signService.assinar(req).statusCode());
    }

    @Test
    void deveRetornar422QuandoDadosCriptograficosNulos() {
        AssinadorRequest req = criarRequisicao("PEM", null);
        assertEquals(422, signService.assinar(req).statusCode());
    }

    // Testes PEM 
    @Test
    void deveAssinarPemComSucesso_200() {
        AssinadorRequest req = criarRequisicao("PEM", criarCriptoCompleto());
        AssinadorResponse res = signService.assinar(req);
        assertEquals(200, res.statusCode());
        assertTrue(res.valid());
    }

    @Test
    void deveFalharPemSemChavePrivada_422() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos(null, null, null, null, "", null, null, null, null, null);
        assertEquals(422, signService.assinar(criarRequisicao("PEM", cripto)).statusCode());
    }

    // Testes PKCS#12 
    @Test
    void deveAssinarPkcs12ComSucesso_200() {
        assertEquals(200, signService.assinar(criarRequisicao("PKCS#12", criarCriptoCompleto())).statusCode());
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

    // Testes TOKEN 
    @Test
    void deveAssinarTokenComSucesso_200() {
        assertEquals(200, signService.assinar(criarRequisicao("TOKEN", criarCriptoCompleto())).statusCode());
    }

    @Test
    void deveFalharTokenSemPin_422() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos("", "id", null, null, null, null, null, null, null, null);
        assertEquals(422, signService.assinar(criarRequisicao("TOKEN", cripto)).statusCode());
    }

    // Testes SMARTCARD 
    @Test
    void deveAssinarSmartcardComPinCorreto_200() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos("1234", "id", null, null, null, null, null, null, null, null);
        AssinadorResponse res = signService.assinar(criarRequisicao("SMARTCARD", cripto));
        assertEquals(200, res.statusCode());
        assertTrue(res.valid());
    }

    @Test
    void deveFalharSmartcardComPinIncorreto_401() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos("9999", "id", null, null, null, null, null, null, null, null);
        AssinadorResponse res = signService.assinar(criarRequisicao("SMARTCARD", cripto));
        assertEquals(401, res.statusCode());
        assertFalse(res.valid());
    }

    @Test
    void deveFalharSmartcardErroHardwareSelect_500() {
        doReturn("6900").when(smartcardSimulator).enviarComando("00A4040000");
        AssinadorRequest req = criarRequisicao("SMARTCARD", criarCriptoCompleto());
        assertEquals(500, signService.assinar(req).statusCode());
    }

    // Valida o bloqueio de hardware após 3 tentativas
    @Test
    void deveBloquearSmartcardAposTresTentativasErradas_401() {
        // Criamos uma requisição com o PIN errado ("9999")
        AssinadorRequest.DadosCriptograficos criptoErro = new AssinadorRequest.DadosCriptograficos("9999", "id", null, null, null, null, null, null, null, null);
        AssinadorRequest reqErro = criarRequisicao("SMARTCARD", criptoErro);

        // Tentativa 1 (Errou a senha, retorna 401)
        assertEquals(401, signService.assinar(reqErro).statusCode());
        
        // Tentativa 2 (Errou a senha, retorna 401)
        assertEquals(401, signService.assinar(reqErro).statusCode());
        
        // Tentativa 3 (Errou a senha, chip BLOQUEIA, retorna 401)
        assertEquals(401, signService.assinar(reqErro).statusCode());

        // Tentativa 4: Agora mandamos a requisição com o PIN CORRETO ("1234")
        AssinadorRequest reqSucesso = criarRequisicao("SMARTCARD", criarCriptoCompleto()); 
        AssinadorResponse resPosBloqueio = signService.assinar(reqSucesso);
        
        // Tem que falhar mesmo com a senha certa, porque o cartão virtual "queimou"
        assertEquals(401, resPosBloqueio.statusCode());
        assertFalse(resPosBloqueio.valid());
        // Verifica se a mensagem retornada acusa que foi um erro de bloqueio/autenticação (6983 ou 6982)
        assertTrue(resPosBloqueio.message().contains("6983") || resPosBloqueio.message().contains("Autenticação"));
    }

    // Testes REMOTE
    @Test
    void deveAssinarRemoteComSucesso_200() {
        assertEquals(200, signService.assinar(criarRequisicao("REMOTE", criarCriptoCompleto())).statusCode());
    }

    @Test
    void deveFalharRemoteSemCredencial_422() {
        AssinadorRequest.DadosCriptograficos cripto = new AssinadorRequest.DadosCriptograficos(null, null, null, null, null, null, null, null, "http", "");
        assertEquals(422, signService.assinar(criarRequisicao("REMOTE", cripto)).statusCode());
    }

    // Teste Tipo Inexistente
    @Test
    void deveRetornar400QuandoTipoNaoSuportado() {
        assertEquals(400, signService.assinar(criarRequisicao("TIPO_FALSO", criarCriptoCompleto())).statusCode());
    }
}