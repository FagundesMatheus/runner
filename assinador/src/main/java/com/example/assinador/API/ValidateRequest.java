package com.example.assinador.API;

public record ValidateRequest(
        String caminhoArquivoJson
) {
    // Objeto que mapeia o conteúdo de dentro do arquivo JSON
    public record DadosValidacao(
            String conteudo,
            String assinatura,
            String politicaAssinaturaUrl
    ) {}
}