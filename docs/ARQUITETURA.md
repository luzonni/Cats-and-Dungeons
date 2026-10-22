# Arquitetura

## Sem engine

O jogo **não usa engine**. Não há Unity, Godot, libGDX nem LWJGL. Tudo é código
Java rodando sobre a biblioteca padrão: `java.awt.Canvas` com `BufferStrategy`
para o desenho, `javax.sound.sampled` para o áudio (via TinySound) e
`java.awt.event` para a entrada.

Isso é uma escolha, não uma limitação: o laço de jogo, o sistema de física, o
renderizador e a interface foram escritos do zero. O que o
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
| `com.retronova.engine` | Laço de jogo, janela, configurações, pilha de Activity, medidor de quadros (`Desempenho`) |
| `engine.graphics` | Sprites, fontes, 9-slice, paleta, escala, texto com contorno (`TextFx`), relevo |
| `engine.inputs` | Teclado, mouse, controle (`gamepad/`) e modo de entrada (`Entrada`) |
| `engine.io` | Leitura de JSON dos recursos |
| `engine.sound` | Música, efeitos e curva de volume (`Ganho`) |
| `com.retronova.game` | A partida (`Game`), o save (`Corrida`), a progressão entre corridas (`Meta`), as conquistas e as estatísticas da corrida |
| `game.map` | Mapa base, câmera, luz e sombra (`Atmosfera`) |
| `game.map.arena` | Arena, horda (`Waves`), campo gerado (`Gerador`), cartas de nível, caça-níquel, escolha de elemento, música da arena |
| `game.map.room` | Salas desenhadas à mão (hoje só a antecâmara) |
| `game.objects` | Entidades, tiles, partículas, física e o golpe com aviso (`Investida`) |
| `game.objects.entities` | Gato, inimigos, mobília, projéteis e as mecânicas de corrida: `Bonus`, `Arsenal`, `ArmasDeArea`, `Furia`, `Habilidades` |
| `game.items` | Armas da mão, consumíveis, `Elemento`, `Classe`, raridades, `Melhoria`, `Passiva`, `ArmaExtra`, `Evolucao` |
| `game.hud` / `game.interfaces` | HUD (relógio, barra de chefe, flash de dano) e telas in-game (lojas; o inventário está desativado) |
| `com.retronova.menus` | Menu, saves, seleção de personagem, opções, pausa, fim de corrida, vitória, conquistas |

## Laço principal

`Engine.run()` segue o "Fix Your Timestep!" (Glenn Fiedler) e o "Game Loop" do
*Game Programming Patterns* (Robert Nystrom): **lógica em passo fixo, desenho
interpolado**. Tick e quadro são **prazos absolutos** no relógio
(`System.nanoTime()`), e cada um avança um período exato a partir do anterior.

- **Tick** a 60 Hz fixos. Movimento, física, colisão, timers, nascimento,
  recargas e animações de sprite rodam só aqui. Se o desenho atrasar, até 5 ticks
  rodam em sequência para recuperar, e além disso a dívida é perdoada. Por isso o
  jogo se comporta igual a 60 ou a 240+ FPS: o que muda é quantas vezes o mesmo
  estado é desenhado.
- **Quadro** no ritmo escolhido (ver abaixo). Um quadro atrasado mais de um
  período recomeça a contagem dali, em vez de mandar quadros em rajada.
- O desenho **interpola** entre o tick anterior e o atual (`Engine.alpha()`), e
  as animações de corpo usam `Game.tempoVisual()`. Com uma tela por cima (pausa,
  cartas, roleta), a interpolação é desligada, senão o mundo parado tremeria.
- **Regra para código novo:** nada que conta tempo ou avança animação pode morar
  em `render`. O portal dos bichos avançava o quadro dentro do desenho e girava
  mais rápido a 240 FPS (corrigido em `2b4bdca`).

### Ritmo dos quadros: VSync e limite

| VSync | Limite (Settings → Frame rate) | O que acontece |
|---|---|---|
| Ligado | ignorado | O buffer pede vsync (`ExtendedBufferCapabilities`, só no OpenGL). Se o driver honra, o `show()` espera o monitor. Se ignora (comum em janela, e nesta máquina também em tela cheia), isso é detectado nos 20 primeiros quadros de cada buffer (`Engine.sondarVsync`: quadros abaixo de 70% do período do monitor) e o relógio assume na taxa do monitor; antes a detecção levava um segundo, que rodava solto a 400+ FPS logo ao abrir. |
| Desligado | **Monitor** (padrão) | Relógio na taxa do monitor em que a janela está. |
| Desligado | 30 a 240 | Relógio no valor escolhido. Acima da taxa do monitor (marcado com "!" em Settings), só acrescenta rasgo. |
| Desligado | Unlimited | Desenha sem esperar. |

- **A taxa do monitor** é relida a cada segundo (`Engine.conferirMonitor`), então
  vale a do monitor para onde a janela foi arrastada. Quando `DisplayMode` devolve
  taxa desconhecida, o jogo usa 60 Hz.
- **A espera não confia no `Thread.sleep`.** No Windows, ele dura o que o timer do
  sistema deixar: medido nesta máquina, `sleep(1)` dormiu 8,9 ms em média e 19 ms
  no pior caso com o timer padrão de 15,6 ms. Desde o Windows 10 2004 a resolução
  é por processo, e o Java não a pede. Duas defesas:
  - `RelogioDoSistema` chama `timeBeginPeriod(1)` (winmm, via JNA) na abertura e
    `timeEndPeriod(1)` no fim, como fazem a SDL e a Unity;
  - `Engine.esperarAte` mede cada sono e só dorme quando sobra tempo para o pior
    sono recente, girando (`Thread.onSpinWait`) no trecho final.

  Era a causa do FPS inconstante sem VSync, introduzida em `508384d` ao trocar a
  espera para "dormir longe do quadro", que supunha um `sleep(1)` de 1 a 2 ms.
- **Padrão da primeira execução:** VSync ligado e limite "Monitor". Um jogador
  novo nunca começa com o ritmo acima do que a tela mostra.
- **GC e flags da JVM:** medido com a horda do minuto 8, o GC gasta de 0 a 4 ms
  a cada 2 s, sem coincidir com os quadros ruins. Nenhuma flag de JVM foi
  acrescentada; o jar duplo-clicado também não teria como recebê-las.

Medição (`-Dcd.perf=true`, horda do minuto 8, piloto automático, monitor de 180
Hz, janela de 2 s estável):

| Cenário | Antes | Depois |
|---|---|---|
| VSync ligado (relógio a 180) | 174–180 FPS, desvio 0,8–1,7 ms | 180 FPS, desvio 0,45–0,7 ms |
| Sem VSync, limite 240 | 148–224 FPS, desvio até 4,7 ms, 1% low 32 FPS | 240 FPS, desvio 0,4 ms, 1% low 170–179 FPS |
| Sem VSync, limite Monitor | — | 180 FPS, desvio 0,36 ms |
| Sem VSync, limite 60 | — | 60 FPS, desvio 0,5–1,1 ms |

### Janela, resolução e DPI

- **Resolução é o tamanho da imagem do jogo**, em pixels da tela. Todo o jogo se
  posiciona por `window.largura()` e `window.altura()`, que são a resolução, e não
  o tamanho do canvas.
- **Quando o canvas tem exatamente a resolução**, o desenho vai direto. Quando
  não tem (tela cheia em resolução menor que o monitor, janela que não cabe,
  janela redimensionada), a imagem é desenhada numa `VolatileImage` e escalada
  na placa de vídeo, centrada, com tarjas pretas. Ampliação por fator inteiro usa
  vizinho mais próximo. Fator quebrado amplia primeiro pelo maior inteiro, sem
  suavizar, e suaviza só o resto ("sharp bilinear"). O mouse é convertido do
  canvas para a imagem (`Window.paraJogoX/Y`).
- **Lista de resoluções:** as comuns que cabem no monitor da janela, mais a dele
  (`Window.resolucoesDoMonitor`); o asterisco marca a do monitor. Gravada como
  largura e altura (`resW`, `resH`). O índice antigo é migrado. Se a gravada não
  cabe no monitor atual, vale a do monitor, sem apagar a escolha. 0×0 (padrão) é
  "a do monitor".
- **Tela cheia é uma janela sem bordas** do tamanho do monitor em que a janela
  está, em todos os sistemas. A tela cheia exclusiva (`setDisplayMode`) não é
  usada: ela troca o modo do monitor, o que apaga a tela a cada troca e a cada
  Alt+Tab (tutorial "Full-Screen Exclusive Mode" da Oracle). Resolução diferente
  da do monitor é feita pela escala acima.
- **Em janela**, a janela tem a resolução pedida. Se não cabe na área útil do
  monitor (sem a barra de tarefas), ela encolhe na proporção da imagem.
- **Aplicar vídeo não recria a janela** (`Window.aplicarVideo`): mudar a
  resolução só redimensiona. Ligar ou desligar a tela cheia esconde e mostra o
  mesmo `JFrame` uma vez, porque `setUndecorated` exige a janela fora da tela.
  Resolução e tela cheia são gravadas juntas no Apply e aplicadas numa passada.
- **Perda de superfície** segue o Javadoc de `BufferStrategy`: redesenha
  enquanto `contentsRestored()` e mostra de novo enquanto `contentsLost()`, com a
  mesma estratégia. Ela só é recriada depois de um segundo inteiro perdida
  (monitor dormindo). Antes, todo `contentsLost()`, como o de um Alt+Tab,
  recriava a estratégia, e isso era a piscada.
- **DPI:** `Main` fixa `sun.java2d.uiScale=1` em todo sistema (JEP 263). Sem isso,
  a 125% o Java2D esticaria uma janela "1280" para 1600 pixels e reamostraria a
  pixel art. O macOS ignora a opção e desenha sempre na escala da tela. Quem
  quiser outra escala passa `-Dsun.java2d.uiScale` na linha de comando.
- **Diagnóstico:** `-Dcd.video=true` imprime, a cada mudança de vídeo, a
  resolução pedida, a imagem, o canvas, a janela, o monitor, a escala de DPI, a
  taxa e cada criação de `BufferStrategy`. O F3 mostra o FPS com o alvo real e um
  gráfico dos últimos 240 quadros.

### Escala da interface e layout do HUD

**Um sistema só** (`engine/EscalaDaInterface.java`), no mesmo modelo de
coordenadas e DPI da correção de resolução. A interface acompanha o tamanho da
imagem do jogo, em pixels da tela (`uiScale=1`). O DPI do sistema não entra.

- **Resolução de referência: 640×360.** É nela que o HUD foi desenhado, em pixels
  de arte. O fator da tela é `min(largura/640, altura/360)`, pelo lado mais
  apertado, para nada sair da tela em outra proporção.
- **Fórmulas, sempre multiplicativas e a partir dos valores de referência:**
  - HUD = `arredonda(fator × Interface% × HUD%)`, no teto do layout (abaixo);
  - menus = `arredonda(fator × 1,125 × Interface% × Menus%)`, no teto de 320×180
    pixels de arte por tela (os menus são compostos em 384×216). O 1,125 faz o
    100% valer 2 em 720p e 3 em 1080p: com 1,5 (a primeira versão) o padrão
    ficava grande demais, e o tamanho escolhido nos testes foi o antigo 75%;
  - margem = `arredonda(fator × margem de referência)`.
- **Escala inteira, arredondada uma vez, no fim.** Fator quebrado borra a pixel
  art (suavizada) ou deixa pixels de larguras diferentes (vizinho mais próximo).
  As resoluções 16:9 comuns são múltiplas de 640×360 (720p = 2, 1080p = 3, 1440p =
  4, 4K = 6), e entre elas 100% ocupa **exatamente a mesma fração** da tela. Fora
  delas (1366×768, 1600×900), o erro é o de arredondar para o pixel inteiro.
- **Tamanho mínimo legível:** a partir de 1280×720 nenhuma camada desce de 2, porque
  as fontes de pixel art têm de 5 a 8 pixels e, em escala 1, ficam ilegíveis.
- **Preferências (Settings → Interface):** Interface size, Menu size e HUD size
  de 50% a 200% em passos de 5% (padrão 100%), Margin de 0 a 16 pixels de
  referência (padrão 7) e Reset interface. O máximo de cada slider é o que cabe na
  tela atual (`OptionRow.ate`). Os valores antigos (GUI size 1/2/3, HUD size em
  pixels, Margin em pixels) migram sozinhos. Como a escala é inteira, nem todo
  passo de 5% muda o tamanho visível: o desenho troca quando o produto cruza a
  metade entre dois inteiros.

**Layout por âncoras** (`game/hud/LayoutDoHud.java`). Cada peça do HUD tem uma
âncora, um deslocamento e um tamanho em pixels de arte:

| Peça | Âncora | Deslocamento | Tamanho |
|---|---|---|---|
| VIDA (retrato, frasco, barra) | topo-esquerda | 0 | 136×32 |
| RELOGIO | topo-centro | 0 | 80×24 |
| XP | topo-direita | 0 | 17×17 |
| CHEFE (nome e barra) | topo-centro | 36 | 180×16 |
| FURIA (rótulo e barra) | base-centro | 29 | 72×14 |
| HOTBAR (casas e orelhas) | base-centro | 0 | 80×22 |

A posição na tela sai da âncora, da margem e da escala a cada leitura
(`LayoutDoHud.caixa`). Quem desenha a peça pede a caixa e não faz conta própria.
**A escala do HUD em vigor é a menor entre a pedida e a maior em que:**

1. toda peça cabe dentro da margem;
2. nenhuma peça encosta em outra (vão de 4 pixels de arte);
3. as faixas de cima e de baixo somam no máximo metade da altura (a outra metade é
   da arena).

Tudo é recalculado a cada tick a partir da imagem do jogo. Por isso trocar
resolução, janela ou monitor, ou mexer num slider, vale no quadro seguinte, sem
recriar nada. **F7** desenha as caixas, as âncoras e a margem. `LayoutDoHudTest`
cobre a matriz inteira: resoluções, por cento e margens.

**Para acrescentar uma peça ao HUD:**

1. Acrescente um item em `LayoutDoHud.Peca` com âncora, deslocamento e tamanho em
   pixels de arte.
2. No desenho, pegue a posição de `LayoutDoHud.caixa(Peca.X)` e desenhe dentro
   dela em pixels de arte × `Configs.HudScale()`.
3. Rode `LayoutDoHudTest`. O teto e o teste passam a considerar a peça sozinhos.
   Se ela cruzar outra, mude o deslocamento.

## Fluxo de uma partida

As telas são `Activity` numa pilha (`Engine.heapActivity` / `backActivity`).
`Engine.pause(tela)` põe uma tela por cima e para os ticks de baixo, sem parar o
desenho.

```
Menu ─ Play ─▶ Saves ─ espaço vazio ─▶ Personagens ─ Embark ─▶ Game(Room "beginning")
                    └ espaço gravado ─▶ Game.retomar(Corrida)          │
                                                                      portão
                                                                        ▼
                                              Game(Arena, dificuldade 0) — corrida sem fim
                                                     │ morte ou "Finish Run"
                                                     ▼
                                              GameOver (resumo + loja permanente)
```

**A corrida sem fim.** O portão da antecâmara executa `LOAD ARENA` (o `EASY`
que segue no mapa é ignorado). A `Arena` monta o campo com `Gerador.cisterna`
(160×160 tiles, só a borda de parede) e o `Waves` comanda a horda: não há ondas
nem salas, só o relógio. Por minuto de corrida entram tipos novos de inimigo,
grupos maiores e mais frequentes, cercos a cada 40 s (o primeiro perto de
1min35), um chefe a cada cinco minutos e o Rato Rei aos 30.

O gato não atira: as armas procuram o alvo alcançável mais próximo e atacam
sozinhas (`Item.alvoAlcancavel`). O jogador anda, dá dash e escolhe cartas.

**O que sobe durante a corrida**, cada coisa num lugar:

| Fonte | Quando | Dá |
|---|---|---|
| `SubirDeNivel` | a cada nível | 3 cartas: atributo com raridade, passiva, próximo nível de arma extra |
| `Roleta` | ao abrir um baú (elite ou sorte) | arma extra nova ou nível dela, carta de `Melhoria`, ou a evolução pronta |
| `EscolhaDeElemento` | aos 45 s | o elemento da corrida, que transmuta a arma da mão |
| `Furia` | a cada 30 abates | onda de choque automática |
| `Quebravel` | caixotes e barris perto do gato | cura ou XP grande |

`Game.tick` é o único laço que roda todas as entidades, e por isso concentra as
regras que valem para todo inimigo: ninguém age enquanto o gato cai, ninguém age
além da coleira de 20 tiles e, no campo aberto, quem ficou para trás reaparece
perto do gato (`Arena.trazerDePerto`).

**O modo antigo, por ondas e salas,** saiu do código em 2026-10-21, porque
nenhum caminho do jogo chegava nele: as dificuldades `normal` e `hard`, as
plantas `cistern_1` a `cistern_6`, os JSON de `maps/waves/`, a `Recompensa` de
fim de sala, o vendedor que aparecia ao lado da alavanca, o `TrapDoor` e o A\*
do zumbi. Está no histórico do git, antes da branch `docs/estado-atual`.

## Desenho

- **OpenGL do Java 2D**, ligado em `Main` antes de qualquer classe AWT: ligá-lo
  depois não tinha efeito, e o jogo desenhava em software.
- **VSync** opcional, por `ExtendedBufferCapabilities`, uma API interna do
  `java.desktop`. Por isso o `run` e o fat-jar exportam
  `java.desktop/sun.java2d.pipe.hw`.
- **Só o que está na tela**: tiles e entidades fora da janela (com dois tiles de
  folga) não são desenhados.
- **`Atmosfera`** desenha sombras suaves debaixo das entidades e a luz por cima
  do mundo; iluminação e vinheta podem ser desligadas nas opções.
- Inimigos e gato ganham movimento no desenho (quique, achata-estica, virada)
  sem quadro novo de arte: `Game.animarBicho` e `Game.animarGato`.

## Interface

Tudo o que é compartilhado entre telas fica em `menus/shared/`. Uma tela nova
monta a partir daqui e não redesenha título, rodapé, hover ou carta.

### Sistema de menus (`TelaDeMenu`)

Toda tela de menu estende `TelaDeMenu`: Menu, Saves, Personagens,
Configuracoes (Settings e pausa), TelaDeConquistas, GameOver e Vitoria. A
moldura é sempre a mesma:

```
┌────────────────────────────────────────────┐
│                  Título                    │  baseDoTitulo(), fonte Game 14·s
│                                            │
│            área do conteúdo                │  areaDoConteudo()
│                                            │
│   [Enter] Select   [Q/E] Tabs   [Esc] Back │  Rodape: dicas do modo de entrada
└────────────────────────────────────────────┘
```

- **Fundo pelo contexto.** Fora da corrida, a ilustração viva da tela inicial
  (`Cenario`) com o véu `Palette.VEIL`; a tela inicial dispensa o véu
  (`veuSobreAIlustracao()`). Dentro da corrida, o jogo congelado com o véu.
- **Pilha e voltar.** `TelaDeMenu.abrir(tela)` empilha: fora da corrida com
  `Engine.heapActivity`; dentro dela como sobreposição (`Engine.pause`), guardando
  a tela de baixo. `voltar()` desempilha pelo mesmo caminho, e Esc e B chamam
  `voltar()` em toda tela. `voltarAoMenuPrincipal()` desempilha até a tela
  inicial: "Main Menu" sempre volta ao título, e não a Saves.
- **Um painel de Settings só, que também é a pausa.** Ver
  [Settings e pausa](#settings-e-pausa-configuracoes) logo abaixo.
- **Com um menu aberto sobre a corrida, o HUD some** (`Game.render`): fica o
  mundo congelado sob o véu, sem o relógio e a hotbar disputando com o painel.
- **Transição única.** Toda tela entra em `TRANSICAO = 9` ticks (0,15 s):
  opacidade de 0 a 1 e o conteúdo subindo `SUBIDA = 6` pixels, com ease-out.
  O valor segue a Nielsen Norman (0,1 s é instantâneo, 0,4 s já é espera).
- **A pausa congela de verdade.** O `Engine` não roda o tick de quem está por
  baixo. As animações do corpo, o tremor da câmera, a aura das elites e o pulso
  da Fúria passaram a usar `Game.tempoVisual()`, que só anda com o jogo rodando.
  A música é abafada a 35% (`Sound.ABAFADO_NA_PAUSA`) e volta ao sair.
- **Mouse, teclado e controle.** O foco anda por setas e D-pad; Enter e A
  confirmam; Esc e B voltam. Um mouse parado não rouba o foco do teclado
  (`Entrada.porMouse()`).

Para criar uma tela: estender `TelaDeMenu`, passar o título ao `super`,
posicionar os elementos em `posicionar()` a partir de `areaDoConteudo()`, tratar
a entrada em `atualizar()`, desenhar em `desenharConteudo()` e devolver as dicas
em `dicas()` (`Rodape.SELECIONAR`, `VOLTAR`, `ABAS`, `AJUSTAR` ou uma
`Rodape.Dica` própria). Abrir com `TelaDeMenu.abrir(new MinhaTela())`.

### Settings e pausa (`Configuracoes`)

Um componente só, `menus/Configuracoes.java`, com duas portas:

| Aberto por | Título | Abas | Botão fora do painel | Esc / B |
|---|---|---|---|---|
| Tela inicial (`new Configuracoes()`) | Settings | Video, Audio, Interface | Back | volta ao título |
| Esc na corrida (`Configuracoes.pausa()`) | Paused | **Run**, Video, Audio, Interface | Resume | retoma o jogo |

- **Aba Run:** Continue, Restart, Main Menu e Quit como linhas de ação
  (`OptionRow.acao`), um resumo curto (tempo, nível, elemento) e o aviso de que
  sair grava a corrida. Restart, Main Menu e Quit pedem confirmação no diálogo
  comum (`Confirm`). O foco abre em Continue. O detalhe da corrida fica na tela
  de Status (tecla E), e não aqui.
- **Abas de configuração:** as mesmas linhas nos dois acessos (`OptionRow`).
  Valor numérico (slider e stepper) é uma barra arrastável no estilo do Forza
  Horizon: trilha fina, trecho escolhido aceso, marcador quadrado e o número à
  direita; arrasta com o mouse ou anda com Left/Right (o teclado e o D-pad
  repetem ao segurar). O trecho além do que cabe na tela fica pontilhado, com um
  traço no teto. Escolha entre opções (ciclo `−/+`) e sim/não (toggle) continuam
  com botões. Resolução e tela cheia esperam o
  Apply, que só redimensiona a janela (ver [Janela, resolução e DPI](#janela-resolução-e-dpi)); o resto se aplica na hora.
- **Rodapé por aba:** na Run, `Enter Select`; nas de configuração,
  `Left/Right Adjust`. As duas mostram `Q/E Tabs` e o `Esc` (Back ou Resume).
- **Por que assim:** o Forza Horizon e o RDR2 juntam partida e configurações em
  abas de um mesmo menu, trocadas por LB/RB; o Hades e o Dead Cells abrem a pausa
  num painel por cima da cena; o Minecraft usa uma tela de opções só, que sabe de
  onde foi aberta. A Run vem primeiro e com foco em Continue porque retomar é o
  que se faz mais vezes numa pausa (Game Accessibility Guidelines: pausar a
  qualquer momento e voltar sem passos extras).
- **Q/E para as abas, e o conflito com o E do Status:** ficaram. Q/E é o par de
  abas mais comum em jogos de PC e espelha LB/RB no controle, que é o que o
  `Gamepad` já traduz. O conflito não acontece: com a pausa aberta o `Game` não
  recebe ticks, então o E só troca de aba; e o tick que abre a pausa termina ali,
  para um E apertado junto com o Esc não abrir o Status por baixo.

Para reaproveitar: uma tela nova de configurações é uma lista de `OptionRow`
dentro de uma aba de `Configuracoes.montarLinhas`; uma ação nova da corrida é
mais uma `OptionRow.acao` em `linhasDaCorrida`.

### Hover e foco (`Realce`)

O padrão é o dos botões da tela inicial: o elemento **cresce a partir do
centro, ganha um brilho suave por trás e clareia**. Nenhum hover usa contorno.

- `Realce.Estado`: a rampa de 0 a 1, com `tick(aceso)` e `noQuadro()`.
- `crescer`, `brilho`, `clarear` e `realcar` (crescer mais brilho) desenham o
  hover.
- `marcadorDeFoco` desenha o foco do teclado e do controle: o mesmo realce mais
  duas setas que respiram dos lados. A Xbox Accessibility Guideline 113 pede foco
  sempre visível e avisa que só um brilho sutil não basta.
- `folgaEntreVizinhos(s)` é o vão mínimo entre dois botões lado a lado, para as
  setas de um não encostarem no outro.

Usam o `Realce`: `Button`, as casas do inventário e da loja (`Slot`), o botão de
comprar, as abas do inventário, as linhas de opção, os espaços de save, os gatos
da seleção, o troféu, a loja do fim de corrida e os botões Reroll e Skip.

### Cartas (`MaoDeCartas`)

Uma mão de cartas para escolher uma. O padrão é a carta de buff do subir de
nível: 88×128 pixels nativos, faixa colorida com o rótulo, medalhão claro com o
ícone, nome em fonte Game, efeito na cor da carta e a tecla de atalho no pé.
O componente traz o comportamento junto: entrada virando uma carta por vez,
carta sob o mouse ou o foco com o realce comum (cresce, brilho suave na cor dela,
clareia; setas no foco do teclado), teclas 1 a 3, setas e
Enter, clarão na escolhida e fechamento.

**Cores:** a moldura, o corpo, o texto da faixa, o medalhão e a caixa da tecla vêm
da `Palette` (`DARKEST`, `DEEP`, `ACCENT`). Só a cor de cada carta é própria,
porque diz o que ela é: raridade, passiva, arma ou elemento. A tela de Level Up
usa o véu `Palette.VEIL` e o título em `Palette.TEXT` com halo `Palette.ACCENT`.
Antes título e halo eram um dourado alaranjado solto no código.

Cada carta só varia no conteúdo:
`new MaoDeCartas.Carta(rotulo, cor, icone, nome, efeito)`. O `icone` é quem
desenha o medalhão: `SubirDeNivel` usa `Icones.desenhar`, e
`EscolhaDeElemento` usa `Relevo.desenhar` com o selo do elemento. O efeito
aceita várias linhas separadas por `\n`.

Uso: criar com a lista de cartas; a cada tick chamar `posicionar(y, s)` e
`tick()`, que devolve o índice escolhido naquele tick (ou -1); fechar a tela
quando `fechou()` for verdadeiro. `pronta()` diz se a mão já aceita entrada, para
os controles em volta dela (Reroll, Skip).

**Variante de destaque**, para escolhas raras, sem um segundo componente de
carta. Os padrões são os das cartas de buff:

- `comEntrada(atraso, virada, subida)`: ritmo de entrada próprio;
  `pularEntrada()` termina a entrada sem escolher nada.
- `comEnfeite(Enfeite)`: desenha por trás da carta (aura, partículas) e por cima
  (moldura especial), já com a entrada, o realce e o voo aplicados.
- `comSons(aoFocar, aoEscolher)`: o som do foco e o da escolha, por carta.
- `comFechamento(ticks, destino, inicioDoVoo)`: a escolhida voa até um ponto.
- `comVao(vao)`: mais espaço entre as cartas, para um enfeite largo.

### Escolha de elemento: a cerimônia (`EscolhaDeElemento`)

Acontece **uma vez por corrida**, aos 45 s, e decide o elemento da corrida
inteira; o level up acontece dezenas de vezes e muda um número. Por isso ela tem
o que o level up não tem, e a hierarquia se lê antes do texto:

| Etapa | Duração | O que acontece |
|---|---|---|
| Antecipação | 42 ticks (0,7 s) | O jogo congela e o HUD some. Um véu e uma vinheta fecham a tela, um clarão e dois anéis saem do gato, e o título (maior que o do level up) cresce com um halo nas cores dos três elementos oferecidos. Toca o stinger, e a trilha da arena desce enquanto a da escolha sobe. |
| Revelação | ~36 ticks | As cartas sobem desde baixo e viram, uma a cada 9 ticks (as de buff: 5). |
| Escolha | livre | Cartas em destaque: moldura dupla na cor do elemento, pedras nos cantos, brilho diagonal, aura que respira e partículas no jeito de cada elemento. O foco usa o `Realce` e, por cima dele, a aura cresce, as partículas triplicam e toca o som do elemento. |
| Confirmação | 36 ticks (0,6 s) | Toca a confirmação do elemento. Clarão na cor dele, explosão de partículas, as outras cartas somem, e a escolhida voa até o gato com rastro. Na chegada, a arma se transmuta. A trilha da escolha desce e a da arena volta. |

- **Pular:** Enter, Espaço, Esc, clique (esquerdo ou direito), A ou B pulam a
  antecipação e a revelação direto para as cartas. A tecla é consumida e não
  escolhe nada.
- **Referências:** a cor de borda por raridade das bênçãos do Hades (a hierarquia
  lida antes do nome); a cerimônia com música própria e pulável do baú de evolução
  do Vampire Survivors; "Juice it or lose it" (Jonasson e Purho, GDC 2012) para o
  impacto curto e forte da confirmação, com a volta rápida ao jogo.
- **Ajuste fino:** tudo no topo de `EscolhaDeElemento` (ver `docs/CONTEXTO.md` §8).

### Status do gato (`TelaDeStatus`) e o inventário desativado

O **E** (Y no controle) abre `menus/TelaDeStatus.java`, uma `TelaDeMenu` que
**pausa a corrida**, como as telas de pausa do Vampire Survivors e do Brotato.
Ler uma ficha com a horda crescendo seria punir quem quer entender a build. Três
colunas:

| Coluna | Conteúdo |
|---|---|
| Cat | vida, dano, golpes por segundo, velocidade, alcance, redução de dano, sorte, crítico, área, regeneração, ganho de XP, alcance do ímã |
| Weapons + Run | arma da mão, elemento com dano e cadência, armas extras com nível ou evolução; nível e XP, abates, ouro, trocas e pulos de carta |
| Buffs | passivas com nível e efeito acumulado, cartas de atributo e melhorias da roleta com quantidade e total |

Os valores são os **finais**, lidos dos mesmos getters que o gameplay usa
(`Entity.getDamage`, `Bonus.critico`, `Melhorias.somaDosPontos`...). O bônus de
cartas e upgrades aparece entre parênteses ("9 (+2)"). As linhas recebem foco
pelas setas, pelo D-pad e pelo mouse, e uma faixa no pé do painel explica a linha
focada. E, Esc e B fecham.

**O inventário está desativado, não removido.** A tela de inventário e a ficha
antiga de sprite (`interfaces/shared/Status`) deixaram de ser abas da `Inter`
(ver `Game`). Os dados do `Inventory` continuam em uso: a arma da mão, a hotbar
e a loja do vendedor. Para religar a tela, basta registrar "inventory" de novo na
`Inter`.

### Portal de nascimento (`Chegada.Tom`)

O portal é o mesmo desenho (`portalCistern.png`) para todos, tingido em tempo de
carga a partir de uma rampa de três cores pela luminosidade de cada pixel:

- **`Tom.PERIGO` (inimigos):** o carmim das bandeiras da arte, a única família
  quente da ilustração e a cor de aviso da interface (`Palette.WARN`), levado ao
  claro para ler sobre a pedra e o mato. Por baixo, um brilho pulsante na mesma
  cor. O verde de antes era a única cor da cena que não vinha da arte.
- **`Tom.CHEGADA` (gato e vendedor):** a luz da lua da paleta
  (`DEEP → LIGHT → TEXT`), sem brilho: chegada sem ameaça.

A rampa de cada tom, `ALFA_DO_BRILHO`, `ALCANCE_DO_BRILHO`, `PULSO` e `TAMANHO`
ficam em `Chegada`.

### Transmutação da arma (`Transmutacao`)

Ao escolher o elemento, a arma da mão vira a variante elemental em **18 ticks
(0,3 s)**, sem bloquear nada:

1. **Estourar (6 ticks):** a arma pula um pouco da mão e branqueia. No pico ela
   troca, escondida no branco; nesse quadro tocam o som do elemento, um anel abre
   na cor do elemento (`OndaDeChoque`) e o nome da arma nova sobe sobre o gato
   nessa cor (`Word`).
2. **Assentar (12 ticks):** a arma nova volta para a mão, maior do que é, e
   encolhe até o tamanho dela com um passo além, perdendo o branco.

A arma anima a partir da pose da mão (`Item.renderNaMao`; o `Bow` o sobrescreve
para manter a mira), então o primeiro e o último quadro são a arma em mãos, sem
salto. O gato anda, ataca e usa habilidades o tempo todo. Antes eram 136 ticks
(2,3 s) com a arma parada acima da cabeça. Ela não toca mais o arpejo de
conquista: o som de recompensa é a confirmação do elemento, na escolha.

## Persistência

Tudo é JSON, gravado ao lado do executável (no `run`, a raiz do projeto):

| Arquivo | Dono | O que guarda |
|---|---|---|
| `config.json` | `Configs` | opções; gravação atômica com `.tmp` e `.bak` |
| `saves/corrida-N.json` (N = 1 a 3) | `Corrida` | a corrida interrompida. O gato: nível, vida, XP, inventário, melhorias, passivas, armas extras, evoluções, elemento. O andamento: relógio, abates, próximo chefe, Rato Rei, cerco, chefes vivos (tipo e fração de vida), níveis pendentes, trocas e pulos, carga da Fúria, números da tela final, vitória |
| `saves/meta.json` | `Meta` | ouro e níveis dos upgrades permanentes |
| `saves/conquistas.json` | `Conquistas` | o progresso das conquistas |

**Permadeath.** Cada partida grava sempre no mesmo espaço, escolhido no início;
ler um save o apaga, e o jogo regrava no primeiro tick; morrer apaga. Assim não
há como guardar uma cópia antes do perigo. O espaço 0 é o da bancada e nunca
grava.

**Quando grava**, sempre só no campo (a antecâmara não tem save — a corrida ainda
não começou):

1. ao entrar no campo (`Game.changeMap`);
2. depois de cada escolha que não pode ser re-sorteada: elemento, carta de nível,
   prêmio do caça-níquel — gravar antes faria do save uma máquina de re-rolar;
3. a cada minuto do relógio (`Arena.gravarACadaMinuto`), contra fechamento à força;
4. ao sair pela pausa ("Main Menu" ou "Quit").

**O que não grava:** inimigos comuns, projéteis, partículas e o campo. A horda é
função do relógio e se refaz em segundos; o campo é sorteado de novo. Os chefes
vivos são a exceção — sem eles, fechar o jogo pularia o chefe.

**Ordem da retomada** (`Game.retomar`): bônus de início (gato e upgrades
permanentes) → o gato do save → o campo → os reinícios de corrida → o andamento
do save → no primeiro tick, os chefes e a regravação. Os bônus vêm antes porque
o upgrade de vida enche a barra; a vida gravada tem de ser a última palavra.

`Meta` e `Conquistas` ficam fora dos espaços: apagar uma corrida não apaga o que
foi conquistado. No modo bancada (`-Dcd.bench`) nenhum dos dois grava.

## Controle

O controle de Xbox é lido pela **XInput**, a API do próprio Windows, chamada
pelo JNA que já vinha com o OSHI. Não entrou dependência nova. Fora do Windows
a DLL não carrega e o controle fica desligado sem erro.

Nenhuma tela conhece o controle. `Gamepad` traduz cada leitura em **teclas
virtuais** (`KeyBoard.pad`), as mesmas que o jogo já lê por nome. O estado do
controle fica num campo separado do teclado, para que soltar um não solte o
outro.

As telas de grade — inventário, loja do vendedor, máquina de chiclete, loja do
fim de corrida — andam por **foco** com as setas (`Grade`), então servem ao
teclado e ao controle do mesmo jeito.

**Dois modos de entrada** (`Entrada`): mexer o mouse põe o jogo no modo mouse e
teclado; apertar qualquer coisa no controle põe no modo controle. No modo
controle o `Mouse` responde que o ponteiro está fora da tela e o ponteiro do
sistema some. Sem isso, um mouse parado em cima de um botão ou de uma carta
tomava de volta, a cada tick, o foco que o D-pad tinha acabado de mudar. Nas
grades, o realce do foco só aparece quando as setas ou o controle estão no
comando.

O **analógico direito é olhar**: em jogo, a `Camera` se afasta até 1,5 tile do gato
na direção dele e volta sozinha ao soltar. Não há cursor virtual.

Duas regras pesam no desempenho e no comportamento:

- **Porta vazia é consultada uma vez por segundo.** `XInputGetState` numa porta
  sem controle é lento, e consultar as quatro a cada tick faria o jogo engasgar
  em quem nem tem controle.
- **O B depende de onde o jogador está.** Com a partida rolando, ele interage
  com o objeto mais próximo: `GameMap.interagir` escolhe o alvo, e o
  `clickOnRect` desse objeto responde como se ele tivesse sido clicado. Nas
  telas, o B volta. O papel é decidido no aperto e fica até soltar, para o B
  que abre a loja não fechá-la no tick seguinte.

## Recursos

Sprites são PNG em pixel art de 16×16, ampliados por fator **inteiro** com
nearest-neighbor. Fator fracionário desalinha e borra a arte, por isso toda
escala no projeto é inteira.

Os mapas são PNG em que **a cor de cada pixel identifica um tile**, com um JSON
irmão descrevendo as entidades. Ver `TileIDs` e `GameMap.convertMap`. O campo
da corrida sem fim não é um arquivo: o `Gerador` pinta a mesma imagem em
memória, com a mesma tabela de cores, e o resto do carregamento é igual. Hoje
o único mapa desenhado em uso é a antecâmara (`beginning`).

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

A exceção é `escolha_elemento.wav`, a trilha da escolha de elemento. Ela é gerada
por código (`tools/GenSonsDaEscolha.java`), e o gerador não tem codificador
Vorbis. Uma música cujo nome em `Musics` já traz a extensão é carregada como está.

**Desvanecimento** (`Sound.desvanecer`): leva uma faixa a um volume em N ticks,
numa rampa em cosseno, e pode pará-la no fim. Quem anda a rampa é o
`Engine.tick`, e não a tela que pediu. Por isso a volta da trilha da arena termina
mesmo depois que a escolha de elemento fechou. Um `Sound.volume` pedido no meio
de uma rampa muda o destino dela, em vez de pular o volume.

**Sons da escolha de elemento** (gerados, ver `tools/GenSonsDaEscolha.java`):
`elemento_revelacao` (stinger da entrada), `escolha_elemento` (trilha em loop de
16 s) e `elemento_fogo/gelo/agua/terra/ar` (confirmação de cada elemento). No
foco de cada carta toca o `hit_*` do elemento, o mesmo do golpe.

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

Registrados para não se perderem. Revistos em 2026-10-21.

**Resolvidos em 2026-10-21**, mantidos aqui como registro:

- O `Repulsion` rodava numa thread paralela, mexendo em posições enquanto o laço
  principal lia e desenhava — e a thread nunca terminava: o `dispose` baixava uma
  flag, mas ela dormia num `wait` que ninguém mais acordava, e cada troca de mapa
  deixava uma presa para trás. Agora roda no `Game.tick`.
- `resetWindow()` (F11, troca de resolução) recriava a janela na thread do
  `Engine`, fora da EDT. Passou pela EDT com `invokeAndWait` e, em 2026-10-22,
  deixou de existir: `Window.aplicarVideo` muda a mesma janela, sem recriá-la.
- O `Waves` abria uma thread por leva de inimigos no modo por ondas; o modo saiu.
- O `Game.tick` contava um segundo a cada 61 ticks.
- O save da corrida sem fim não guardava o relógio, e a retomada com o upgrade
  Vitality curava o gato. Ver [Persistência](#persistência).

**Em aberto**

- **Retomar limpa a tela.** Como os inimigos comuns não são gravados, quem sai no
  meio de um cerco volta com a tela vazia. A horda se refaz em segundos, e o aviso
  da pausa diz isso ("Enemies on screen are not kept"), mas é uma brecha
  consciente.
- **Dinheiro em espera.** Não há fonte de moeda dentro da corrida: o gato começa
  com 100 e só a loja da antecâmara as consome. `Seller`, `Store`, `Coin` e
  `GumMachine` (com `GumInterface` e `GumPanel`) ficam no código, marcados como
  em espera, até o dinheiro ganhar papel na corrida.
- `tools/GenCisterna.java` ainda escreve as plantas `cistern_1` a `6`, que o jogo
  não lê mais; os tiles que ele gera continuam em uso pelo `Gerador`.
