# Arquitetura

## Sem engine

O jogo **não usa engine**. Não há Unity, Godot, libGDX nem LWJGL. Tudo é código
Java rodando sobre a biblioteca padrão: `java.awt.Canvas` com `BufferStrategy`
para o desenho, `javax.sound.sampled` para o áudio (via TinySound) e
`java.awt.event` para a entrada.

Isso é uma escolha, não uma limitação: o laço de jogo, o sistema de física, o
pathfinding, o renderizador e a interface foram escritos do zero. O que o
projeto chama de "engine" é o pacote `com.retronova.engine`, camada interna
própria — não um motor de terceiros.

## Layout do repositório

```
cats-and-dungeons/
├── .github/workflows/     Pipeline de CI
├── docs/                  Documentação, planejamento e notas de versão
├── gradle/                Wrapper do Gradle
├── libs/                  Dependências sem publicação no Maven Central
├── src/main/java/         Código-fonte
├── src/main/resources/    Sprites, áudio, fontes, mapas e dados
├── src/test/java/         Testes
└── tools/                 Utilitários de desenvolvimento
```

O layout `src/main/java` e `src/main/resources` é a convenção padrão de Maven e
Gradle. O projeto vinha com tudo dentro de um único `src/`, com os `.jar` de
terceiros misturados aos assets.

Os recursos mantêm o caminho `com/retronova/resources/...` dentro de
`src/main/resources` de propósito: assim o caminho de classpath continua
idêntico ao de antes, e nenhuma chamada a `getResource()` precisou mudar.

## Pacotes

O código é organizado **por camada**, não por feature — adequado ao tamanho
atual e à divisão clara entre motor e jogo.

| Pacote | Responsabilidade |
|---|---|
| `com.retronova.engine` | Laço de jogo, janela, configurações, pilha de Activity |
| `engine.graphics` | Sprites, fontes, 9-slice, paleta, escala |
| `engine.inputs` | Teclado e mouse |
| `engine.io` | Leitura de JSON dos recursos |
| `engine.sound` | Música e efeitos |
| `com.retronova.game` | Activity principal do jogo |
| `game.map` | Mapas, arenas, salas, câmera e waves |
| `game.objects` | Entidades, tiles, partículas, física e pathfinding |
| `game.items` | Armas e consumíveis |
| `game.hud` / `game.interfaces` | HUD e telas in-game |
| `com.retronova.menus` | Menu, opções, seleção de personagem, pausa |

## Laço principal

`Engine.run()` mantém dois relógios independentes:

- **Tick** a 60 Hz fixos, com recuperação de atraso limitada a 5 ticks por volta.
  A simulação tem prioridade sobre o desenho.
- **Render** ao limite configurado em Opções, descartando quadros perdidos em
  vez de tentar recuperá-los.

Tick e render compartilham a mesma thread. Quando o desenho atrasa, o jogo perde
quadros, não velocidade.

## Recursos

Sprites são PNG em pixel art de 16×16, ampliados por fator **inteiro** com
nearest-neighbor. Fator fracionário desalinha e borra a arte, por isso toda
escala no projeto é inteira.

Os mapas são PNG em que **a cor de cada pixel identifica um tile**, com um JSON
irmão descrevendo as entidades. Ver `TileIDs` e `GameMap.convertMap`.

Os sprites de UI (botões e ícone) são **gerados** por `tools/GenUiAssets.java` e
versionados. A CI regenera e compara: se o gerador e os PNGs divergirem, o build
quebra. Um artista pode repintá-los à mão, desde que preserve dimensões e
recortes — nesse caso, o gerador deve ser aposentado.

## Áudio

Segue a divisão padrão de jogos: **efeitos em WAV**, curtos e carregados em
memória, para não pagar decodificação onde a latência é perceptível; **músicas
em OGG Vorbis**, lidas em streaming, porque são faixas de minutos.

O TinySound não conhece OGG. Quem decodifica é o `vorbis-support`, registrado
como Service Provider do `javax.sound`: como o TinySound usa
`AudioSystem.getAudioInputStream()` por baixo, basta a biblioteca estar no
classpath. Nenhuma linha de código de áudio mudou por causa do formato — só a
extensão do arquivo.

## Pontos frágeis conhecidos

Registrados para não se perderem:

- `Repulsion` roda numa thread paralela alterando posições enquanto o laço
  principal lê e desenha.
- Threads não-daemon impedem o processo de encerrar ao fechar a janela.
- `resetWindow()` executa operações Swing fora da EDT.
- O `Waves` abre uma thread nova a cada leva de inimigos.
