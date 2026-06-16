package com.example.assinador.API;

import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final ApplicationContext context;

    public AssinadorController(
            @Qualifier("SignService") ISignService signService,
            @Qualifier("validate") IValidateService validateService,
            ApplicationContext context
    ) {
        this.signService = signService;
        this.validateService = validateService;
        this.context = context;
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

    @GetMapping("/readiness")
    public ResponseEntity<Map<String, String>> readiness() {
        return ResponseEntity.ok(Map.of("status", "READY"));
    }

    @GetMapping("/liveness")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "ALIVE"));
    }

    @PostMapping("/shutdown")
    public ResponseEntity<Map<String, String>> shutdown() {
        new Thread(() -> {
            try {
                Thread.sleep(500); // Pequeno atraso para garantir o retorno da requisição HTTP ao cliente
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.exit(SpringApplication.exit(context, () -> 0));
        }).start();
        return ResponseEntity.ok(Map.of("message", "Encerrando a API com segurança..."));
    }
}