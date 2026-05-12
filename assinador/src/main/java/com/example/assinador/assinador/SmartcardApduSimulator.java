package com.example.assinador.assinador;

import org.springframework.stereotype.Service;

@Service
public class SmartcardApduSimulator {

    // Códigos de status padrão de um Smartcard real (ISO 7816)
    private static final String SUCESSO = "9000";
    private static final String ERRO_PIN_INCORRETO = "6900";
    private static final String INSTRUCAO_NAO_SUPORTADA = "6D00";

    public String enviarComando(String comandoHex) {
        System.out.println("Terminal enviou: " + comandoHex);

        // Pega a "Instrução" (INS) que fica no 2º byte do comando
        String ins = comandoHex.substring(2, 4); 
        String resposta;

        switch (ins) {
            case "A4" -> { 
                // Comando SELECT (Selecionar o aplicativo no chip)
                resposta = SUCESSO;
            }
            case "20" -> { 
                // Comando VERIFY PIN (Verificar Senha)
                // Checa se a senha "1234" (31323334 em Hexadecimal) foi enviada
                if (comandoHex.contains("31323334")) {
                    resposta = SUCESSO; 
                } else {
                    resposta = ERRO_PIN_INCORRETO; 
                }
            }
            case "2A" -> { 
                // Comando COMPUTE SIGNATURE (Assinar documento)
                resposta = "MOCKED_SIGNATURE_BASE64_==" + SUCESSO;
            }
            default -> {
                resposta = INSTRUCAO_NAO_SUPORTADA;
            }
        }

        System.out.println("Cartão respondeu: " + resposta);
        return resposta;
    }
}