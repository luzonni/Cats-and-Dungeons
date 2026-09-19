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

### A régua de loudness

Os arquivos **não** são usados como chegaram. `tools/GenMixagem.java` põe todos
na mesma régua, e isso é o que torna os controles de volume utilizáveis.

O estado anterior era um laço sem saída: as músicas iam de **-6,9 LUFS**
(`fight_boss`) a **-33,8** (`menu_ambient`), vinte e sete decibéis — o chefe
soava seis vezes e meia mais alto que o menu. Os efeitos iam de **-13,0 dB RMS**
(`laser`) a **-43,6** (`woosh`), trinta e um decibéis. Como a dispersão estava
DENTRO de cada categoria, e um slider move a categoria inteira junto, não existia
posição certa do controle: subir para ouvir a espada estourava o laser. Onze
arquivos chegavam ainda em 0 dBFS ou acima, já clipados.

Normalizar **não** é achatar — o passo do gato deve ser mais baixo que a
explosão. O que estava errado era a diferença ser acidental. Então são camadas:

| Camada | Alvo | O que entra |
|---|---|---|
| Fundo | -34 dB RMS | passo, ponteiro, ronco |
| Rotina | -28 dB RMS | espada, flecha, laser, impactos elementais |
| Evento | -24 dB RMS | moeda, portal, gritos de bicho |
| Marcante | -20 dB RMS | levar dano, explosão, conquista |
| Trilha | -18 LUFS | todas as músicas |
| Leito ambiente | -27 LUFS | `menu_ambient`, colchão debaixo da música |

RMS para efeito e LUFS para música porque o algoritmo da EBU R128 trabalha em
blocos de 400 ms: em arquivo de 30 ms ele devolve -70, que quer dizer "não medi".
O teto de pico é -1,5 dBFS, com folga para o mixer somar dois sons.

O gerador é **idempotente** — o alvo é absoluto, então a segunda passada não
escreve nada. Isso importa para os OGG, que perderiam uma geração de qualidade a
cada execução. Ele deve rodar **depois** dos outros geradores de áudio.

### A curva dos controles

`Ganho.java` converte a posição do slider em ganho. Era `valor/100`, ganho
linear, que é um erro conhecido: audição é logarítmica, então metade do curso
cobria só os últimos 6 dB e o resto quase não respondia.

A curva é `y = a·e^(b·x)` com **40 dB** de alcance, mais uma rampa em reta nos
10% finais para o mínimo ser silêncio de verdade — exponencial nunca chega a
zero. Quarenta, e não os sessenta da recomendação genérica, porque os arquivos já
passaram pela régua: o slider ajusta gosto, não conserta loudness.

Há **três** controles: Master, Music e Sound effects. O master multiplica os
outros dois, para baixar o jogo inteiro sem desfazer o equilíbrio.

Configs antigos são migrados uma vez, pela ausência da chave `AUDIOVERSION`: os
números do mundo linear são convertidos para a posição equivalente na curva
(`MUSIC 20` vira `65`). Sem isso, quem já jogava abriria o jogo em -32 dB.

## Pontos frágeis conhecidos

Registrados para não se perderem:

- `Repulsion` roda numa thread paralela alterando posições enquanto o laço
  principal lê e desenha.
- Threads não-daemon impedem o processo de encerrar ao fechar a janela.
- `resetWindow()` executa operações Swing fora da EDT.
- O `Waves` abre uma thread nova a cada leva de inimigos.
