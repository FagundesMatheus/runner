package com.example.assinador.API;

import com.example.assinador.assinador.ISignService;
import com.example.assinador.assinador.IValidateService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<AssinadorResponse<String>> sign(@RequestBody AssinadorRequest request) {
        AssinadorResponse<String> response = new AssinadorResponse<>(
                signService.sign(
                        request.bundleEndereco(),
                        request.provenanceTargetEndereco(),
                        request.pkcs11Endereco(),
                        request.cadeiaCertificadosEndereco(),
                        request.fonteTemporal(),
                        request.politicaAssinaturaUrl()
                )
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse<String>> validate(@RequestBody ValidateRequest request) {
        ValidateResponse<String> response = new ValidateResponse<>(
                validateService.validate(
                        request.assinaturaPath(),
                        request.politicaAssinatura()
                )
        );
        return ResponseEntity.ok(response);
    }
}