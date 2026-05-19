# Simulador de Assinador Digital 

---

## Sobre o Projeto
Este projeto consiste em uma API desenvolvida em Java (Spring Boot) responsável por simular operações de assinatura digital e validação de documentos. O sistema é capaz de diferenciar e simular operações baseadas em diferentes materiais criptográficos, como tokens de hardware (PKCS#11), smartcards, arquivos locais (PEM/PKCS#12) e serviços remotos.

O fluxo de funcionamento segue o padrão RESTful, baseando-se em requisições de rede (HTTP POST) que enviam os dados do documento e as configurações criptográficas diretamente no corpo (Request Body) da requisição payload JSON, eliminando a dependência de arquivos físicos no disco do servidor.

---

## Como Funciona a Arquitetura

1. O usuário (ou sistema cliente) monta um payload JSON estruturado contendo as chaves, senhas ou metadados da operação.
2. É feita uma requisição de rede (POST) para a API passando esses dados diretamente no corpo da mensagem.
3. O Spring Boot converte automaticamente o JSON recebido no objeto Java correspondente.
4. O `SignService` ou `ValidateService` processa as regras de negócio e devolve uma resposta contendo uma flag de validação, uma mensagem explicativa e o código de status HTTP correto para cada situação.

### Simulação de Hardware (Smartcard via APDU)
Quando o tipoCriptografia escolhido é SMARTCARD, o sistema aciona um simulador interno de comandos hexadecimais (APDU) simulando o chip físico:
* O simulador exige que a senha (PIN) seja exatamente "1234".
* Se o PIN estiver correto, o cartão simulado retorna o código de sucesso 9000 e a API devolve HTTP 200 OK.
* Se o PIN estiver incorreto, o cartão devolve o erro 6900 e a API bloqueia a operação devolvendo HTTP 401 Unauthorized.

### Tabela de Códigos de Erro HTTP da API
* 200 OK: Operação realizada com sucesso.
* 400 Bad Request: Erro de sintaxe (ex: tipo de criptografia não suportado).
* 401 Unauthorized: Falha de autenticação ou integridade violada (ex: PIN do smartcard incorreto ou assinatura inválida).
* 422 Unprocessable Entity: JSON bem estruturado, mas faltando dados obrigatórios para a regra de negócio.
* 500 Internal Server Error: Falha interna simulada no hardware (ex: falha de comunicação APDU).

---

## Exemplos de Payloads para Teste (Rota: /api/sign)

Abaixo estão os modelos de JSON que devem ser colados diretamente no Request Body do Swagger para testar cada cenário da Assinatura:

### 1. Criptografia PEM - Sucesso (HTTP 200)
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "PEM",
  "dadosCriptograficos": {
    "chavePrivada": "-----BEGIN PRIVATE KEY-----"
  }
}
```

### 2. Criptografia PEM - Erro de Campo Ausente (HTTP 422)
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "PEM",
  "dadosCriptograficos": {
    "chavePrivada": ""
  }
}
```

### 3. Criptografia PKCS#12 - Sucesso (HTTP 200)
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "PKCS#12",
  "dadosCriptograficos": {
    "conteudo": "ARQUIVO_BASE64",
    "senha": "senha-secreta",
    "alias": "meu-certificado"
  }
}
```

### 4. Criptografia TOKEN - Sucesso (HTTP 200)
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "TOKEN",
  "dadosCriptograficos": {
    "pin": "123456",
    "identificador": "token-usb-01"
  }
}
```

### 5. Criptografia SMARTCARD - Sucesso PIN Correto (HTTP 200)
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "SMARTCARD",
  "dadosCriptograficos": {
    "pin": "1234",
    "identificador": "leitora-01"
  }
}
```

### 6. Criptografia SMARTCARD - Erro de PIN Incorreto (HTTP 400)
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "SMARTCARD",
  "dadosCriptograficos": {
    "pin": "9999",
    "identificador": "leitora-01"
  }
}
```

### 7. Criptografia REMOTE - Sucesso (HTTP 200)
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "REMOTE",
  "dadosCriptograficos": {
    "enderecoServico": "https://api.nuvem.com",
    "credencial": "token-jwt-aqui"
  }
}
```

### 8. Tipo de Criptografia Não Suportado - Erro (HTTP 400)
```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "INVALIDA",
  "dadosCriptograficos": {
    "pin": "123"
  }
}
```

---

## Exemplos de Payloads para Teste (Rota: /api/validate)

### 1. Validação com Sucesso (HTTP 200)
```json
{
  "conteudo": "CONTEUDO_PDF_BASE64",
  "assinatura": "MOCKED_SIGNATURE_BASE64_==",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica"
}
```

### 2. Validação com Assinatura Inválida/Corrompida (HTTP 401)
```json
{
  "conteudo": "CONTEUDO_PDF_BASE64",
  "assinatura": "ASSINATURA_FALSA_OU_ALTERADA",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica"
}
```

### 3. Validação com Campo Faltante (HTTP 422)
```json
{
  "conteudo": "CONTEUDO_PDF_BASE64",
  "assinatura": "MOCKED_SIGNATURE_BASE64_==",
  "politicaAssinaturaUrl": ""
}
```

---

#### Como Testar o Sistema
Execute o seguinte comando no terminal dentro do diretorio assinador:
`.\mvnw.cmd spring-boot:run`

Após o sistema iniciar com sucesso, abra o navegador e acesse:
`http://localhost:8080/swagger-ui.html`

Expanda a rota desejada (/api/sign ou /api/validate) e clique no botão "Try it out". No corpo da requisição (Request body), limpe o conteúdo padrão e cole um dos exemplos de JSON listados acima. Em seguida, clique em "Execute" para disparar a requisição de rede e visualizar o resultado e o código de resposta HTTP diretamente na tela do Swagger.

---

##### Resultados Esperados na Resposta da API

Ao testar uma operação com Sucesso (Código HTTP 200 OK):
```json
{
  "signature": "MOCKED_SIGNATURE_BASE64_==",
  "valid": true,
  "message": "Assinatura criada com sucesso. Operação: SMARTCARD (Operação validada via comunicação APDU)."
}
```

Ao testar o Smartcard com Senha Errada (Código HTTP 401 Unauthorized):
```json
{
  "signature": null,
  "valid": false,
  "message": "Erro APDU: PIN incorreto. O Smartcard recusou a operação."
}
```

Ao testar a Validação com Assinatura Adulterada (Código HTTP 401 Unauthorized):
```json
{
  "signature": null,
  "valid": false,
  "message": "Assinatura inválida: O conteúdo ou o material criptográfico divergem do original."
}
```

Ao testar payloads com campos em branco (Código HTTP 422 Unprocessable Entity):
```json
{
  "signature": null,
  "valid": false,
  "message": "Erro (PEM): 'chavePrivada' é obrigatória."
}
```