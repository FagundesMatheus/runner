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
    public ResponseEntity<AssinadorResponse> sign(@RequestBody AssinadorRequest request) {
        AssinadorResponse response = signService.sign(request);
        
        return ResponseEntity.ok(response);
    }

   @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@RequestBody ValidateRequest request) {
        
        ValidateResponse response = validateService.validate(request);
        
        return ResponseEntity.ok(response);
    }
}