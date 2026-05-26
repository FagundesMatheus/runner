package com.example.assinador.assinador;

import org.springframework.stereotype.Service;

@Service
public class SmartcardApduSimulator {

    // Códigos de status padrão de um Smartcard real (ISO 7816)
    private static final String SUCESSO = "9000";
    private static final String ERRO_PIN_INCORRETO = "6900";
    private static final String INSTRUCAO_NAO_SUPORTADA = "6D00";
    private static final String ERRO_BLOQUEADO = "6983"; // Método de autenticação bloqueado
    private static final String ERRO_NAO_AUTENTICADO = "6982"; // Status de segurança não satisfeito

    // --- Memória RAM do Cartão Virtual ---
    private int tentativasRestantes = 3;
    private boolean bloqueado = false;
    private boolean autenticado = false;

    public String enviarComando(String comandoHex) {
        System.out.println("Terminal enviou: " + comandoHex);

        // Pega a "Instrução" (INS) que fica no 2º byte do comando
        String ins = comandoHex.substring(2, 4); 
        String resposta;

        switch (ins) {
            case "A4" -> { 
                // Comando SELECT (Selecionar o aplicativo no chip)
                this.autenticado = false; // Sempre que seleciona o applet, reseta a sessão
                resposta = SUCESSO;
            }
            case "20" -> { 
                // Comando VERIFY PIN (Verificar Senha)
                if (this.bloqueado) {
                    // Se o chip já queimou, recusa imediatamente
                    resposta = ERRO_BLOQUEADO;
                } else {
                    // Checa se a senha "1234" (31323334 em Hexadecimal) foi enviada
                    if (comandoHex.contains("31323334")) {
                        this.tentativasRestantes = 3; // Reseta o contador ao acertar
                        this.autenticado = true; // Libera o uso da chave privada
                        resposta = SUCESSO; 
                    } else {
                        this.tentativasRestantes--; // Perdeu uma tentativa
                        this.autenticado = false;
                        
                        if (this.tentativasRestantes == 0) {
                            this.bloqueado = true; // Queimou o chip
                            resposta = ERRO_BLOQUEADO;
                        } else {
                            resposta = ERRO_PIN_INCORRETO; 
                        }
                    }
                }
            }
            case "2A" -> { 
                // Comando COMPUTE SIGNATURE (Assinar documento)
                if (this.bloqueado) {
                    resposta = ERRO_BLOQUEADO;
                } else if (!this.autenticado) {
                    resposta = ERRO_NAO_AUTENTICADO; // Tenta assinar sem mandar o PIN antes
                } else {
                    resposta = "MOCKED_SIGNATURE_BASE64_==" + SUCESSO;
                }
            }
            default -> {
                resposta = INSTRUCAO_NAO_SUPORTADA;
            }
        }

        System.out.println("Cartão respondeu: " + resposta);
        return resposta;
    }

    // --- Método Auxiliar para Testes ---
    // Simula o ato de tirar o cartão da leitora e plugar um novo
    public void resetarCartaoVirtual() {
        this.tentativasRestantes = 3;
        this.bloqueado = false;
        this.autenticado = false;
    }
}