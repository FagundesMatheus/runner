# Runner

## O que é

Este repositório centraliza dois componentes principais:

- `assinador/`: motor Java Spring Boot para assinatura e validação de documentos.
- `CLIHub/`: utilitários Python auxiliares para instalação e gerenciamento de dependências.

O foco principal é o módulo `assinador`, que oferece:

- API REST para `POST /api/sign` e `POST /api/validate`
- modo CLI *one-shot* para integração local sem servidor web
- suporte a diferentes tipos de material criptográfico: `PEM`, `PKCS#12`, `TOKEN`/`SMARTCARD` via PKCS#11 e `REMOTE`

## Como gerar executáveis

### Build do módulo `assinador`

Entre no diretório `assinador` e use o wrapper Maven do projeto:

```powershell
cd assinador
.\mvnw clean package -DskipTests
```

O artefato gerado será:

- `assinador/target/assinador-0.0.1-SNAPSHOT.jar`

> Observação: o `pom.xml` define `java.version` 25. Use um JDK compatível para build e execução.

## Como executar o artefato resultante

### 1) Modo API

Rode o servidor web na porta `9742`:

```powershell
cd assinador
java -jar target\assinador-0.0.1-SNAPSHOT.jar -API
```

Depois disso, você poderá acessar a aplicação via HTTP em `http://localhost:9742`. A interface Swagger geralmente estará disponível em:

- `http://localhost:9742/swagger-ui.html`

Os endpoints principais são:

- `POST /api/sign` — gerar assinatura
- `POST /api/validate` — validar assinatura

### 2) Modo CLI / Local

O modo `-local` executa a operação em formato one-shot, imprimindo JSON no `stdout` e encerrando o processo imediatamente.

Para assinar um arquivo JSON de entrada:

```powershell
cd assinador
java "-Dspring.main.banner-mode=off" "-Dlogging.level.root=ERROR" -jar target\assinador-0.0.1-SNAPSHOT.jar -local -assinar "C:\caminho\para\payload.json"
```

Para validar um arquivo JSON de entrada:

```powershell
cd assinador
java "-Dspring.main.banner-mode=off" "-Dlogging.level.root=ERROR" -jar target\assinador-0.0.1-SNAPSHOT.jar -local -validar "C:\caminho\para\payload_validacao.json"
```

Esse modo é ideal para chamadas de subprocesso, por exemplo, quando o `assinador` for utilizado por um script Python ou outra aplicação externa.

## Teste com SoftHSM2 / PKCS#11

O projeto oferece um caminho de teste para `TOKEN` ou `SMARTCARD` via PKCS#11 usando `assinador/softhsm2.cfg`. Como este é um simulador, o objetivo é verificar o fluxo de interface com o token, não gerar uma assinatura real.

### 1) Preparar o ambiente SoftHSM2

1. Instale o SoftHSM2 no seu sistema.

- Windows: use WSL2 ou um pacote pré-compilado do SoftHSM2 e ajuste o caminho da DLL em `assinador/softhsm2.cfg`.

2. Configure `assinador/softhsm2.cfg` para apontar à biblioteca PKCS#11 do SoftHSM2.

Exemplo Windows:

```text
name = SoftHSM2
library = C:\\Program Files\\SoftHSM2\\lib\\softhsm2-x64.dll
slotListIndex = 0
```

### 2) Inicializar um token

Use `softhsm2-util` para criar um token com label e PINs:

```bash
softhsm2-util --init-token --free --label token-usb-01 --pin 1234 --so-pin 5678
```

Anote o `label` e o `pin` usados para testar o `assinador`.

### 3) Verificar o token e objetos

```bash
softhsm2-util --show-slots
pkcs11-tool --module /usr/lib/softhsm/libsofthsm2.so --list-objects --login --pin 1234
```

### 4) Testar no Swagger via API

1. Inicie o servidor:

```powershell
cd assinador
java -jar target\assinador-0.0.1-SNAPSHOT.jar -API
```

2. Acesse `http://localhost:9742/swagger-ui.html`.
3. No endpoint `POST /api/sign`, use um payload de exemplo com `tipoCriptografia` = `TOKEN` ou `SMARTCARD` e preencha os campos:

```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "TOKEN",
  "dadosCriptograficos": {
    "pin": "1234",
    "identificador": "token-usb-01"
  }
}
```

4. Envie a requisição e observe a resposta. O simulador retornará uma assinatura mockada e o fluxo de autenticação PKCS#11 será acionado internamente.

### 6) Testar localmente no modo CLI

Crie um arquivo `payload_token.json` com o payload acima e execute:

```powershell
cd assinador
java "-Dspring.main.banner-mode=off" "-Dlogging.level.root=ERROR" -jar target\assinador-0.0.1-SNAPSHOT.jar -local -assinar "C:\caminho\para\payload_token.json"
```

A saída será o JSON de resposta no terminal. Se o PIN ou a configuração estiverem incorretos, o console mostrará mensagens de falha PKCS#11.

### 7) Observações importantes

- Este projeto simula a assinatura: o valor retornado será sempre `MOCKED_SIGNATURE_BASE64_==` para os fluxos suportados.
- O objetivo do teste SoftHSM2 é garantir que o caminho de autenticação PKCS#11 seja executado e que o token seja aceito pelo Java.
- Se ocorrer `CKR_PIN_INCORRECT`, `CKR_PIN_LOCKED` ou `CKR_DEVICE_REMOVED`, revise o PIN, o token e o caminho da biblioteca.

## Como executar os testes

No diretório `assinador`, execute:

```powershell
cd assinador
.\mvnw test
```

Os testes estão em `assinador/src/test/java/com/example/assinador/` e cobrem:

- validação de entrada para assinatura e validação
- fluxos de assinatura `PEM`, `PKCS#12`, `TOKEN`/`SMARTCARD` e `REMOTE`
- respostas com códigos HTTP simulados

## Como contribuir

Se você deseja contribuir com o projeto, siga estes passos:

1. Faça um fork do repositório.
2. Crie uma branch para a sua alteração.
3. Mantenha as mudanças pequenas e focadas.
4. Adicione ou atualize testes para qualquer comportamento novo ou alterado.
5. Inclua instruções claras no README se o comportamento ou as flags mudarem.


## Status atual

### Módulo `assinador`

- Arquitetura básica implementada com `controller`, `service` e DTOs (`record`).
- Suporte a modo API e modo CLI local.
- Validação de entrada existente para campos obrigatórios.
- Assinatura principal ainda trabalha com um mock de assinatura fixa (`MOCKED_SIGNATURE_BASE64_==`).
- Há um caminho de integração PKCS#11 em `SignService` usando `assinador/softhsm2.cfg`, mas a execução real depende do ambiente e das bibliotecas nativas.
- A validação retorna `200` apenas para a assinatura mock esperada e `401` para qualquer outro valor.

### Módulo `CLIHub`

- Contém utilitários Python para instalação e gerenciamento de dependências.
- Não é o foco principal desta documentação, mas está disponível como suporte ao projeto.

### Observações gerais

- O arquivo `assinador/softhsm2.cfg` deve ser ajustado ao ambiente local antes de usar `TOKEN` ou `SMARTCARD`.
- As operações `TOKEN`/`SMARTCARD` devem usar um PIN válido e uma biblioteca PKCS#11 configurada.

## Estrutura do repositório

- `assinador/` — projeto Java principal
  - `pom.xml` — configuração Maven
  - `mvnw`, `mvnw.cmd` — wrappers do Maven
  - `src/main/java` — código-fonte Java
  - `src/test/java` — testes de unidade
  - `softhsm2.cfg` — configuração de provedor PKCS#11
- `CLIHub/` — utilitários Python auxiliares
- `release.json`, `roadmap.md`, `plano.md` — arquivos de planejamento e release

## Exemplo de payloads de uso

### Exemplo de assinatura PEM

```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----...",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "PEM",
  "dadosCriptograficos": {
    "chavePrivada": "-----BEGIN PRIVATE KEY-----..."
  }
}
```

### Exemplo de validação

```json
{
  "conteudo": "CONTEUDO_PDF_BASE64",
  "assinatura": "MOCKED_SIGNATURE_BASE64_==",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica"
}
```

### Exemplo de token/smartcard

```json
{
  "bundleEndereco": "CONTEUDO_PDF_BASE64",
  "provenanceTargetEndereco": "<autor>Luis</autor>",
  "cadeiaCertificadosEndereco": "-----BEGIN CERTIFICATE-----...",
  "fonteTemporal": "http://pki.gov.br",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica",
  "tipoCriptografia": "TOKEN",
  "dadosCriptograficos": {
    "pin": "1234",
    "identificador": "token-usb-01"
  }
}
```

