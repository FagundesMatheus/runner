package com.example.assinador.API;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.assinador.assinador.ISignService;
import com.example.assinador.assinador.IValidateService;

@WebMvcTest(AssinadorController.class)
@ActiveProfiles("test")
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
        when(signService.assinar(any())).thenReturn(new AssinadorResponse("ASSINATURA_BASE64", true, "OK", 200));

        String jsonPayload = """
                {
                  "bundleEndereco": "http://bundle",
                  "provenanceTargetEndereco": "http://target",
                  "cadeiaCertificadosEndereco": "http://cadeia",
                  "fonteTemporal": "http://time",
                  "politicaAssinaturaUrl": "http://politica",
                  "tipoCriptografia": "PEM",
                  "dadosCriptograficos": {
                    "chavePrivada": "CHAVE_MOCK"
                  }
                }
                """;

        mockMvc.perform(post("/api/sign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.signature").value("ASSINATURA_BASE64"));
    }

    @Test
    void deveResponder422AoFaltarDadosNaAssinatura() throws Exception {
        when(signService.assinar(any())).thenReturn(new AssinadorResponse(null, false, "Falta PIN ou Identificador", 422));

        String jsonPayload = """
                {
                  "tipoCriptografia": "SMARTCARD"
                }
                """;

        mockMvc.perform(post("/api/sign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonPayload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Falta PIN ou Identificador"));
    }

    @Test
    void deveResponder401QuandoHardwareRecusarPin() throws Exception {
        when(signService.assinar(any())).thenReturn(new AssinadorResponse(null, false, "PIN Errado", 401));

        String jsonPayload = """
                {
                  "tipoCriptografia": "SMARTCARD",
                  "dadosCriptograficos": {
                    "pin": "9999",
                    "identificador": "leitora-01"
                  }
                }
                """;

        mockMvc.perform(post("/api/sign")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // Testes da Rota de Validação (/api/validate)

    @Test
    void deveResponder200AoValidarComSucesso() throws Exception {
        when(validateService.validar(any())).thenReturn(new ValidateResponse(true, "Documento Íntegro", 200));

        String jsonPayload = """
                {
                  "conteudo": "DocBase64",
                  "assinatura": "AssinaturaBase64",
                  "politicaAssinaturaUrl": "http://politica"
                }
                """;

        mockMvc.perform(post("/api/validate")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void deveResponder401AoValidarAssinaturaFalsa() throws Exception {
        when(validateService.validar(any())).thenReturn(new ValidateResponse(false, "Assinatura Adulterada", 401));

        String jsonPayload = """
                {
                  "conteudo": "DocBase64",
                  "assinatura": "FALSA",
                  "politicaAssinaturaUrl": "http://politica"
                }
                """;

        mockMvc.perform(post("/api/validate")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }
}