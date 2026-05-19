package com.example.assinador.API;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.assinador.assinador.ISignService;
import com.example.assinador.assinador.IValidateService;

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
    public ResponseEntity<AssinadorResponse> assinar(@RequestBody AssinadorRequest request) {
        AssinadorResponse response = signService.assinar(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validar(@RequestBody ValidateRequest request) {
        ValidateResponse response = validateService.validar(request);
        return ResponseEntity.status(response.statusCode()).body(response);
    }
}