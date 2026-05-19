package com.example.assinador.API;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.assinador.assinador.ISignService;
import com.example.assinador.assinador.IValidateService;

@WebMvcTest(AssinadorController.class)
class AssinadorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "SignService")
    private ISignService signService;

    @MockitoBean(name = "validate")
    private IValidateService validateService;

    // Testes da Rota de Assinatura (/api/sign)

    @Test
    void deveResponder200AoAssinarComSucesso() throws Exception {
        when(signService.assinar(any())).thenReturn(new AssinadorResponse("ASSINATURA", true, "OK", 200));

        mockMvc.perform(post("/api/sign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"tipoCriptografia\": \"PEM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void deveResponder422AoFaltarDadosNaAssinatura() throws Exception {
        when(signService.assinar(any())).thenReturn(new AssinadorResponse(null, false, "Falta PIN", 422));

        mockMvc.perform(post("/api/sign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"tipoCriptografia\": \"SMARTCARD\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void deveResponder401QuandoHardwareRecusarPin() throws Exception {
        when(signService.assinar(any())).thenReturn(new AssinadorResponse(null, false, "PIN Errado", 401));

        mockMvc.perform(post("/api/sign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"tipoCriptografia\": \"SMARTCARD\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // Testes da Rota de Validação (/api/validate)

    @Test
    void deveResponder200AoValidarComSucesso() throws Exception {
        when(validateService.validar(any())).thenReturn(new ValidateResponse(true, "Integro", 200));

        mockMvc.perform(post("/api/validate")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"assinatura\": \"OK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void deveResponder401AoValidarAssinaturaFalsa() throws Exception {
        when(validateService.validar(any())).thenReturn(new ValidateResponse(false, "Adulterado", 401));

        mockMvc.perform(post("/api/validate")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"assinatura\": \"FALSA\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }
}