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
| `game.hud` / `game.interfaces` | HUD (relógio, barra de chefe, flash de dano) e telas in-game (inventário, lojas) |
| `com.retronova.menus` | Menu, saves, seleção de personagem, opções, pausa, fim de corrida, vitória, conquistas |

## Laço principal

`Engine.run()` mantém dois relógios independentes:

- **Tick** a 60 Hz fixos, com recuperação de atraso limitada a 5 ticks por volta.
  A simulação tem prioridade sobre o desenho.
- **Render** ao limite configurado em Opções, descartando quadros perdidos em
  vez de tentar recuperá-los.

Tick e render compartilham a mesma thread. Quando o desenho atrasa, o jogo perde
quadros, não velocidade.

O render **interpola** entre o tick anterior e o atual (`Engine.alpha()`): cada
entidade guarda a posição antes do tick e é desenhada no meio do caminho. É o
que deixa 144 Hz ou mais lisos com a simulação presa a 60. Com uma tela por cima
(pausa, cartas, roleta) a interpolação é desligada, senão o mundo parado
tremeria.

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

Toda tela de menu estende `TelaDeMenu`: Menu, Saves, Personagens, Options,
Pause, TelaDeConquistas, GameOver e Vitoria. A moldura é sempre a mesma:

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
- **Uma tela de opções só.** `Options` abre igual da tela inicial e da pausa;
  só o fundo muda. As abas (Video, Audio, Interface) usam `Abas`, com o mouse, Q/E
  e LB/RB.
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
carta sob o mouse ou o foco subindo com brilho na cor dela, teclas 1 a 3, setas e
Enter, clarão na escolhida e fechamento.

Cada carta só varia no conteúdo:
`new MaoDeCartas.Carta(rotulo, cor, icone, nome, efeito)`. O `icone` é quem
desenha o medalhão: `SubirDeNivel` usa `Icones.desenhar`, e
`EscolhaDeElemento` usa `Relevo.desenhar` com o selo do elemento. O efeito
aceita várias linhas separadas por `\n`.

Uso: criar com a lista de cartas; a cada tick chamar `posicionar(y, s)` e
`tick()`, que devolve o índice escolhido naquele tick (ou -1); fechar a tela
quando `fechou()` for verdadeiro. `pronta()` diz se a mão já aceita entrada, para
os controles em volta dela (Reroll, Skip).

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
(2,3 s) com a arma parada acima da cabeça.

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
  `Engine`, fora da EDT. Agora passa pela EDT com `invokeAndWait`.
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
