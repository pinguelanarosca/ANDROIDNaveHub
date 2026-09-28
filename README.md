# ANDROIDNaveHub

NaveHub para Android, com contas isoladas por perfil nativo de WebView quando `MULTI_PROFILE` está disponível.

## APK pronto

O repositório já contém um APK compilado em:

`release-apk/ANDROIDNaveHub.apk`

Download direto:

https://raw.githubusercontent.com/pinguelanarosca/ANDROIDNaveHub/main/release-apk/ANDROIDNaveHub.apk

## Requisitos para compilar

- Git
- JDK 17
- Android SDK com API 36
- `unzip` e `wget`
- Gradle 9.3.1

O projeto atualmente contém o `gradle-wrapper.properties`, mas não versiona o executável `gradlew`. Por isso, use o Gradle 9.3.1 diretamente ou adicione o wrapper ao projeto.

## Compilar direto do Git

Linux:

```bash
cd ~/Documentos

rm -rf ANDROIDNaveHub
git clone https://github.com/pinguelanarosca/ANDROIDNaveHub.git
cd ANDROIDNaveHub

sudo apt update
sudo apt install -y openjdk-17-jdk wget unzip

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

java -version

wget -q https://services.gradle.org/distributions/gradle-9.3.1-bin.zip -O /tmp/gradle-9.3.1-bin.zip
rm -rf /tmp/gradle-9.3.1
mkdir -p /tmp/gradle-9.3.1
unzip -q /tmp/gradle-9.3.1-bin.zip -d /tmp/gradle-9.3.1

/tmp/gradle-9.3.1/gradle-9.3.1/bin/gradle assembleDebug
```

APK:

`app/build/outputs/apk/debug/app-debug.apk`

Copiar para uma pasta de fácil acesso:

```bash
cp app/build/outputs/apk/debug/app-debug.apk ~/Documentos/ANDROIDNaveHub-debug.apk
```

## Compilar a partir de uma cópia local

Depois de baixar ou clonar o projeto, entre na pasta dele:

```bash
cd /caminho/para/ANDROIDNaveHub
```

Com o JDK 17 configurado e o Gradle 9.3.1 disponível:

```bash
/tmp/gradle-9.3.1/gradle-9.3.1/bin/gradle assembleDebug
```

Ou, caso você tenha o Gradle 9.3.1 instalado no sistema:

```bash
gradle assembleDebug
```

## APK Release

Para gerar o APK de produção:

```bash
/tmp/gradle-9.3.1/gradle-9.3.1/bin/gradle assembleRelease
```

Saída:

`app/build/outputs/apk/release/app-release.apk`

A build Release usa assinatura própria. O projeto espera:

- `KEYSTORE_PATH` opcional; sem ele procura `my-upload-key.jks` na raiz
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- alias: `upload`

Exemplo:

```bash
export KEYSTORE_PATH="/caminho/my-upload-key.jks"
export STORE_PASSWORD="sua-senha"
export KEY_PASSWORD="sua-senha"

 /tmp/gradle-9.3.1/gradle-9.3.1/bin/gradle assembleRelease
```

Não publique senhas, keystores ou arquivos `.env` no Git.

## Instalar no Android via ADB

Com USB debugging habilitado:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Para instalar o APK Release:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Rodar os testes

Testes unitários:

```bash
/tmp/gradle-9.3.1/gradle-9.3.1/bin/gradle test
```

Testes instrumentados em aparelho/emulador conectado:

```bash
/tmp/gradle-9.3.1/gradle-9.3.1/bin/gradle connectedDebugAndroidTest
```

## Problemas comuns

### `./gradlew: Arquivo ou diretório inexistente`

O repositório não possui atualmente o executável `gradlew`. Use o Gradle 9.3.1 diretamente como mostrado acima.

### `JAVA_HOME is not set`

Instale o JDK 17 e configure:

```bash
sudo apt install -y openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

Confirme:

```bash
java -version
```

### `gradle: command not found`

Baixe o Gradle 9.3.1 manualmente:

```bash
wget -q https://services.gradle.org/distributions/gradle-9.3.1-bin.zip -O /tmp/gradle-9.3.1-bin.zip
rm -rf /tmp/gradle-9.3.1
mkdir -p /tmp/gradle-9.3.1
unzip -q /tmp/gradle-9.3.1-bin.zip -d /tmp/gradle-9.3.1
```

Depois execute:

```bash
/tmp/gradle-9.3.1/gradle-9.3.1/bin/gradle assembleDebug
```

## Estrutura de saída

Debug:

`app/build/outputs/apk/debug/app-debug.apk`

Release:

`app/build/outputs/apk/release/app-release.apk`

APK versionado no Git:

`release-apk/ANDROIDNaveHub.apk`

## Fluxo recomendado

Para pegar a versão atual do Git e gerar um APK novo:

```bash
cd ~/Documentos
git clone https://github.com/pinguelanarosca/ANDROIDNaveHub.git
cd ANDROIDNaveHub
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
/tmp/gradle-9.3.1/gradle-9.3.1/bin/gradle assembleDebug
```

O APK estará em `app/build/outputs/apk/debug/app-debug.apk`.
