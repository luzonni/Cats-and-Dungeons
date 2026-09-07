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

## Arte de terceiros

| Pacote | Autor | Licença | Uso |
|---|---|---|---|
| [Kenney — Tiny Dungeon](https://kenney.nl/assets/tiny-dungeon) | Kenney | CC0 1.0 | Base dos tiles e props do cenário da antecâmara |
| [Simple Torch Animation 16x16](https://opengameart.org/content/simple-torch-animation-16x16) | Natural_Privateer | CC0 1.0 | Quadros de chama da tocha e do braseiro |

O pacote do Kenney **não** está versionado. A folha de chama está, em
`tools/assets/` — são 678 bytes, e baixar um arquivo desse tamanho a cada build
custa mais do que guardá-lo.

Os sprites do jogo são derivados, não cópias: `tools/GenKenney.java` remapeia a
paleta azul-acinzentada do Kenney para as rampas de pedra fria, laje e madeira do
projeto — ver `documentation/padroes/ARTE-CENARIO.md` para o porquê. A chama passa
intacta, porque é o único acento quente do cenário. Para regerar:

```
java tools/GenKenney.java <pasta Tiles do pacote Kenney>
```

O texto da licença, como vem no pacote:

> License: (Creative Commons Zero, CC0)
> http://creativecommons.org/publicdomain/zero/1.0/
>
> This content is free to use in personal, educational and commercial projects.
> Support us by crediting Kenney or www.kenney.nl (this is not mandatory)

CC0 é renúncia de direitos, então não há obrigação de atribuição nem restrição de
uso comercial — o crédito acima é cortesia, e o pedido do próprio autor. Isso não
altera a licença proprietária do resto do projeto: obra derivada de CC0 pode ser
licenciada como se queira.

## Áudio de terceiros

| Faixa | Arquivo | Autor | Licença | Uso |
|---|---|---|---|---|
| [Cave Theme](https://opengameart.org/content/cave-theme) | `audio/dungeon_hall.ogg` | Brandon75689 (envio de HaelDB) | CC0 1.0 (também oferecida em OGA-BY 3.0) | Música da antecâmara |
| [Loopable Dungeon Ambience](https://opengameart.org/content/loopable-dungeon-ambience) | `audio/menu_ambient.ogg` | JaggedStone | CC0 1.0 | Leito ambiente da tela de título |

**Cave Theme** foi reencodada em Vorbis com `-2,5 dB` de ganho, para o volume
médio bater com o das outras faixas (−16,6 dBFS) e sobrar margem de pico.
Substituiu `room.ogg`, que era clara e agitada demais para uma masmorra — e cuja
origem, ao contrário desta, nunca foi confirmada.

**Loopable Dungeon Ambience** — vento e goteiras — foi reencodada com `-6 dB` e
em qualidade menor, ficando em −35,8 dBFS de média. Ela toca JUNTO com a música do
menu, não no lugar dela: o nível baixo é o que a mantém como ambiente e não como
segunda faixa disputando com a primeira. Como o nível está gravado no arquivo, ela
continua obedecendo ao controle de música das opções.

## Ferramentas de build

| Ferramenta | Licença |
|---|---|
| [Gradle](https://gradle.org/) | Apache-2.0 |
| [Eclipse Temurin (JDK 21)](https://adoptium.net/) | GPL-2.0 with Classpath Exception |

## Fontes

A fonte `septem` em `src/main/resources/com/retronova/resources/fonts/` precisa
ter a licença confirmada antes de qualquer distribuição pública do jogo.

> **Pendência.** O mesmo vale para as faixas de áudio herdadas do projeto
> original — `geral`, `menu_principal`, `fight` e `game_over`: é preciso
> confirmar a origem e os direitos de cada uma antes de publicar. As duas da
> seção acima, `dungeon_hall` e `menu_ambient`, já estão resolvidas.
