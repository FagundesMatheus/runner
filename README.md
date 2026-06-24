# Runner

Este repositório contém um ecossistema de ferramentas para assinatura digital de documentos, centrado em um motor Java e complementado por utilitários de linha de comando (CLI) em Python.

## Visão Geral

O projeto é dividido em três componentes principais:

- **`assinador`**: Um motor de assinatura digital desenvolvido em **Java/Spring Boot**. Ele expõe uma API REST e um modo de execução local para operações de assinatura e validação.
- **`CLIHub`**: Um utilitário em **Python** projetado para gerenciar dependências e facilitar a instalação de componentes do ecossistema.
- **`CLIAssinador`**: Uma interface de linha de comando em **Python** que interage com o motor `assinador`, permitindo a automação de fluxos de assinatura a partir de scripts ou do terminal.

## Funcionalidades Principais

### Módulo `assinador` (Java)
- **Dois Modos de Operação**:
  - **Modo API**: Inicia um servidor web na porta `9742` com endpoints REST (`/api/sign`, `/api/validate`) e documentação Swagger UI.
  - **Modo CLI (`-local`)**: Executa uma operação de assinatura ou validação de forma *one-shot*, ideal para integração com outros processos e scripts.
- **Suporte a Múltiplos Materiais Criptográficos**:
  - `PEM`: Chave privada em formato PEM.
  - `PKCS#12`: Arquivos `.p12` ou `.pfx`.
  - `TOKEN` / `SMARTCARD`: Dispositivos físicos ou virtuais via PKCS#11.
  - `REMOTE`: Integração com serviços de assinatura remotos.

### Módulos `CLIHub` e `CLIAssinador` (Python)
- **Executáveis Nativos**: Compilados para Windows, macOS e Linux.
- **Interface Simplificada**: Abstraem a complexidade de chamar o JAR do `assinador`, oferecendo comandos diretos para os usuários.
- **Automação**: Facilitam a integração de funcionalidades de assinatura em scripts de automação.

## Estrutura do Repositório

```
runner/
├── .github/workflows/         # Workflows de CI/CD para releases
│   ├── clihub-release.yml
│   ├── cliassinador-release.yml
│   └── release.yml
├── assinador/                 # Projeto Java (Spring Boot)
│   ├── src/
│   ├── pom.xml
│   └── softhsm2.cfg
├── CLIHub/                    # Código-fonte do CLIHub (Python)
│   └── main.py
├── CLIAssinador/              # Código-fonte do CLIAssinador (Python)
│   └── main.py
└── README.md
```

## Começando

### Pré-requisitos
- **JDK 25** ou superior para construir e executar o módulo `assinador`.
- **Apache Maven** para gerenciar as dependências do projeto Java.
- **Python 3.11+** e **PyInstaller** para construir os CLIs a partir do código-fonte.

### 1. Build dos Componentes

#### Módulo `assinador` (Java)
Navegue até o diretório `assinador` e execute o build com o Maven Wrapper:
```powershell
# No Windows
cd assinador
.\mvnw clean package -DskipTests

# No Linux/macOS
cd assinador
./mvnw clean package -DskipTests
```
O artefato final será `assinador/target/assinador-0.0.1-SNAPSHOT.jar`.

#### Módulos CLI (Python)
Para compilar os CLIs, você precisa do PyInstaller. O exemplo abaixo é para o `CLIAssinador`:
```bash
pip install pyinstaller typer
pyinstaller --onefile --name cliassinador-win.exe CLIAssinador/main.py
```
> **Nota**: Os executáveis para todas as plataformas são gerados automaticamente via GitHub Actions e anexados às Releases do repositório.

### 2. Executando o `assinador`

#### Modo API
Para iniciar o servidor REST na porta `9742`:
```powershell
cd assinador
java -jar target\assinador-0.0.1-SNAPSHOT.jar -API
```
Acesse a documentação da API em `http://localhost:9742/swagger-ui.html`.

#### Modo CLI (Local)
Execute uma operação única, passando o caminho de um arquivo JSON com o payload.

**Assinatura:**
```powershell
java -Dspring.main.banner-mode=off -jar target\assinador-0.0.1-SNAPSHOT.jar -local -assinar "C:\caminho\payload.json"
```

**Validação:**
```powershell
java -Dspring.main.banner-mode=off -jar target\assinador-0.0.1-SNAPSHOT.jar -local -validar "C:\caminho\payload_validacao.json"
```

## Testando com PKCS#11 (SoftHSM2)

O projeto está configurado para testar a integração com dispositivos PKCS#11 usando o simulador **SoftHSM2**.

1.  **Instale o SoftHSM2** no seu sistema operacional.
2.  **Inicialize um token** com um PIN. Ex: `softhsm2-util --init-token --free --label token-usb-01 --pin 1234 --so-pin 5678`.
3.  **Ajuste o caminho da biblioteca** (`.dll`, `.so`) no código do `SignService.java` ou externalize essa configuração.
4.  **Execute uma operação de assinatura** do tipo `TOKEN` ou `SMARTCARD`, fornecendo o `pin` e o `identificador` do token.

O serviço tentará autenticar no dispositivo com o PIN fornecido. Atualmente, após a autenticação bem-sucedida, uma assinatura mockada (`MOCKED_SIGNATURE_BASE64_==`) é retornada. O objetivo principal deste fluxo é validar a camada de comunicação com o hardware.

## Releases e Versionamento

O projeto utiliza **GitHub Actions** para automatizar o processo de release:
- Ao criar uma tag no formato `assinador-v*`, uma nova release é gerada contendo o `.jar` do motor de assinatura.
- Tags no formato `CLI-Hub-v*` ou `cliassinador-v*` disparam a compilação de executáveis nativos (Windows, macOS, Linux) para os respectivos CLIs.

Todos os artefatos de release são **assinados digitalmente com Cosign** (keyless), garantindo sua autenticidade e integridade.

## 💡 Exemplos de Payload

### Assinatura com PEM
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

### Assinatura com Token/Smartcard
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

### Validação de Assinatura
```json
{
  "conteudo": "CONTEUDO_PDF_BASE64",
  "assinatura": "MOCKED_SIGNATURE_BASE64_==",
  "politicaAssinaturaUrl": "http://pki.gov.br/politica"
}
```

## Como Contribuir

Contribuições são bem-vindas! Siga os passos abaixo:

1.  Faça um **fork** do repositório.
2.  Crie uma nova **branch** para sua feature ou correção (`git checkout -b feature/nova-funcionalidade`).
3.  Faça o commit de suas alterações (`git commit -m 'Adiciona nova funcionalidade'`).
4.  Envie para a sua branch (`git push origin feature/nova-funcionalidade`).
5.  Abra um **Pull Request**.

Certifique-se de atualizar os testes e a documentação conforme necessário.

