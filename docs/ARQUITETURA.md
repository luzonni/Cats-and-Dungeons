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
| `com.retronova.game` | A partida (`Game`), o save (`Corrida`), a progressão por gato (`save/Progresso`, `espelho/Espelho`), as conquistas e as estatísticas da corrida |
| `game.map` | Mapa base, câmera, luz e sombra (`Atmosfera`) |
| `game.map.arena` | Arena, horda (`Waves`), campo gerado (`Gerador`), cartas de nível, abertura do baú, escolha de elemento, música da arena |
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
  cartas, baú), a interpolação é desligada, senão o mundo parado tremeria.
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

- **Ativação ao iniciar** (`Window.destacarAoIniciar`, `FocoDoWindows`): uma única
  chamada na EDT pede `SetForegroundWindow` e depois o teclado para o Canvas.
  O antigo `alwaysOnTop` temporário elevava o frame sem garantir a ativação: a
  barra só sumia depois do clique. O foco informado pelo AWT também não bastava:
  foi observado `AWT=true` com `GetForegroundWindow` apontando para outro app.
  Se o pedido normal for recusado, envia uma passagem de Alt pressionado/solto
  e repete o pedido, como `force_window_active` da biblioteca Tao. Não faz esse
  desbloqueio com Alt/Ctrl/Shift/Windows já pressionado. A tentativa ocorre uma
  única vez; perder foco depois não a repete. O modo foto fica de fora, inclusive
  com janela real. `-Dcd.video=true` diferencia foco AWT, primeiro plano nativo e
  resultado do pedido de ativação. O sistema ainda pode recusar a operação.
  Referências: [Tao](https://github.com/tauri-apps/tao/blob/dev/src/platform_impl/windows/window.rs),
  [SetForegroundWindow](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setforegroundwindow)
  e [LockSetForegroundWindow](https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-locksetforegroundwindow).

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
- **Barra de tarefas em tela cheia:** além de desligar o redimensionamento, o
  Windows recebe uma marca explícita via `TelaCheiaDoWindows` e
  `ITaskbarList2.MarkFullscreenWindow` (JNA/COM já disponível no projeto).
  Só mudar `setResizable(false)` não resolveu no uso real: a detecção automática
  do Shell ainda deixava a barra por cima. A marca é aplicada depois de mostrar
  a janela, ao ativá-la e ao aplicar vídeo; ela se perde quando o frame é
  escondido. Em modo janela a marca é removida. A barra continua disponível
  nas outras aplicações, sem mudar sua configuração nem a resolução do monitor.
  Referência: [Microsoft Learn](https://learn.microsoft.com/en-us/windows/win32/api/shobjidl_core/nf-shobjidl_core-itaskbarlist2-markfullscreenwindow).
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
- **Padrão pela resolução, e passos inteiros a partir dele:**
  - HUD padrão = `arredonda(fator)`, no teto do layout (abaixo);
  - menus padrão = `arredonda(fator × 1,125)`: 2 em 720p e 3 em 1080p, o tamanho
    escolhido nos testes (com 1,5, a primeira versão, o padrão era grande demais).
    O teto é a tela comportar 384×224 pixels de arte, o que o painel de Settings
    ocupa com título, abas, Resume e rodapé;
  - escala = padrão + passo, entre o mínimo legível e o teto (`EscalaDaInterface.Faixa`);
  - margem = `arredonda(fator × margem de referência)`.
- **Escala inteira.** Fator quebrado borra a pixel art (suavizada) ou deixa pixels
  de larguras diferentes (vizinho mais próximo); é o modelo do Minecraft (GUI Scale
  1 a 4 e Auto). As resoluções 16:9 comuns são múltiplas de 640×360 (720p = 2,
  1080p = 3, 1440p = 4, 4K = 6), e entre elas o padrão ocupa **exatamente a mesma
  fração** da tela. Fora delas (1366×768, 1600×900), o erro é o de arredondar
  para o pixel inteiro.
- **Tamanho mínimo legível:** a partir de 1280×720 nenhuma camada desce de 2, porque
  as fontes de pixel art têm de 5 a 8 pixels e, em escala 1, ficam ilegíveis.
- **Preferências (Settings → Interface):** Menu size e HUD size em passos
  (Smaller, Small, Default, Large, Larger, Largest), um bloco por tamanho que a
  tela atual comporta; Margin de 0 a 16 pixels de referência (padrão 7) e Reset
  interface. Passo gravado que a tela não comporta (Largest de 4K aberto em 720p)
  vale o maior que cabe, sem perder a escolha.
- **Por que passos, e não por cento:** a primeira versão tinha Interface, Menu e
  HUD size de 50% a 200%, multiplicados. Com escala inteira, em 1080p os menus têm
  uns quatro tamanhos reais e a barra oferecia 31 posições: a maioria não mudava
  nada, o "200%" aparecia cheio com a escala presa no teto, e acima de 100% não
  fazia sentido com um padrão que já vem da resolução. Os valores antigos (por
  cento, GUI size, HUD size e Margin em pixels) migram sozinhos.

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
cobre a matriz inteira: resoluções, passos e margens.

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
| `AberturaDoBau` (regra em `RecompensaDoBau`) | ao abrir um baú (elite ou sorte) | arma extra nova ou nível dela, carta de `Melhoria`, ou a evolução pronta |
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

### Abertura do jogo (`Abertura`)

Ao abrir o jogo, a janela aparece preta (`Window` pinta o fundo de preto antes do
primeiro quadro, sem o clarão do fundo padrão) e `menus/Abertura.java` roda por
cima da tela inicial, que já espera embaixo na pilha:

| Fase | Completa | Curta | O que acontece |
|---|---|---|---|
| Preto | 0,5 s | 0,2 s | o ar da masmorra entra (`abertura_vento`) |
| Crédito | 2,7 s | — | "a game by" e o nome da equipe sobem do escuro e somem |
| Logo | 2,6 s | 1,5 s | a lua acende atrás; a logo surge acinzentada e uma frente de luz a deixa branca |
| Cena | 1,6 s | 1,0 s | a ilustração surge em volta da logo; música e ambiência da tela inicial sobem |
| Menu | ~0,8 s | ~0,8 s | botões, troféu e versão surgem um a um (`Menu.entrarEmSequencia`) |

- **A logo vira a tela inicial.** `tools/GenLogo.java` extrai o letreiro da própria
  ilustração, e a abertura o desenha exatamente onde ele está na arte. Na fase da
  cena, as letras pintadas aparecem por baixo das da abertura, que somem sem pular.
- **Crédito antes da logo:** é a convenção de apresentar quem fez antes do título
  (como o "EXOK presents" antes do Celeste). É também a única ordem em que a logo
  passa direto para a tela inicial.
- **Completa a cada início** (7,4 s), mesmo se a abertura já foi vista. O antigo
  `introVista` do `config.json` é ignorado; a curta só é usada com `-Dcd.intro=curta`.
- **Pulo bloqueado por padrão:** Settings → Interface → **Allow intro skip**
  libera o pulo por tecla, clique ou botão do controle (`KeyBoard.apertos`).
  A preferência `permitirPularAbertura` é persistida em `config.json`; quando
  ausente, vale `false`, inclusive para configurações antigas. Ligada, permite
  ir à tela inicial já pronta, com a música entrando; não pula automaticamente.
- **Aproveita o carregamento:** as músicas carregam por trás (ver Áudio), e a
  ilustração da tela inicial é escalada numa thread (`Cenario.prepararEmSegundoPlano`).
- **Sem clarão:** só desvanecimentos e a passagem de luz, de baixo contraste, longe
  do limite de três flashes por segundo da WCAG 2.3.1.
- **O crédito da equipe** fica em `Abertura.CREDITO_ACIMA` e `CREDITO_EQUIPE`: "a game by"
  e "Runforce". Com `CREDITO_EQUIPE` vazio, a fase do crédito não
  acontece.
- **Ajuste fino:** tudo no topo de `Abertura` e de `Menu` (ver `docs/CONTEXTO.md` §8).
  A bancada e o modo foto pulam a abertura; `-Dcd.intro=completa|curta` a força.

**Tempo de abertura**, medido do `main` ao primeiro quadro: 5,0 s antes, sem nada na
tela até lá; 1,2 s agora, já na abertura. A tela inicial fica pronta, pela soma das fases, em
uns 8,6 s na versão completa (7,4 s de abertura; a primeira versão, de 4,4 s, passava
rápido demais para ler o crédito) e uns 3,9 s na curta (2,7 s).

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
  Valor contínuo (volume, zoom, margem) é uma barra arrastável no idioma dos
  toggles: contorno de um pixel, miolo fundo, trecho escolhido cheio e o número
  à direita. Tamanho (Menu size, HUD size) é uma fila de blocos, um por tamanho
  que a tela comporta, com o nome à direita (`OptionRow.tamanho`). Os dois
  respondem ao clique, ao arrasto e a Left/Right (o teclado e o D-pad repetem ao
  segurar). O arrasto mede contra a barra como estava ao começar, e o bloco de
  tamanho se aplica ao soltar: aplicar no meio redimensionava a linha debaixo do
  mouse, que escolhia outro valor, e a tela piscava. Escolha entre opções (ciclo
  `−/+`) e sim/não (toggle) continuam com botões. Resolução e tela cheia esperam o
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
desenha o medalhão: `SubirDeNivel` e `EscolhaDeElemento` desenham o ícone do
registro (`Icone.desenharNoCentro`, ver "Ícones" abaixo). O efeito aceita várias
linhas separadas por `\n`.

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

### Baú: a abertura (`AberturaDoBau`) e a recompensa (`RecompensaDoBau`)

O baú cai de elite (sempre) e de bicho comum conforme a sorte
(`Player.chanceDeBau`). Encostar nele abre `game/map/arena/AberturaDoBau.java`, que
**pausa a corrida**, como a roleta pausava.

**A regra e a tela são separadas.** `game/items/RecompensaDoBau.java` é só a regra:

1. evolução pronta (arma no máximo + passiva parceira) sai garantida;
2. senão, arma extra em 40% das vezes (55% sem nenhuma), no próximo nível;
3. senão, uma `Melhoria` com o grau sorteado à parte (`Melhorias.oferecer`),
   promovido pela sorte de `Modifiers.Fortune`.

`abrir` decide o prêmio e conta o baú nas estatísticas, `entregar` aplica e grava a
corrida, e `possiveis` lista o pool para a prévia. A tela só desenha o que já saiu:
trocá-la de novo não mexe no que o jogador recebe (`RecompensaDoBau` tem testes).

**O fluxo**, em ticks de 60 Hz desenhados com `Engine.alpha()` (igual a 60 e a 240+ FPS):

| Fase | Ticks | O que acontece |
|---|---|---|
| Entrada | `ENTRADA = 12` | o véu sobe, o baú cai no meio e toca o som de chegada |
| Espera | até o clique | o baú respira; embaixo, a prévia do que pode sair passa ícone a ícone, cada um com a moldura e o tic do seu grau |
| Tremor | `TREMOR = 18` | o baú chacoalha e acende na cor do grau do prêmio |
| Tampa | `TAMPA = 9` | três quadros da tampa, clarão curto e faíscas |
| Subida | `SUBIDA = 16` | o cartão do prêmio sobe de dentro e assenta |
| Pronto | `ATE_COLETAR = 8` | o clique coleta, entrega e devolve o jogo |

Clique, Enter, Espaço ou A abrem e coletam; um clique no tremor, na tampa ou na
subida pula para o cartão, sem pular o som nem a recompensa.

**Duração medida no modo foto**, da tela aberta até liberar a coleta:

| | Sem pular | Pulando |
|---|---|---|
| Roleta (antes) | 3,28 s | 1,18 s |
| Baú | 1,28 s (clique de abrir aos 0,25 s; 0,85 s depois dele) | 0,65 s |

**Por que um baú:** a roleta decidia o prêmio antes de girar e os três rolos sempre
paravam nele, uma máquina de sorte que nunca errava. O baú conta a mesma coisa sem
fingir sorte. Referências:
- a cor do baú do Enter the Gungeon, que diz a qualidade antes de abrir (aqui, o
  brilho do tremor);
- a reclamação contra a animação longa do baú do Vampire Survivors, que ganhou
  atalho para pular e versão curta;
- "Juice it or lose it" (GDC 2012) e "The art of screenshake" para o tremor curto,
  as faíscas e o som em cada passo.

**Arte e som:**
- `bau_abrindo.png` sai de `tools/GenBau.java`: os quadros CC0 do 0x72, remapeados
  para a paleta do baú do chão;
- os sons `bau_*.wav` saem de `tools/GenSonsDoBau.java`: chegada, tremor e tampa,
  mais uma revelação e um tic de prévia por grau (comum, raro, épico, lendário), em
  escada como o brilho;
- na prévia, cada melhoria ganha um grau sorteado com as chances de verdade
  (`Raridade.sortear`), então a fileira mostra o quanto cada grau é comum. É só
  amostra: o prêmio já foi decidido ao abrir;
- arma nova soa épica, nível de arma soa raro e evolução soa lendária
  (`Premio.grau`).

**A roleta foi desligada, não removida:** `Roleta` está `@Deprecated` e sem nenhuma
porta de entrada; `-Dcd.roleta` abre o baú.

### Status do gato (`TelaDeStatus`) e o inventário desativado

O **E** (Y no controle) abre `menus/TelaDeStatus.java`, uma `TelaDeMenu` que
**pausa a corrida**, como as telas de pausa do Vampire Survivors e do Brotato.
Ler uma ficha com a horda crescendo seria punir quem quer entender a build. Três
colunas:

| Coluna | Conteúdo |
|---|---|
| Cat | vida, dano, golpes por segundo, velocidade, alcance, redução de dano, sorte, crítico, área, regeneração, ganho de XP, alcance do ímã |
| Weapons + Run | arma da mão, elemento com dano e cadência, armas extras com nível ou evolução; nível e XP, abates, ouro, trocas e pulos de carta |
| Buffs | passivas com nível e efeito acumulado, cartas de atributo e melhorias do baú com quantidade e total |

Os valores são os **finais**, lidos dos mesmos getters que o gameplay usa
(`Entity.getDamage`, `Bonus.critico`, `Melhorias.somaDosPontos`...). O bônus de
cartas e upgrades aparece entre parênteses ("9 (+2)"). As linhas recebem foco
pelas setas, pelo D-pad e pelo mouse, e uma faixa no pé do painel explica a linha
focada. E, Esc e B fecham.

**Cada coluna rola.** A coluna dos buffs passa do que cabe no meio da corrida (14
linhas em 720p), e antes o resto virava um "+N more" que não se abria. Agora o
foco das setas e do D-pad leva a rolagem junto, a roda do mouse rola a coluna sob
o cursor, e os avisos "N more above / below" dizem o que está fora da vista. Para
conferir com a coluna cheia: `-Dcd.bench=true -Dcd.buffs=true`.

**O inventário está desativado, não removido.** A tela de inventário e a ficha
antiga de sprite (`interfaces/shared/Status`) deixaram de ser abas da `Inter`
(ver `Game`). Os dados do `Inventory` continuam em uso: a arma da mão, a hotbar
e a loja do vendedor. Para religar a tela, basta registrar "inventory" de novo na
`Inter`.

### Morte e apresentação de Run Over

`Player.die()` é idempotente: corta os efeitos de combate, abaixa a música,
solta uma voz de morte e aplica um impacto de câmera próprio (`Camera.impactarNaMorte`) e vibração.
O som não usa o intervalo dos gemidos de dano: o golpe fatal já o gastava e
calava a morte. `death_muffin`, `death_azrael` e `death_finn` vêm dos próprios
miados, com tom descendente e impacto grave, gerados por `GenSonsDaMorte`.
Duram de 670 a 1175 ms, a -20 dB RMS; `run_over` dura 320 ms, a -24 dB RMS.

A sequência dura 138 ticks: impacto/parada 18, tombo 72 com curva suave,
desaparecimento 48. A câmera fecha até 1,6×. `ImpactoDaMorte` dura 24 ticks (0,4 s), com amplitude
de oito pixels nativos da UI e envelope quadrático decrescente. O deslocamento
entra na transformação antes do zoom: um ponto do cenário se move pela mesma
distância na tela a 50% ou 200%. Substitui o tremor genérico, que era reduzido
pelo zoom. A música permanece abafada durante a queda, em vez de voltar ao
volume normal no tick seguinte.
`EspiritoDoGato` desenha a chama e cinco fragmentos em pixels da família ACO,
subindo 12 pixels nativos durante a segunda metade da queda e o apagamento.
O véu chega a preto no fim, emendando no fundo preto da tela de resultados.

`GameOver` usa `ApresentacaoDoFim`, relógio próprio de 60 ticks, sem mexer nos
valores de `Estatisticas` ou `Progresso`: título em 18 ticks; resumo começa em 12 e
colunas de detalhe em 20, cada painel leva 22; barras crescem entre 24 e 54; brilho único do
ouro e botões entre 42 e 60. O deslocamento é de oito pixels nativos. O som do
título toca uma vez, apenas na derrota. Conquistas aparecem depois da entrada.
`TelaDeMenu.desenharCabecalho` mantém o título padrão e permite esse gesto.
O título é rasterizado opaco uma vez por escala; a imagem recebe o movimento e
a transparência. Camadas ainda invisíveis não são desenhadas.

Durante a entrada, a loja e os botões não recebem comandos. Após 18 ticks,
clique, Enter/A ou Esc/B concluem a apresentação; o comando é consumido e não
compra nem troca de tela. No tick seguinte, a navegação comum volta. A morte
continua impedindo controle e dano, sem mudar o salvamento ou as recompensas.

O relatório segue a distribuição ampla da tela de resultados de Hades II:
identidade e resultado à esquerda, ficha final ao centro e dano por fonte à
direita. `LayoutDoFim` usa toda a largura disponível menos 12 pixels nativos de
margem por lado, vãos de dez e proporções 24%/38%/38%. A escala é a escolhida
nos menus, sem encolher tudo para encaixar painéis fixos. Personagem tem retrato,
nome, habilidade, tempo, abates, nível, baús, maior horda, moedas recebidas e saldo do gato.

`ResumoFinalDoGato.capturar(player)` é chamado ANTES de descartar o `Game`.
Guarda cópias imutáveis das mesmas linhas que `TelaDeStatus` calcula: 12 atributos
finais, arma de mão, elemento, armas extras/evoluções, passivas e cartas, sem
recalcular atributos na tela final nem consultar um personagem novo. Valores e
ordem do dano também são copiados ao abrir `GameOver`; nenhuma fonte é descartada.

As abas comuns (`Abas`) são `Run report` e `Final build`, acessíveis por clique,
Q/E ou LB/RB. Listas que não cabem têm rolagem; Left/Right escolhe a coluna.
A loja foi removida do fim da corrida: toda compra fica no vendedor da
antecâmara, em `TelaDoEspelho`. `Try Again` leva ao hub sem obrigar a comprar.
Geometria de `GameOver`: margem interna 12; cabeçalhos 20; atributos começam em
38 com passo 16; dano em 78 com passo 24; build em 30 com passo 32. Fontes do corpo Septem 8,
efeitos 6/7 e retrato de até 48 pixels nativos. Listas reservam espaço para seu
rodapé. O retrato se adapta à altura, mantendo legíveis os números da corrida.

### Portal de nascimento (`Chegada.Tom`)

O portal compartilha a folha `portalRunforce.png`, reproduzível com
`java tools/GenPortal.java`: quatro quadros de 16×16, aro elíptico com trechos de
luz circulando, centro escuro e fragmentos em pixels. Substitui a textura antiga
recolorida e o halo suavizado; mantém abertura, formação e fechamento nos mesmos
ticks, ancorados no centro do tile. Cada quadro dura seis ticks, nunca avança em
`render`.

`Chegada.Tom` converte os cinco níveis da folha diretamente nas cinco cores de
`Familia.ACO` (jogador e vendedor) ou `Familia.VIDA` (inimigos), com cache por
quadro. Sem gradientes intermediários ou elipses rasterizadas por portal. O
tamanho cheio continua em `Chegada.TAMANHO` (1,4 tiles).

A alavanca de entrada mantém cursor de interação e luz suave de `Familia.OURO`,
sem retângulo de seleção. O halo usa `Brilho` em cache e fase avançada no tick
(`Lever.PASSO_DO_BRILHO = 0,08`), independente da taxa de renderização.

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

### Animação das armas (`AnimacaoDaArma`)

A arma da mão deixou de ser um adesivo preso à pata. Uma camada só,
`AnimacaoDaArma`, dá o movimento que é igual para todas, e cada arma só diz
**quando** ataca e **quanto** o gesto dela mexe.

**Como roda.** Tempo fixo mais interpolação, como o resto do jogo: o estado anda
no tick (60 Hz, chamado por `Player.tickItemHand` **antes** do tick da arma) e o
desenho mistura o tick anterior com o atual por `Engine.alpha()`. O balanço fica
contínuo a 144 ou 240 Hz e não acelera com a taxa de quadros. Nada é alocado por
quadro: `calcular` escreve em campos e `Item.naMao` soma o resultado à pose de
porte (ou à pose ajustada no editor), sem substituí-la. Os deslocamentos são
arredondados ao pixel de tela.

**Os estados**, somados:

| Estado | O que faz | De onde vem o ritmo |
|---|---|---|
| Respiro (parado) | balanço de ±2,5° | ciclo de 72 ticks, o mesmo da folha parada do gato |
| Passo (andando) | balanço de ±6° e 1 px de peso no contato do pé, com um tick de atraso (ação sobreposta) | `Player.faseDaFolha` e `Player.corpoAfundado`, os quadros reais da folha |
| Troca parado ↔ andando | mistura dos dois em ~7 ticks | `TROCA_DE_ANDAR` |
| Virada | a arma se recolhe 35° e volta em 7 ticks quando o gato troca de lado | `Player.getLadoDoPasso` |
| Flutuação (porte `CAJADO`) | o orbe gira ±4° em volta da mão em 100 ticks, menos ao andar; a mão não solta a haste | `Item.flutua()` |
| Gesto do ataque | antecipação (recolhe e sobe), ação (estende rápido, ease-out) e volta (smoothstep) | a arma chama `preparar(f)` com o tempo que falta para o disparo e `disparou()` **no tick em que o projétil nasce**; o pico cai nesse quadro |

A ação parte de onde a antecipação estava, e a antecipação sem aviso cai sozinha
(`PREPARO_SUAVE`): perder o alvo no meio da carga não deixa a arma erguida nem a
faz pular. Os testes em `AnimacaoDaArmaTest` andam as curvas em quartos de tick e
recusam salto.

**O que a animação nunca muda:** de onde o tiro sai e a linha que libera o
disparo. A varinha calcula a ponta duas vezes, a de jogo (sem animação, origem da
bola) e a de desenho (com animação, para o halo e as faíscas); o arco atira com a
mira exata e só o desenho persegue a mira (`GIRO_SUAVE`), encaixando no ângulo do
tiro no tick da soltura. Dano, cadência, alcance, hitbox e velocidade de
projétil não passam por aqui.

**Por arma:**

- **Espada (`Sword`).** O golpe orbita o gato (antecipação, corte, sustentação,
  volta, pela `Investida`). Agora ele **sai da pose da mão e volta para ela**:
  `Investida.presenca` mistura as duas poses, usando o mesmo desenho (espelhado
  ou não) da mão. O rastro só acende depois do preparo (`passouDoPreparo`),
  porque borrão na antecipação distrai do corte. No tick do acerto, uma estrela de
  pixel e um disco de luz na ponta (`IMPACTO_TICKS`).
- **Arco (`Bow`).** A corda pintada no PNG é achada (`CordaDoArco`: a maior
  sequência de uma cor numa anti-diagonal), apagada de uma cópia do sprite e
  redesenhada em dois segmentos das pontas até o encaixe. A carga é a mesma conta
  do disparo (`countShot` e `count`), então a puxada (ease-in, 4 px) termina no
  tick real do tiro. Na carga cheia a corda treme; na soltura o arco recua 2 px
  com um coice de 7° e a corda passa do repouso e assenta (`cordaVoltando`). A
  flecha encaixada segue o encaixe da corda; a próxima só aparece na metade da
  soltura.
- **Cajados (`Wand`).** Flutuam; um halo pulsa atrás do orbe e cresce com a
  carga; faíscas saem do orbe (um punhado fixo, sem partícula do mapa) e andam
  conforme o elemento: o fogo sobe rápido, gelo e água caem devagar, terra cai
  pesada, ar sobe serpenteando. O gesto recolhe o cajado (em giro, sem tirá-lo da pata) nos últimos 12 ticks antes
  do tiro e estende em 3 ticks no disparo, com clarão na ponta. Sem elemento, o
  efeito é verde, como o brilho do orbe na arte oficial.
- **Demais armas de mão** (machado, garra, foice, tridente, espada de fogo)
  recebem respiro, passo, virada e flutuação pelo `naMao`; o golpe delas continua
  o da `Investida` via `naMaoGolpeando`. Armas apontadas (laser, kunai, arco
  elétrico) não passam pelo `naMao` e ficam como estavam.

**Evolução.** A arma da mão evolui pela transmutação (variante elemental) e pela
lendária. `Item.intensidade()` vale 0, 1 ou 2, e quem desenha efeito multiplica
por ele: a espada evoluída tem halo mais largo e um fio claro no rastro e
estrela maior; o arco evoluído acende a ponta da flecha durante a carga; o cajado
evoluído tem halo maior e mais faíscas. A cor vem de `Item.familiaDoEfeito`, a
família do elemento em `Familia`.

**Animar uma arma nova:**

1. Se ela é segurada, desenhe com `naMao(g, sprite)`: respiro, passo e virada vêm
   de graça. Se é de magia, `porte()` = `CAJADO` (ou sobrescreva `flutua()`).
2. Declare o gesto: sobrescreva `gesto()` com um `AnimacaoDaArma.Gesto` (ticks de
   ação e de volta, graus e pixels para trás e para a frente, subida), em
   constante nomeada no topo da classe.
3. No `tick` da arma, chame `animacao().preparar(fração)` enquanto o ataque
   carrega (a fração sai do mesmo contador que decide o disparo) e
   `animacao().disparou()` no tick em que o dano ou o projétil sai.
4. Efeitos (halo, clarão) leem `animacao().carga` e `.clarao` depois de
   `calcular`, usam `Brilho.disco` e cores de `Familia.tom(degrau, alfa)`, e
   escalam com `intensidade()`.
5. Números novos vão para `docs/CONTEXTO.md` §8.

**Referências usadas:** os 12 princípios de animação aplicados a jogos
(antecipação, follow-through e ação sobreposta: [Game Developer](https://gamedeveloper.com/production/the-12-principles-of-animation-in-video-games));
as fases de golpe em top-down e o "sem borrão na antecipação" do
[Pixelblog 56, Raymond Schlitter](https://www.slynyrd.com/blog/2025/5/23/pixelblog-56-top-down-character-attack-animation);
o *Animation Bootcamp* da GDC sobre Skullgirls (quadros-chave fortes,
antecipação e timing com orçamento curto: [GDC Vault](https://gdcvault.com/play/1020575/contactUs));
"The Art of Screenshake", de Jan Willem Nijman (Vlambeer), sobre reação da arma
ao disparo; e *Fix Your Timestep*, de Glenn Fiedler
([Gaffer on Games](https://gafferongames.com/post/fix_your_timestep/)), para o
tick fixo com interpolação. O cajado flutuante do "LPC simple staff", de Dr.
Jamgo ([OpenGameArt, CC0](https://opengameart.org/content/lpc-simple-staff)),
foi só referência de gesto; nenhum arquivo dele está no jogo.

**O cajado do Azrael** (`wand.png`) é desenhado por `tools/GenElementais.java`
(`cajadoDoAzrael`), não recortado de pacote. Ele segue `icons/Gato.png`: haste
longa dourada-clara, esfera dourada grande com luz em cima à esquerda, colar mais
escuro e o brilho verde, que não vai no PNG e é desenhado em jogo. É o mesmo PNG
na mão, na hotbar, no seletor de gatos e na tela de Status.

### Diálogos (`game/dialogos/`)

Conversas lineares (sem escolha de resposta) com retrato, emoção, máquina de
escrever e voz. A base serve a qualquer personagem; o primeiro é o vendedor da
antecâmara. A estrutura não impede escolhas no futuro: uma fala pode ganhar
opções que apontam para outra conversa, sem mudar a escolha nem a memória.

**Peças:**

| Peça | Papel |
|---|---|
| `Dialogos` | porta de entrada: `conversar(personagem, falante)`, `aoEvento(gatilho, falante)` e `escolher(...)`; `mostrarParaTeste(id, falante)` para a bancada |
| `CatalogoDeDialogos` | lê `resources/.../dialogos/` e valida textos, personagens, condições e eventos |
| `EscolhaDeConversa` | a regra de escolha, pura |
| `Condicoes` | as condições aceitas nos dados, por nome |
| `EventosDoDialogo` | o que um nome de evento faz (abrir loja, tocar som) |
| `MemoriaDosDialogos` | conversas vistas e a última corrida, em `saves/dialogos.json` |
| `CaixaDeDialogo`, `MaquinaDeEscrever`, `Retrato` | a tela, o texto letra a letra e o retrato com expressões |

**Os arquivos** (o JSON não aceita comentário; chaves começadas por `_` são
notas e são ignoradas):

- `personagens.json`: quem fala.
- `textos.json`: todas as frases, por chave. É o único lugar com texto de
  diálogo, pronto para uma tradução futura (o jogo hoje é só em inglês).
- um arquivo de conversas por personagem (`vendedor.json`).

```jsonc
// personagens.json
"vendedor": {
  "nome": "Merchant",                 // a aba (texto de tela, em inglês)
  "retrato": "vendedor",              // sprites/retratos/vendedor.png
  "voz": ["VozVendedor1", "VozVendedor2", "VozVendedor3"],  // nomes em Sounds, sorteados
  "moldura": "dialogo",               // 9-slice em ui/ (o tema da caixa)
  "conversas": "vendedor.json"        // arquivo das conversas dele
}

// vendedor.json, uma conversa
{
  "id": "vendedor.morte_rapida.a",    // único; é o que a memória grava
  "gatilho": "interagir",             // interagir | fim_de_corrida | fala_solta | outro
  "prioridade": 55,                   // maior toca primeiro; genéricas em 0
  "repeticao": "always",              // "once" = uma vez por save
  "condicoes": {                      // todas precisam valer (ver Condicoes)
    "ultima_corrida": "morte",
    "ultima_corrida_ate_segundos": 120
  },
  "falas": [
    { "emocao": "smug", "texto": "vendedor.morte_rapida.a.1" },
    { "emocao": "smug", "texto": "vendedor.morte_rapida.a.2",
      "eventos": ["som:Coin"] }       // opcional: dispara quando a fala aparece
  ],
  "aoTerminar": ["abrir_loja_do_vendedor"]   // no fim ou ao pular
}
```

`personagem` pode ir na conversa (dono) e em cada fala (quem fala naquela
linha); sem ele, vale o dono do arquivo. `emocao` desconhecida vira `neutral`.

**Emoções** (`Emocao`; a ordem é a das colunas do retrato, e emoção nova entra no fim):

| Nos dados | Emoção | Expressão do vendedor |
|---|---|---|
| `neutral` | neutro | olhos abertos, boca de gato |
| `happy` | feliz | olhos em arco, sorriso, bochechas coradas |
| `sad` | triste | sobrancelhas subindo ao meio, olhar baixo, boca caída, lágrima |
| `angry` | bravo | sobrancelhas descendo ao meio, olhos estreitos, boca reta |
| `confused` | confuso | uma sobrancelha alta, olhos de tamanhos diferentes, boca torta |
| `surprised` | surpreso | sobrancelhas altas, olhos grandes, boca em O |
| `smug` | malicioso | pálpebra caída, uma sobrancelha erguida, sorriso de canto |

O retrato (`sprites/retratos/<nome>.png`) tem uma coluna de 32×32 por emoção e
três linhas: olhos abertos, piscada e boca aberta (a fala alterna as duas). A
fala diz a emoção e o retrato troca sozinho, com um pulo de um pixel. Coluna que
falta, ou vazia, cai na neutra; linha que falta, na de olhos abertos. Parado, o
retrato pisca a cada 2,5 a 5 s e respira um pixel.

**Condições** aceitas hoje: `primeiro_encontro`, `ultima_corrida`
(`nenhuma`/`morte`/`vitoria`), `ultima_corrida_ate_segundos`,
`ultima_corrida_desde_segundos`, `ultima_corrida_nivel_minimo`, `gato`,
`moedas_minimo`, `moedas_maximo`, `ouro_minimo`, `vitorias_minimo`,
`corridas_minimo`, `corridas_maximo`, `conversas_minimo`, `vista_no_maximo`.
Condição desconhecida **reprova** a conversa e aparece no console e no
`DialogosTest`.

**A escolha** (`EscolhaDeConversa`): só as do gatilho e do personagem, que
passam nas condições, sem as `once` já vistas; ganha a de maior prioridade; no
empate, a menos vista, depois a vista há mais tempo, depois sorteio. Com isso a
reação ao que acabou de acontecer passa na frente da rotina (como no Hades), e
as genéricas giram sem repetir antes de todas tocarem.

**A caixa** (`CaixaDeDialogo`): retrato na moldura com orelhas (`ui/retrato.png`)
à esquerda, aba com o nome no desenho dos botões, painel `ui/dialogo.png` com a
fala e, embaixo, as dicas com os glyphs da tela de Controles ("Continue"
quando a fala acabou, com o botão do mouse, Espaço/Enter ou A conforme o
aparelho em uso; Esc/B "Skip" sempre). O jogo pausa por baixo. O clique é lido
à parte (`Mouse.click`), porque o `apertou()` das ações só lê teclas; o que for
apertado na carência ou no deslize da loja é descartado.
O primeiro clique, Enter, Espaço ou A completa a fala; o segundo passa. Esc/B
pula a conversa e dispara os eventos do fim. Velocidade em Settings → Interface
→ Dialogue speed (28, 48 ou 90 letras/s, ou instantânea), com pausa depois de
fim de frase e de vírgula. Um blip da voz a cada duas letras, no volume dos
efeitos. Entra subindo e aparecendo em 10 ticks e sai em 8. Tudo em ticks, com
`Engine.alpha()` entre eles; nada é criado por quadro.

**Gatilhos:** `Dialogos.conversar` (interação com o personagem; o vendedor usa
no clique e no B do controle) e `Dialogos.aoEvento(gatilho, falante)` para
eventos do jogo, em que qualquer personagem com conversa para aquele gatilho
pode falar. Falas soltas durante a corrida (barks, gatilho `fala_solta`) usam a
mesma escolha; falta o balão que não pausa o jogo, que não foi feito. No
teclado, falar com o vendedor continua sendo o clique: a tecla de interagir do
teclado foi uma decisão registrada no `AcaoTest` e não mudou.

**Eventos** (`EventosDoDialogo`): o nome no dado aponta para um efeito
registrado no código. Hoje: `abrir_loja_do_vendedor` (chama `abrirLoja()` do
falante, que é um `Lojista`) e `som:<Sounds>`. Evento novo é um
`EventosDoDialogo.registrar("nome", origem -> ...)`.

**A loja entrando de lado:** `EntradaLateral` desloca o desenho da direita para o
lugar em 14 ticks (ease-out) e bloqueia aperto enquanto desliza; a `Inter` a
recebe em `open(nome, entrada)`. O espelho usa a mesma entrada, sem aceitar compra durante a transição.

**Memória e migração.** `saves/dialogos.json` (versão 1) guarda, à parte do save
da corrida, que é apagado na morte: `vistas` (id → vezes), `quando` (id →
momento da última vez, em conversas tocadas), `conversas` (personagem → vezes),
`relogio` e `ultimaCorrida` (`total`, `resultado`, `segundos`, `nivel`,
`abates`, `gato`). O fim de corrida é gravado por `Game.gameOver` (morte, ou
encerrar depois de vencer). Na bancada nada é gravado.

**Integração com o espelho (2026-10-23).** `Seller.abrirLoja()` atende ao
mesmo evento `abrir_loja_do_vendedor`, abrindo `TelaDoEspelho`. Concluir ou
pular a conversa entrega o espelho; sem conversa elegível, abre diretamente.
A vitrine de armas fica em standby. `EntradaLateral` também é usada pelo
espelho, com entrada protegida por 14 ticks. O retrato e o nome vêm do mesmo
`Personagem`/`Retrato`; respostas de compra e reembolso usam chaves em
`dialogos/textos.json` e expressões da folha de emoções. As condições
`moedas_*` e o alias legado `ouro_minimo` consultam a carteira permanente do
gato atual. A memória continua em `saves/dialogos.json`, sem migração destrutiva.
O resultado final usa `Estatisticas.venceu()`, preservando vitórias retomadas.
**Como fazer:**

- *Diálogo novo:* acrescente a conversa no arquivo do personagem, as frases em
  `textos.json` e rode `./gradlew test` (o `DialogosTest` acusa texto,
  condição ou evento faltando). Para ver: `-Dcd.bench=true -Dcd.dialogo=<id>`.
- *Personagem novo:* entrada em `personagens.json`, arquivo de conversas, retrato
  em `sprites/retratos/` (pode começar só com a coluna neutra) e, se quiser voz,
  WAVs curtos + entradas em `Sounds`. Para ele falar, chame
  `Dialogos.conversar("<id>", entidade)` na interação dele.
- *Expressão nova num personagem:* desenhe a coluna da emoção no retrato (para
  o vendedor, em `tools/GenRetratos.java`, e rode o gerador); emoção nova para
  todos entra no fim de `Emocao`.
- *Disparar por evento:* `Dialogos.aoEvento("fim_de_corrida", null)` onde o
  evento acontece, e conversas com `"gatilho": "fim_de_corrida"` nos dados.
- *Trocar a loja aberta:* alterar `Seller.abrirLoja()`, mantendo o evento dos dados.

**Referências usadas:** a palestra *Breathing Life into Greek Myth: The Dialogue of
Hades* (Greg Kasavin e Darren Korb, GDC 2021,
[GDC Vault](https://gdcvault.com/play/1026975/Breathing-Life-into-Greek-Myth))
e a *AI-driven Dynamic Dialog through Fuzzy Pattern Matching* (Elan Ruskin,
Valve, GDC 2012, [GDC Vault](https://gdcvault.com/play/1015317/AI-driven-Dynamic-Dialog-through),
[resumo de Emily Short](https://emshort.blog/2012/03/16/gdc-2012-talk-on-dynamic-dialogue/)),
para condições, prioridade e a regra mais específica antes da genérica; o
formato de diálogo do Stardew Valley ([wiki de modding](https://wiki.stardewvalley.net/Modding:Dialogue)),
com retratos por emoção e neutro como padrão; o diálogo do Celeste
([Everest](https://github-wiki-see.page/m/EverestAPI/Resources/wiki/Adding-Custom-Dialogue)),
com retrato e expressão por linha; os blips por personagem do Undertale; e os
[Game Accessibility Guidelines](https://gameaccessibilityguidelines.com/allow-players-to-progress-through-text-prompts-at-their-own-pace),
para o avanço sempre por ação do jogador e o pulo. "Incraft" não foi
encontrado com esse nome; **a confirmar** a referência.

## Persistência

Tudo é JSON, gravado ao lado do executável (no `run`, a raiz do projeto):

| Arquivo | Dono | O que guarda |
|---|---|---|
| `config.json` | `Configs` | opções; gravação atômica com `.tmp` e `.bak` |
| `saves/corrida-N.json` (N = 1 a 3) | `Corrida` | a corrida interrompida. O gato: nível, vida, XP, inventário, melhorias, passivas, armas extras, evoluções, elemento. O andamento: relógio, abates, próximo chefe, Rato Rei, cerco, chefes vivos (tipo e fração de vida), níveis pendentes, trocas e pulos, carga da Fúria, números da tela final, vitória |
| `saves/progresso.json` | `save/Progresso` | moedas, níveis por gato e IDs de corridas pagas (v2); migra meta.json |
| `saves/credito-<hash>.json` | `save/Progresso` | recibo durável pendente de depósito; repetido com idempotência na abertura |
| `saves/conquistas.json` | `Conquistas` | o progresso das conquistas |

**Permadeath.** Cada partida grava sempre no mesmo espaço, escolhido no início;
ler um save o apaga, e o jogo regrava no primeiro tick; morrer apaga. Assim não
há como guardar uma cópia antes do perigo. O espaço 0 é o da bancada e nunca
grava.

**Quando grava**, sempre só no campo (a antecâmara não tem save — a corrida ainda
não começou):

1. ao entrar no campo (`Game.changeMap`);
2. depois de cada escolha que não pode ser re-sorteada: elemento, carta de nível,
   prêmio do baú — gravar antes faria do save uma máquina de re-rolar;
3. a cada minuto do relógio (`Arena.gravarACadaMinuto`), contra fechamento à força;
4. ao sair pela pausa ("Main Menu" ou "Quit").

**O que não grava:** inimigos comuns, projéteis, partículas e o campo. A horda é
função do relógio e se refaz em segundos; o campo é sorteado de novo. Os chefes
vivos são a exceção — sem eles, fechar o jogo pularia o chefe.

**Ordem da retomada** (`Game.retomar`): bônus de início (gato e upgrades
permanentes) → o gato do save → o campo → os reinícios de corrida → o andamento
do save → no primeiro tick, os chefes e a regravação. Os bônus vêm antes porque
o upgrade de vida enche a barra; a vida gravada tem de ser a última palavra.

`Progresso` e `Conquistas` ficam fora dos espaços: apagar uma corrida não apaga o que
foi conquistado. No modo bancada (`-Dcd.bench`) nenhum dos dois grava.

### Progressão permanente

Contrato completo, pesquisa, formatos, recuperação, tabela econômica e roteiro
manual em [PROGRESSAO.md](PROGRESSAO.md). `ArquivoSeguro` valida, força o
arquivo temporário, troca e conserva backup válido; `Corrida` usa a mesma
proteção. `Progresso` publica cópias somente após persistir. Um recibo antecede
cada crédito final; o id pago e as moedas entram no mesmo save. Falhas de saída
ou depósito permitem nova tentativa (`FalhaDeSave`) sem descartar a corrida.
`Espelho` aplica bônus sobre gatos novos, soma só diferenças nas compras do
hub e remove só sua camada ao reembolsar. A base continua em `players/*.json`.

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

### Ações e a tela de Controles

**A fonte única é `engine/inputs/Acao.java`.** Cada ação diz o nome que a tela
mostra, o grupo dela e quais **teclas virtuais** ela lê; o resto é derivado:

- a tecla física sai de `KeyBoard.Keys` (o código `KeyEvent` de cada nome);
- o botão do controle sai do mapa do `Gamepad` (`Gamepad.comandosDaTecla`), com as
  duas regras que não estão no mapa: o B (interage em jogo, volta nas telas) e o
  analógico esquerdo, que também aperta as setas;
- quem lê o controle direto (LB/RB da hotbar e das abas, o analógico direito da
  câmera) declara a peça na própria ação.

As ações de jogo leem por ela (`Acao.PAUSAR.apertou()`, `STATUS`, `INTERAGIR`,
`DASH`, `USAR_ITEM`, `LARGAR_ITEM`, `TELA_CHEIA`), então trocar a tecla de uma
delas muda o jogo e a tela juntos. As telas de menu continuam lendo setas, Enter
e Esc pelo nome. O `AcaoTest` varre `src/main/java` e falha se o código ler uma
tecla, ou um botão direto do controle, que não esteja em nenhuma ação. Não há
remapeamento; se vier, ele muda `KeyBoard.Keys` e o mapa do `Gamepad`, e a tela já
mostra o resultado.

**Ação nova:** uma constante em `Acao`, no grupo dela, com o rótulo em inglês e a
frase de ajuda. Ela aparece sozinha nas duas visões e o teste passa a cobri-la.
Se for lida no jogo, leia por `Acao.X.apertou()`.

**A tela** é a aba **Controls** do Settings (`menus/controles/PainelDeControles`),
pela tela inicial e pela pausa. A linha **Device** troca entre as duas visões, e a
aba abre na do último aparelho usado (`Entrada`).

- **Teclado e mouse:** lista em duas colunas por grupo (Movement, In a run, Menus,
  System): ação à esquerda, teclas à direita. WASD e setas do movimento em cruz.
  Sem mini-teclado: com umas vinte teclas espalhadas de F3 a Enter, um teclado
  inteiro nesta largura deixaria cada tecla com 4 ou 5 pixels.
- **Controle:** o desenho (`DesenhoDoControle`) com uma linha de cada peça até o
  rótulo. A **carcaça** é o Xbox One do pacote do lightUP (ver `TERCEIROS.md`),
  remapeada para a paleta e esculpida com volume; o logotipo do botão do meio é
  coberto por um botão liso. As **peças** (analógicos, direcional, botões de face
  com a letra, View e Menu) são pintadas em código por cima. Volume por escultura:
  a distância até a borda vira altura, o declive vira a direção da superfície e
  uma luz do alto à esquerda escolhe o tom numa rampa curta da paleta.
- **Linhas com um ângulo no máximo** (`LigacoesDoControle`). A maior parte vai a
  uma coluna: reta até a borda do desenho e diagonal até o rótulo, com os rótulos
  na ordem das âncoras para as diagonais não se cruzarem. View e Menu (na altura
  do analógico esquerdo) sobem retos até uma fileira de rótulos acima do desenho;
  o X (cercado pelos outros botões de face) e o analógico direito descem até uma
  fileira abaixo. O `DesenhoDoControleTest` confere que nada se cruza e que
  nenhuma reta passa por cima de outra peça.
- **Seletor In a run / In menus** acima do desenho: a mesma peça muda de papel (o
  B interage em jogo e volta nas telas), e cada rótulo mostra só o papel do
  contexto escolhido. Peça sem papel naquele contexto (o A na corrida, os
  gatilhos nos menus) sai esmaecida e sem rótulo. Clique, ou foco com
  esquerda/direita/Enter. A primeira versão mostrava os dois papéis com uma
  legenda de cores, que se lia como botão e não era.
- Foco ou mouse numa ação acende a peça; mouse na peça acende a ação. O balão
  de ajuda, nesta aba, aparece só pelo mouse: pelo foco ele ficaria parado em
  cima do desenho.
- **Escala:** cada visão usa a maior escala inteira que cabe, até a da interface
  (`escalaQueCabe`); o desenho do controle cresce até o dobro da escala do texto
  se houver espaço. Só em 1x, sem ter para onde descer, a lista do teclado rola.
- O painel do Settings passou de 240 para 288 pixels nativos de largura, para o
  desenho e as duas colunas de rótulos caberem, e cresce na altura até caber a
  lista do teclado, igual em todas as abas.

**Glyphs** (`engine/graphics/Glyphs`): teclas, mouse e botões do pacote da Kenney
(ver `TERCEIROS.md`), com a auréola de dois pixels da folha afinada para um
contorno de um pixel na cor da paleta e o desenho recortado. Ampliados por fator
inteiro e guardados por escala. O **rodapé** (`Rodape`) usa os mesmos desenhos:
cada pedaço do rótulo separado por "/" vira um desenho, e o rótulo sem desenho
continua como texto numa capa de tecla.

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

### Ícones (`Icone`)

Buffs (atributos), passivas, armas extras, o troféu, os elementos e as raridades
têm um ícone cada, num **registro só**: `game/items/Icone`. Cada constante é um PNG
de 16x16 em `resources/.../icones/`, gerado por `tools/GenIcones.java`. Quem
desenha pede pelo tipo (`Icone.de(melhoria)`, `de(passiva)`, `de(arma)`,
`de(elemento)`, `de(raridade)`) e nunca abre o arquivo direto. Aparecem nas cartas
de nível e no "Your build" (`SubirDeNivel`), na prévia e no prêmio do baú
(`AberturaDoBau`), nas cartas de elemento (`EscolhaDeElemento`), nos selos do item
(`Selos`, caixa de informação), no troféu da tela inicial, do fim de corrida e das
conquistas. A tela de Status e a HUD não desenham ícone de buff.

**O padrão visual**, igual ao dos itens que o gato segura:

- **Grade:** 16x16, a mesma dos sprites; desenhado em fator inteiro
  (`Icone.lado(escala)`, `desenharNoCentro` escolhe o maior que caiba), com a cópia
  de cada fator guardada. Nada de esticar por fator quebrado.
- **Contorno:** um pixel, sempre `Palette.OUTLINE`, em volta da silhueta. Traço de
  um pixel (haste de flecha) fica sem contorno, num tom só.
- **Sombreado:** luz do alto à esquerda, como na interface inteira. Rampa de cinco
  tons por material: sombra funda, sombra, base, luz e brilho. Sem antisserrilhado
  nem pixel meio transparente.
- **Cor:** cada material usa uma família de cor gerada da mesma fórmula a partir de
  uma cor oficial do jogo (`Elemento.cor`, `Raridade.cor`, a vida da `BarraDeChefe`,
  as cores das cartas), puxada 20% para `Palette.MAIN`; o metal é a escada da
  própria `Palette`. Nada de preto ou branco puros.
- **Forma antes da cor:** cada ícone da build tem silhueta própria, e o
  `IconeTest` reprova dois com menos de 20 pixels de diferença de silhueta. Quem
  não distingue as cores distingue as formas.
- **Apagado:** `desenharApagado` leva cada pixel ao cinza da interface de mesma
  luminância (DEEP, MAIN, LIGHT), mantendo o contorno: a prévia do baú fora do
  destaque, a conquista por fazer, o troféu do menu sem hover.

**De onde vêm:** a base é o pacote DB32 (CC0, `tools/assets/fonte/db32`),
remapeado cor a cor: cada cor da paleta DawnBringer 32 tem um material, e dentro do
material as cores vão para os degraus da rampa pela luminância. O que o pacote não
tem é desenhado no gerador como silhueta de texto 16x16, com o sombreado e o
contorno calculados pela mesma regra. A tabela de origem de cada ícone está em
`docs/TERCEIROS.md`.

**Ícone novo:** uma entrada em `ICONES` no `tools/GenIcones.java` (do pacote, com a
família de cada material, ou desenhado), rodar `java tools/GenIcones.java`, e uma
constante com o mesmo nome em `Icone` (e o `de(...)` do tipo, se for de um tipo
novo). O `IconeTest` confere arquivo, tamanho, contorno, paleta e silhueta.

Os ícones de antes ficam fora de uso: as matrizes 9x9 saíram de
`game/map/arena/Icones` (que só sobrou para a Roleta desativada), e os PNG de
`sprites/items/elementos/` e `sprites/items/melhorias/`, com os geradores
`GenIconesElementais` e `GenIconesMelhorias`, não são lidos por nada.

### A gameplay na identidade: arte em jogo = arte do ícone

**A regra:** o que aparece na arena para uma arma, buff, elemento ou coletável sai
do **mesmo registro** do card. A forma vem de `Icone` e a cor de `Familia`
(`engine/graphics/Familia`): as mesmas famílias e a mesma fórmula do gerador de
ícones, com as versões transparentes prontas em degraus (nenhuma `Color` nova por
quadro). O `FamiliaTest` confere que toda cor dos PNG de ícone está numa família,
para jogo e gerador nunca divergirem.

| No mundo | De onde vem |
|---|---|
| Novelo e grimório orbitando | `Icone.NOVELO`, `Icone.GRIMORIO`, em fator inteiro (o grimório era um livro vermelho de cruz, outro que o do card) |
| Aura de catnip | campo e borda na família verde (ouro na evolução), três folhas do `Icone.AURA` girando na borda |
| Raio, tremor, meteoro | famílias elétrica, terra e fogo; o meteoro que cai é o `Icone.METEORO`; o aviso é fogo, e não vermelho (é arma do gato) |
| Gema de XP, XP grande | `Icone.XP` (azul do raro), `Icone.XP_GRANDE` (ouro), halo de imagem pronta |
| Coração de cura | `Icone.VIGOR`, o mesmo do card de vida |
| Coleta | `Coleta`: o ícone sobe e pisca por 8 ticks |
| Barra de vida e de armadura do bicho | o desenho das barras da HUD: contorno `OUTLINE`, fundo `DEEP`, família da vida (armadura em ouro) |
| Aura de elite, brilho do baú | ouro da família; disco de imagem pronta |
| Fúria (onda e clarão) e Retaliação | família épica (a barra) e verde (o ícone) |
| Brasa do fogo, coração da regeneração, bolha do veneno, faísca do golpe | tiras remapeadas por `GenIcones` (originais em `tools/assets/particulas_original`); o veneno é roxo, como o número dele |
| Aviso de spawn (`Chegada.Tom.PERIGO`) | família da vida |

### Números de dano (`NumeroDeDano`)

Uma partícula só para dano e cura; as palavras ("Dodge", "FURY!", o nome da arma
transmutada) ficam em `Word`, no texto da casa (`TextFx`). A cor diz o que foi:

| Número | Cor | Estilo |
|---|---|---|
| Dano comum | `TextFx.CLARO` | fonte do jogo 5 |
| Dano de fogo, gelo, água, terra, ar | a luz da família do elemento (o ar na base) | 5 |
| Veneno | família épica (roxa) | 5 |
| Crítico | ouro (luz) | 7, com pop de 8 por 5 ticks e o estouro do `Icone.CRITICO` ao lado |
| Cura | verde (luz), com "+" | 5 |

O crítico não depende só da cor: é maior e leva o estouro. O rótulo "CRIT" saiu.
**Golpes seguidos somam:** do mesmo tipo, no mesmo bicho, a menos de 12 ticks,
viram um número que cresce. **Teto:** com 36 números no ar, um golpe comum só
aparece se somar num que já está no ar; crítico e cura sempre aparecem. Cada
algarismo é pintado uma vez por estilo e escala, com o contorno da casa um degrau
mais fino que a escala; o quadro só copia imagens.

**Hierarquia de leitura:** gato > perigo > projétil inimigo > coletável > efeito.
Por isso o teto dos números, o contorno mais fino e o clarão da Fúria mais fraco
(60 de opacidade em vez de 120: estoura várias vezes por minuto).

**Visual novo:** se tem card, desenhe com o `Icone` dele; a cor sai de `Familia`
(acrescente uma família nos dois lugares, `Familia` e `GenIcones`, se o assunto
for novo); disco ou halo por `Brilho.disco`, nunca `fillOval` suavizado por
quadro; partícula com tira PNG entra em `TIRAS` do `GenIcones`, com o original em
`tools/assets/fonte`.

## Áudio

Segue a divisão padrão de jogos: **efeitos em WAV**, curtos e carregados em
memória, para não pagar decodificação onde a latência é perceptível; **músicas
em OGG Vorbis**, lidas em streaming, porque são faixas de minutos.

O TinySound não conhece OGG. Quem decodifica é o `vorbis-support`, registrado
como Service Provider do `javax.sound`: como o TinySound usa
`AudioSystem.getAudioInputStream()` por baixo, basta a biblioteca estar no
classpath. Nenhuma linha de código de áudio mudou por causa do formato — só a
extensão do arquivo.

**As músicas carregam em segundo plano** (`Sound.load`). Medido, carregar as
faixas levava 3,4 s dos 3,9 s em que o jogo ficava sem janela depois do clique:
a TinySound decodifica cada OGG inteiro ao carregar (com `streamFromFile`, para
um arquivo temporário). Agora os efeitos (0,2 s) carregam antes da janela e as
músicas numa thread de baixa prioridade, a da tela inicial e a ambiência
primeiro, enquanto a abertura roda. Pedir uma faixa que ainda não chegou não
quebra nada: ela fica pendente e o `Sound.tick` a inicia quando estiver pronta, no
volume e na rampa que pediram; parar uma pendente só desfaz o pedido.

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

**Timbre arcano:** `GenSomDoCajado.java` sintetiza `spell_arcane.wav` para
`Sounds.FeiticoArcano`, usado pelo cajado comum no lançamento e pelo grimório no
contato. É um sopro filtrado com ressonâncias estáveis, sem a varredura de laser;
240 ms, ataque de 3 ms, cauda de 60 ms, -28 dB RMS, teto -1,5 dBFS. O laser
continua com seu efeito próprio e cajados elementais mantêm os `hit_*`.

**Sincronização dos golpes (2026-10-23):** a chamada de áudio pertence ao tick do
evento. `Wand` tocava no callback de colisão, por isso o atraso variava com a
distância e um tiro perdido ficava mudo; agora toca ao criar a bola, junto do
gesto. Arremessáveis têm áudio no lançamento; foice, escudo e orbitais no contato;
a aura toca por pulso com acerto. Bombas e meteoros tocam na explosão, não no
aviso. Garra e machado mantêm o som do corte mesmo se o alvo escapar.

Os WAV também atrasavam o retorno: medidos a 10% do pico, `sword.wav` começava
perto de 202 ms, `laser.wav` de 78 ms e `woosh.wav` de 274 ms. `hit_earth.wav`
começava fraco e tinha pico a 180 ms. `GenSonsElementais` agora procura uma frente
forte no trecho inicial, preserva 2 ms antes dela, aplica abertura/queda suaves
e normaliza em -28 dB RMS. Gera os seis `hit_*` e os seis `weapon_*`; os originais
permanecem intactos. `Sounds.Sword/Laser` apontam para os recortes mecânicos.
Os testes verificam os WAV reais (ataque em até 10 ms, cauda até 350 ms e nível).

O segundo freio de 130 ms em `Item.tocarGolpe` foi retirado: ele descartava
golpes legítimos de armas aceleradas. A proteção continua centralizada em
`LimiteDeVozes`, sem fila de reprodução atrasada. A latência física do dispositivo
e do buffer do TinySound não foi medida por esses testes; eles verificam a emissão
no tick e o ataque dos arquivos, não a saída acústica do computador.

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
- **Economia inicial a validar com jogadores.** Moedas permanentes e vendedor ativos;
  preços e ritmo propostos em PROGRESSAO.md. Dinheiro local e máquinas antigas
  continuam em standby, sem segundo ponto de compra.
- `tools/GenCisterna.java` ainda escreve as plantas `cistern_1` a `6`, que o jogo
  não lê mais; os tiles que ele gera continuam em uso pelo `Gerador`.

### Vendedor na interface do espelho

TelaDoEspelho reutiliza o personagem `vendedor` do catálogo de diálogos:
nome, retrato 32×32 com emoções e chaves de fala em `dialogos/textos.json`.
Não usa mais o sprite de corpo inteiro ampliado. Retrato em fator 2 da escala
da interface (64 pixels nativos), faixa de 80 e texto de compra/reembolso por
180 ticks. Boca alterna a cada 6 ticks e piscada a cada 180. O relógio anda no
tick e `Retrato` mantém o cache. Grade preservada; saldo à direita. A conversa
com voz antecede a loja; a entrada lateral protege a primeira compra.