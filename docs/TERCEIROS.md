# Bibliotecas de terceiros

O código e os assets de *Cats & Dungeons* são proprietários (ver `LICENSE`).
As bibliotecas abaixo **não** são: cada uma permanece sob a própria licença.

## Resolvidas do Maven Central

Declaradas em `build.gradle.kts`.

| Biblioteca | Versão | Licença | Uso |
|---|---|---|---|
| [json-simple](https://github.com/fangyidong/json-simple) | 1.1.1 | Apache-2.0 | Leitura de `config.json` e dos JSON de mapas e jogadores |
| [OSHI](https://github.com/oshi/oshi) | 6.8.1 | MIT | Informações de hardware no overlay de depuração (F3) |
| [JNA](https://github.com/java-native-access/jna) | 5.14.0 | Apache-2.0 / LGPL-2.1 | Dependência do OSHI |
| [SLF4J](https://www.slf4j.org/) | 2.0.13 | MIT | Fachada de log exigida pelo OSHI |

## Versionadas em `libs/`

Não têm publicação no Maven Central.

| Biblioteca | Licença | Observação |
|---|---|---|
| [TinySound](https://github.com/finnkuusisto/TinySound) | BSD 2-Clause | Sem release desde 2012. Usa `javax.sound.sampled` por baixo, o que permitiria adicionar suporte a OGG por SPI sem trocar a biblioteca. |
| Sheeter | Interna | Biblioteca do próprio time (`studio.retrozoni.sheeter`), sem código-fonte no repositório. |

## Ferramentas de build

| Ferramenta | Licença |
|---|---|
| [Gradle](https://gradle.org/) | Apache-2.0 |
| [Eclipse Temurin (JDK 21)](https://adoptium.net/) | GPL-2.0 with Classpath Exception |

## Fontes

A fonte `septem` em `src/main/resources/com/retronova/resources/fonts/` precisa
ter a licença confirmada antes de qualquer distribuição pública do jogo.

> **Pendência.** O mesmo vale para os assets de áudio: é preciso confirmar a
> origem e os direitos de cada faixa antes de publicar.
