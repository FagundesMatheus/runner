# Simulador de Assinador Digital 

---

## Sobre o Projeto
Este projeto consiste em uma API desenvolvida em Java (Spring Boot) responsável por simular operações de assinatura digital e validação de documentos. O sistema é capaz de diferenciar e simular operações baseadas em diferentes materiais criptográficos, como tokens de hardware (PKCS#11), smartcards, arquivos locais (PEM/PKCS#12) e serviços remotos.

O fluxo de funcionamento baseia-se em requisições de rede (HTTP) que enviam o caminho de um arquivo `.json` local. A API faz a leitura deste arquivo e executa a lógica de negócio correspondente ao tipo de criptografia selecionado.

---

## Como Funciona a Arquitetura

1. O usuário (ou sistema cliente) cria um arquivo `.json` contendo os dados do documento, os certificados e a configuração de criptografia desejada.
2. É feita uma requisição de rede (POST) para a API, informando apenas o caminho absoluto deste arquivo `.json`.
3. O `SignService` ou `ValidateService` intercepta a requisição, localiza o arquivo na máquina, converte os dados e devolve o status da simulação.

### Simulação de Hardware (Smartcard via APDU)
Um dos grandes diferenciais desta API é a simulação de comunicação de baixo nível com cartões inteligentes (Smartcards). Quando o tipoCriptografia escolhido é SMARTCARD, o sistema aciona um simulador interno de comandos hexadecimais (APDU).
* O simulador exige que a senha (PIN) seja exatamente "1234".
* Se o PIN estiver correto, o cartão simulado retorna o código de sucesso 9000 e a assinatura é gerada.
* Se o PIN estiver incorreto, o cartão devolve o erro 6900 e a API bloqueia a assinatura.

---

## Estrutura dos Arquivos JSON

Para testar o sistema, você deve criar os arquivos JSON localmente em sua máquina. Abaixo estão os modelos para cada operação.

### 1. JSON para Assinatura (Exemplo com Token PKCS#11)
Crie um arquivo chamado `dados_assinatura.json:`
```json
{
  "bundleEndereco": "CONTEUDO_BASE64_DO_DOCUMENTO",
  "provenanceTargetEndereco": "<metadados><autor>Luis Vittor</autor></metadados>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br/carimbo",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "TOKEN",
  "dadosCriptograficos": {
    "pin": "123456",
    "identificador": "chave-cert-1",
    "slotId": 1,
    "tokenLabel": "Meu_eToken_Seguro"
  }
}
```
---

### 2. JSON para Assinatura (Simulação de Smartcard - Sucesso)
Crie um arquivo chamado `smartcard_sucesso.json.` Neste cenário, o PIN enviado é o correto (1234):
```json
{
  "bundleEndereco": "CONTEUDO_BASE64_DO_DOCUMENTO",
  "provenanceTargetEndereco": "<metadados><autor>Luis Vittor</autor></metadados>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br/carimbo",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "SMARTCARD",
  "dadosCriptograficos": {
    "pin": "1234",
    "identificador": "cartao-luis-01"
  }
}
```
---

### 3. JSON para Assinatura (Simulação de Smartcard - Erro de PIN)
Crie um arquivo chamado `smartcard_erro_pin.json`. Aqui forçamos um erro no hardware simulado enviando uma senha incorreta:
```json
{
  "bundleEndereco": "CONTEUDO_BASE64_DO_DOCUMENTO",
  "provenanceTargetEndereco": "<metadados><autor>Luis Vittor</autor></metadados>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br/carimbo",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "SMARTCARD",
  "dadosCriptograficos": {
    "pin": "9999",
    "identificador": "cartao-luis-01"
  }
}
```
---

### 4. JSON para Validação da Assinatura
Crie um arquivo chamado `dados_validacao.json`:
```json
{
  "conteudo": "CONTEUDO_BASE64_DO_DOCUMENTO",
  "assinatura": "MOCKED_SIGNATURE_BASE64_==",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica"
}
```
---

#### Como Testar o Sistema
Execute o seguinte comando no terminal dentro do diretorio assinador:
`.\mvnw.cmd spring-boot:run`

Após isso, se o sistema não apresentar nenhum erro, acesse:
`http://localhost:8080/swagger-ui.html`

Expanda a rota `/api/sign` ou `/api/validate` e clique no botão "Try it out". No corpo da requisição (Request body), informe o caminho do seu arquivo JSON criado anteriormente (lembre-se de utilizar barras normais /). Em seguida, clique em "Execute" para disparar a sincronização de rede e visualizar a resposta da simulação. Vale ressaltar que esse método de teste é provisório e tem como objetivo validar o comportamento das funções principais do sistema.

---

##### Resultados Esperados
Após executar os testes, o sistema deverá processar o arquivo .json local e retornar uma resposta padronizada indicando o sucesso ou a falha da operação. Abaixo estão os resultados esperados:

Ao testar a rota de Assinatura (/api/sign) com TOKEN (Código 200):
O sistema identificará os parâmetros criptográficos fornecidos e retornará a assinatura simulada em Base64.
```json
{
  "signature": "MOCKED_SIGNATURE_BASE64_==",
  "valid": true,
  "message": "Assinatura criada com sucesso. Operação: TOKEN (Operação PKCS#11 validada via Token Local)."
}
```
Ao testar a rota de Assinatura com SMARTCARD (Sucesso - Código 200):
O sistema se comunicará com o simulador APDU, validará a senha "1234" e retornará sucesso.
```json
{
  "signature": "MOCKED_SIGNATURE_BASE64_==",
  "valid": true,
  "message": "Assinatura criada com sucesso. Operação: SMARTCARD (Operação validada via comunicação APDU)."
}
```
Ao testar a rota de Assinatura com SMARTCARD (Erro de PIN - Código 400):
O simulador de hardware rejeitará o acesso, resultando em uma requisição inválida.
```json
{
  "signature": null,
  "valid": false,
  "message": "Erro APDU: PIN incorreto. O Smartcard recusou a operação."
}
```
Ao testar a rota de Validação (/api/validate):
Ao enviar a assinatura gerada e os dados do documento para a rota de validação, o sistema fará o cruzamento das informações e deverá confirmar a integridade do pacote.
```json
{
  "valid": true,
  "message": "Assinatura é válida e o documento está íntegro."
}
```