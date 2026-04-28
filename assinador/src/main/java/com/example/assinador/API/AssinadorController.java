package com.example.assinador.API;

import com.example.assinador.assinador.ISignService;
import com.example.assinador.assinador.IValidateService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AssinadorController {

    private final ISignService signService;
    private final IValidateService validateService;

    public AssinadorController(
            @Qualifier("SignService") ISignService signService,
            @Qualifier("validate") IValidateService validateService
    ) {
        this.signService = signService;
        this.validateService = validateService;
    }

    @PostMapping("/sign")
    public ResponseEntity<AssinadorResponse> sign(@RequestBody AssinadorRequest request) {
        AssinadorResponse response = new AssinadorResponse(
                "/api/sign",
                "SignService",
                signService.sign(
                        request.bundleEndereco(),
                        request.provenanceTargetEndereco(),
                        request.pkcs11Endereco(),
                        request.cadeiaCertificadosEndereco(),
                        request.timestampUnixUtc(),
                        request.fonteTemporal(),
                        request.politicaAssinaturaUrl()
                ),
                buildDadosRecebidos(request)
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<AssinadorResponse> validate(@RequestBody AssinadorRequest request) {
        AssinadorResponse response = new AssinadorResponse(
                "/api/validate",
                "validate",
                validateService.validate(),
                buildDadosRecebidos(request)
        );
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildDadosRecebidos(AssinadorRequest request) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("bundleEndereco", request.bundleEndereco());
        dados.put("provenanceTargetEndereco", request.provenanceTargetEndereco());
        dados.put("pkcs11Endereco", request.pkcs11Endereco());
        dados.put("cadeiaCertificadosEndereco", request.cadeiaCertificadosEndereco());
        dados.put("timestampUnixUtc", request.timestampUnixUtc());
        dados.put("fonteTemporal", request.fonteTemporal());
        dados.put("politicaAssinaturaUrl", request.politicaAssinaturaUrl());
        return dados;
    }
}