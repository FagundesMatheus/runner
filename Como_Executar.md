# Simulador de Assinador Digital 

---

## Sobre o Projeto
Este projeto consiste em um motor criptográfico desenvolvido em Java (Spring Boot) responsável por realizar operações de assinatura digital e validação de documentos. O sistema é capaz de diferenciar e operar com diferentes materiais criptográficos, como tokens de hardware virtuais (SoftHSM2 via PKCS#11), arquivos locais (PEM/PKCS#12) e serviços remotos.

O grande diferencial desta arquitetura é o seu formato **Híbrido**. O sistema atua tanto como uma **API RESTful** (recebendo requisições via rede na porta 9742) quanto como uma ferramenta de **Linha de Comando (CLI)** nativa, permitindo integração direta via terminal com scripts em Python ou Shell, eliminando a dependência de um servidor web sempre ligado.

---

## Como Funciona a Arquitetura

O motor Java possui um roteador inteligente que decide o modo de execução com base nas *flags* passadas na inicialização:

### 1. Modo API (`-API`)
* O servidor web é iniciado na porta **9742**.
* O fluxo segue o padrão RESTful, recebendo requisições HTTP POST que enviam os dados de configuração diretamente no corpo (Request Body) em formato JSON.

### 2. Modo CLI / Local (`-local`)
* O Spring Boot é executado no modo *One-Shot* (Tiro Único), sem subir o servidor web.
* Requer as *flags* de operação (`-assinar` ou `-validar`) seguidas do **caminho absoluto do arquivo JSON** no disco.
* O sistema lê o arquivo, processa a criptografia conectando-se ao hardware, imprime a resposta JSON diretamente no terminal (`stdout`) e encerra o processo imediatamente, devolvendo o controle ao sistema operacional.

---

## Integração Real com Hardware (SoftHSM2)
Quando o `tipoCriptografia` escolhido é `TOKEN` ou `SMARTCARD`, o sistema abandona as simulações em software e aciona a integração via provedor `SunPKCS11`:
* O Java se comunica diretamente com a biblioteca dinâmica (`.dll` ou `.so`) configurada no arquivo `softhsm2.cfg`.
* O motor accesses o cofre criptográfico real no sistema operacional exigindo o PIN correto (ex: "1234").
* Se o PIN estiver correto, a assinatura é gerada e o sistema retorna HTTP 200 OK.
* Se o PIN estiver incorreto, o acesso ao hardware é negado, gerando o erro de autorização e devolvendo HTTP 401 Unauthorized.

### Tabela de Códigos de Erro HTTP
* **200 OK:** Operação realizada com sucesso.
* **400 Bad Request:** Erro de sintaxe (ex: tipo de criptografia não suportado).
* **401 Unauthorized:** Falha de autenticação ou integridade violada (ex: PIN do token incorreto ou assinatura inválida).
* **422 Unprocessable Entity:** JSON bem estruturado, mas faltando dados obrigatórios para a regra de negócio.
* **500 Internal Server Error:** Falha interna de processamento no Java.

---

## Exemplos de Payloads para Teste (Rota: Assinatura)

Abaixo estão os modelos de JSON que devem ser utilizados para testar cada cenário.

### Métodos Auxiliares

Os exemplos de payloads abaixo servem de guia estrutural para preencher e validar as requisições tanto localmente quanto na API.

### Testes de Validações Base (HTTP 400 e 422)

**Erro de Campo Ausente (HTTP 422)**
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "[http://pki.gov.br](http://pki.gov.br)",
  "politicaAssinaturaUrl": "[http://pki.gov.br/politica](http://pki.gov.br/politica)",
  "tipoCriptografia": "PEM",
  "dadosCriptograficos": {
    "chavePrivada": ""
  }
}
```

### Testes - Tipo: PEM

**Sucesso (HTTP 200)**
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "[http://pki.gov.br](http://pki.gov.br)",
  "politicaAssinaturaUrl": "[http://pki.gov.br/politica](http://pki.gov.br/politica)",
  "tipoCriptografia": "PEM",
  "dadosCriptograficos": {
    "chavePrivada": "-----BEGIN PRIVATE KEY-----"
  }
}
```

### Testes - Tipo: PKCS#12

**Sucesso (HTTP 200)**
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "[http://pki.gov.br](http://pki.gov.br)",
  "politicaAssinaturaUrl": "[http://pki.gov.br/politica](http://pki.gov.br/politica)",
  "tipoCriptografia": "PKCS#12",
  "dadosCriptograficos": {
    "conteudo": "ARQUIVO_BASE64",
    "senha": "senha-secreta",
    "alias": "meu-certificado"
  }
}
```

### Testes - Integração Real com Hardware (Token e Smartcard)

**Sucesso com PIN Correto (HTTP 200)**
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "[http://pki.gov.br](http://pki.gov.br)",
  "politicaAssinaturaUrl": "[http://pki.gov.br/politica](http://pki.gov.br/politica)",
  "tipoCriptografia": "TOKEN",
  "dadosCriptograficos": {
    "pin": "1234",
    "identificador": "token-usb-01"
  }
}
```

**Erro de Autenticação - PIN Incorreto (HTTP 401)**
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "[http://pki.gov.br](http://pki.gov.br)",
  "politicaAssinaturaUrl": "[http://pki.gov.br/politica](http://pki.gov.br/politica)",
  "tipoCriptografia": "SMARTCARD",
  "dadosCriptograficos": {
    "pin": "9999",
    "identificador": "leitora-01"
  }
}
```

### Testes - Tipo: Remote

**Sucesso (HTTP 200)**
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "[http://pki.gov.br](http://pki.gov.br)",
  "politicaAssinaturaUrl": "[http://pki.gov.br/politica](http://pki.gov.br/politica)",
  "tipoCriptografia": "REMOTE",
  "dadosCriptograficos": {
    "enderecoServico": "[https://api.nuvem.com](https://api.nuvem.com)",
    "credencial": "token-jwt-aqui"
  }
}
```

### Tipo Inválido

**Erro (HTTP 400)**
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "[http://pki.gov.br](http://pki.gov.br)",
  "politicaAssinaturaUrl": "[http://pki.gov.br/politica](http://pki.gov.br/politica)",
  "tipoCriptografia": "INVALIDA",
  "dadosCriptograficos": {
    "pin": "1234"
  }
}
```

---

## Exemplos de Payloads para Teste (Rota: Validação)

### Validação com Sucesso (HTTP 200)
```json
{
  "conteudo": "CONTEUDO_PDF_BASE64",
  "assinatura": "MOCKED_SIGNATURE_BASE64_==",
  "politicaAssinaturaUrl": "[http://pki.gov.br/politica](http://pki.gov.br/politica)"
}
```

### Validação com Assinatura Inválida/Corrompida (HTTP 401)
```json
{
  "conteudo": "CONTEUDO_PDF_BASE64",
  "assinatura": "ASSINATURA_FALSA_OU_ALTERADA",
  "politicaAssinaturaUrl": "[http://pki.gov.br/politica](http://pki.gov.br/politica)"
}
```

---

## Como Testar o Sistema

Primeiro, compile o projeto para gerar o executável `.jar`:
``` .\mvnw clean package -DskipTests ```

### Método 1: Via API Web (Swagger / Postman)
Inicie o servidor rodando o comando com a flag `-API`:
``` java -jar target\assinador-0.0.1-SNAPSHOT.jar -API ```
Após o sistema iniciar, abra o navegador e acesse a documentação do Swagger em:
`http://localhost:9742/swagger-ui.html`
*(Utilize as rotas `/api/sign` ou `/api/validate` colando os JSONs no Request Body).*

### Método 2: Via Terminal CLI (Integração Local)
Para testar como uma ferramenta de linha de comando (ideal para uso com subprocessos em Python), salve um dos payloads acima em um arquivo físico (ex: `payload.json`) e execute a chamada silenciosa:

**Para Assinar:**
``` java "-Dspring.main.banner-mode=off" "-Dlogging.level.root=ERROR" -jar target\assinador-0.0.1-SNAPSHOT.jar -local -assinar "C:\caminho\absoluto\para\o\payload.json"
```

**Para Validar:**
``` java "-Dspring.main.banner-mode=off" "-Dlogging.level.root=ERROR" -jar target\assinador-0.0.1-SNAPSHOT.jar -local -validar "C:\caminho\absoluto\para\o\payload_validacao.json"
```
*A resposta será impressa diretamente no console em formato JSON, e o processo será encerrado em seguida.*
```