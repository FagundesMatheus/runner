package com.example.assinador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import com.example.assinador.API.AssinadorRequest;
import com.example.assinador.API.ValidateRequest;
import com.example.assinador.assinador.SignService;
import com.example.assinador.assinador.ValidateService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class AssinadorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssinadorApplication.class, args);
    }

    @Bean
	@Profile("!test")
    public CommandLineRunner motorCli(SignService signService, ValidateService validateService, ObjectMapper objectMapper) {
        return args -> {
            List<String> argumentos = Arrays.asList(args);

            // 1. MODO API: Se passar -API ou não passar nada
            if (argumentos.contains("-API") || argumentos.isEmpty()) {
                System.out.println("Servidor rodando na porta 9742");
                return; // Encerra o interpretador e deixa o Spring Boot subir o servidor
            }

            // 2. MODO LOCAL: Se passar a flag -local
            if (argumentos.contains("-local")) {
                try {
                    // Busca qual argumento não começa com "-" (Esse será o caminho do arquivo JSON)
                    String caminhoArquivo = argumentos.stream()
                            .filter(arg -> !arg.startsWith("-"))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Caminho do arquivo JSON não fornecido."));

                    // Lê o arquivo do disco
                    String jsonEntrada = Files.readString(Path.of(caminhoArquivo));
                    String jsonSaida = "";

                    // 2.1 ROTA DE ASSINATURA
                    if (argumentos.contains("-assinar")) {
                        AssinadorRequest request = objectMapper.readValue(jsonEntrada, AssinadorRequest.class);
                        var resposta = signService.assinar(request);
                        jsonSaida = objectMapper.writeValueAsString(resposta);
                    } 
                    // 2.2 ROTA DE VALIDAÇÃO
                    else if (argumentos.contains("-validar")) {
                        ValidateRequest request = objectMapper.readValue(jsonEntrada, ValidateRequest.class);
                        var resposta = validateService.validar(request); // Ajuste o nome do método se necessário
                        jsonSaida = objectMapper.writeValueAsString(resposta);
                    } 
                    else {
                        throw new IllegalArgumentException("Operação não definida. Use -assinar ou -validar.");
                    }

                    // Imprime o resultado limpo para o Python ler
                    System.out.println(jsonSaida);

                } catch (IOException | IllegalArgumentException e) {
                    System.out.println("{\"valid\":false, \"message\":\"Erro fatal no Java: " + e.getMessage() + "\"}");
                }

                // Mata o processo Java imediatamente após imprimir a resposta, impedindo o servidor web de ligar
                System.exit(0);
            }
        };
    }
}