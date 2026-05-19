package com.example.assinador.assinador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.assinador.API.ValidateRequest;
import com.example.assinador.API.ValidateResponse;

class ValidateServiceTest {

    private final ValidateService validateService = new ValidateService();

    @Test
    void deveRetornar400QuandoRequisicaoNula() {
        ValidateResponse res = validateService.validar(null);
        assertEquals(400, res.statusCode());
        assertFalse(res.valid());
    }

    @Test
    void deveRetornar422QuandoConteudoVazio() {
        ValidateRequest req = new ValidateRequest("", "assinatura", "http://pol");
        assertEquals(422, validateService.validar(req).statusCode());
    }

    @Test
    void deveRetornar422QuandoAssinaturaVazia() {
        ValidateRequest req = new ValidateRequest("conteudo", "", "http://pol");
        assertEquals(422, validateService.validar(req).statusCode());
    }

    @Test
    void deveRetornar422QuandoPoliticaVazia() {
        ValidateRequest req = new ValidateRequest("conteudo", "assinatura", "");
        assertEquals(422, validateService.validar(req).statusCode());
    }

    @Test
    void deveValidarComSucesso_200() {
        ValidateRequest req = new ValidateRequest("conteudo", "MOCKED_SIGNATURE_BASE64_==", "http://pol");
        ValidateResponse res = validateService.validar(req);
        
        assertEquals(200, res.statusCode());
        assertTrue(res.valid());
    }

    @Test
    void deveFalharQuandoAssinaturaForAdulterada_401() {
        ValidateRequest req = new ValidateRequest("conteudo", "ASSINATURA_ERRADA", "http://pol");
        ValidateResponse res = validateService.validar(req);
        
        assertEquals(401, res.statusCode());
        assertFalse(res.valid());
        assertTrue(res.message().contains("divergem"));
    }
}