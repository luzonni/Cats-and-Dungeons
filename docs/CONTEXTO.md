# Contexto do projeto

Ponto de partida para quem chega sem saber nada — pessoa ou agente. Lendo só
este arquivo dá para retomar o trabalho; os outros documentos aprofundam.

Estado descrito: **2026-10-21**, a partir do commit `8a9cbb8` (merge do PR #31) mais a limpeza
da branch `docs/estado-atual` (save da corrida, remoção do código inalcançável). Itens
marcados **a confirmar** não puderam ser verificados pelo código nem pelos
commits.

---

## 1. O jogo em poucas linhas

*Cats & Dungeons* é um roguelike de sobrevivência em horda, no molde do
*Vampire Survivors*, em pixel art. O jogador escolhe um de três gatos, passa por
uma antecâmara e cai num campo aberto gerado na hora, onde a horda de ratos não
para. As armas atacam sozinhas; o jogador anda, dá dash e decide o que pegar a
cada nível. A corrida tem **30 minutos**: aos 30 nasce o chefe final, o **Rato
Rei**. Vencê-lo dá a vitória, com a opção de continuar jogando. Morrer encerra a
corrida, mas o **ouro** ganho compra upgrades permanentes para as próximas.

Projeto de faculdade de quatro autores, retomado em 2026-09 depois de mais de
um ano parado. Sem engine: tudo em Java puro.

## 2. Stack, versões e como rodar

| | |
|---|---|
| Linguagem | Java 21 (toolchain do Gradle; o código usa `SequencedCollection`) |
| Build | Gradle (wrapper) com Kotlin DSL — `build.gradle.kts` |
| Gráficos | `java.awt.Canvas` + `BufferStrategy`, pipeline OpenGL do Java 2D ligado em `Main` |
| Áudio | TinySound (`libs/`) + `vorbis-support` para OGG via SPI |
| Dados | json-simple |
| Controle | XInput do Windows via JNA (que vem com o OSHI) |
| Testes | JUnit 5, headless — 15 classes, 120 testes passando em 2026-10-21 |
| CI | GitHub Actions (`.github/workflows/ci.yml`) + CI local (`tools/CiLocal.java`) |
| Versão declarada | `0.1.3-alpha` em `build.gradle.kts` (ver [releases](releases/)) |

```sh
./gradlew run        # roda o jogo (Windows: gradlew.bat)
./gradlew build      # compila e testa
./gradlew test       # só os testes
./gradlew fatJar     # jar único em build/libs/*-all.jar
./gradlew ci         # a CI inteira, local
./gradlew hooks      # uma vez por clone: o push passa a rodar a CI local antes
./gradlew run -Pvitrine   # vendedor com todos os itens, dinheiro não cai
```

**Modo bancada** (propriedades de sistema lidas em `Engine`, `Waves`, `Meta`,
`Conquistas`): `-Dcd.bench=true` pula os menus e entra numa corrida no espaço de
save 0, que não grava nada. Com ele: `-Dcd.gato=0|1|2` (Muffin, Azrael, Finn),
`-Dcd.minuto=N`, `-Dcd.roleta`, `-Dcd.fim`, `-Dcd.nivel`, `-Dcd.rei=1..4`,
`-Dcd.armas=RAIO,TREMOR,...`, `-Dcd.habilidade=1..3`. `-Dcd.perf=true` liga o
medidor de quadros. O `build.gradle.kts` só repassa `-Pvitrine` ao `run`; **como
o time passa os `-Dcd.*` (IDE, `java -jar`?) está a confirmar**.

**Modo foto** (`engine/Foto.java`), para conferir telas sem jogar até elas:
`-Dcd.foto=<pasta>` roda um roteiro e grava cada tela em PNG. O tamanho vem de
`-Dcd.foto.tamanho=1280x720`, e o roteiro de
`-Dcd.foto.roteiro="espera:60;tecla:Down;tecla:Enter;mouse:x,y;clique;foto:nome"`.
A entrada é injetada no estado do jogo, nunca no sistema, e a janela abre fora
da tela, sem foco e sem ícone na barra de tarefas. Saves, meta e conquistas vão
para `build/foto/saves`, o `config.json` nunca é gravado e o áudio fica mudo.
Combina com o modo bancada (`-Dcd.bench=true -Dcd.nivel=true`, por exemplo).

Arquivos gerados em tempo de execução, todos no `.gitignore`: `config.json`,
`saves/corrida-{1,2,3}.json`, `saves/meta.json`, `saves/conquistas.json`.

## 3. Mapa de pastas

```
src/main/java/com/retronova/
├── Main.java                  liga OpenGL e abre o Engine na EDT
├── engine/                    camada própria: laço, janela, configs, pilha de Activity
│   ├── graphics/              sprites, fontes, 9-slice, paleta, texto com contorno
│   ├── inputs/                teclado, mouse, controle (gamepad/), modo de entrada
│   ├── io/                    leitura de JSON dos recursos
│   └── sound/                 músicas, efeitos, curva de volume (Ganho)
├── game/
│   ├── Game.java              a Activity da partida: tick, render, fim de jogo
│   ├── Corrida.java           save da corrida (3 espaços)
│   ├── Meta.java              ouro e upgrades permanentes
│   ├── Conquistas.java        10 conquistas
│   ├── Estatisticas.java      números da corrida para a tela final
│   ├── hud/                   HUD, hotbar, relógio, barra de chefe, flash de dano
│   ├── interfaces/            inventário, loja, máquina de chiclete, Grade (foco)
│   ├── items/                 armas da mão, consumíveis, Elemento, Classe,
│   │                          Melhoria/Melhorias, Passiva, ArmaExtra, Evolucao
│   ├── map/                   GameMap, Camera, Atmosfera (luz e sombra)
│   │   ├── arena/             Arena, Waves, Gerador, cartas, roleta, música
│   │   └── room/              Room (antecâmara)
│   └── objects/               entidades, tiles, partículas, física
│       └── entities/          Player, Bonus, Arsenal, ArmasDeArea, Furia,
│                              Habilidades, enemies/, furniture/, utilities/, NPCs/
└── menus/                     Menu, Saves, Personagens, Options, Pause,
    │                          GameOver, Vitoria, TelaDeConquistas
    └── shared/                TelaDeMenu, Rodape, Abas, Realce, MaoDeCartas,
                               Button, OptionRow, Confirm
src/main/resources/com/retronova/resources/
├── maps/                      PNG (cor = tile) + JSON (entidades); waves/*.json
├── players/                   Muffin.json, Azrael.json, Finn.json
├── sprites/  audio/  fonts/  ui/
tools/                         geradores de assets (rodar com `java tools/X.java`)
docs/                          esta pasta
```

## 4. Onde fica cada sistema

| Sistema | Arquivos |
|---|---|
| Laço de jogo (60 Hz fixo, render separado) | `engine/Engine.java` |
| Fluxo de telas | `Engine.heapActivity` / `backActivity` / `pause` (pilha de `Activity`) |
| Base de toda tela de menu (título, rodapé, transição, voltar) | `menus/shared/TelaDeMenu.java`, `Rodape.java`, `Abas.java` |
| Hover e foco de todo elemento da interface | `menus/shared/Realce.java` |
| Cartas de escolha (nível e elemento) | `menus/shared/MaoDeCartas.java` |
| Transmutação da arma ao escolher o elemento | `game/objects/entities/Transmutacao.java` |
| Modo foto (capturas de tela por roteiro) | `engine/Foto.java` |
| Partida | `game/Game.java` |
| Corrida sem fim, horda, chefes | `game/map/arena/Waves.java` |
| Campo gerado (160×160 tiles) | `game/map/arena/Gerador.java` |
| Antecâmara | `game/map/room/Room.java` + `maps/beginning.{png,json}` |
| Gato: movimento, dash, dano, XP, morte | `game/objects/entities/Player.java` |
| Atributos base dos gatos | `resources/.../players/*.json` |
| Bônus de gato, passivas, meta | `game/objects/entities/Bonus.java` |
| Habilidades exclusivas (Muffin, Azrael) | `game/objects/entities/Habilidades.java`; Finn em `items/Bow.java` |
| Armas extras orbitais | `game/objects/entities/Arsenal.java` |
| Armas extras de área | `game/objects/entities/ArmasDeArea.java` |
| Fúria Felina | `game/objects/entities/Furia.java` |
| Inimigos (base, escala, elite, golpe) | `game/objects/entities/enemies/Enemy.java` |
| Chefes | `enemies/Chefe.java`, `RatoRei.java`, `CatToyBoss.java`, `KingCursedCatBoss.java` |
| Golpe com aviso | `game/objects/Investida.java` |
| Subir de nível (3 cartas) | `game/map/arena/SubirDeNivel.java` |
| Baú → caça-níquel | `utilities/Bau.java` → `game/map/arena/Roleta.java` |
| Escolha de elemento (aos 45 s) | `game/map/arena/EscolhaDeElemento.java` |
| Save | `game/Corrida.java`, `menus/Saves.java` |
| Entre corridas | `game/Meta.java`, `menus/GameOver.java` (loja de upgrades) |
| Vitória | `menus/Vitoria.java`, `Game.vencer` |
| Música da arena | `game/map/arena/TrilhaDaArena.java`, `engine/sound/Musics.java` |
| Controle | `engine/inputs/gamepad/`, `engine/inputs/Entrada.java`, `game/interfaces/Grade.java` |
| Luz, sombra, vinheta | `game/map/Atmosfera.java` |
| Configurações | `engine/Configs.java` (`config.json`, gravação atômica) |

## 5. Convenções

- **Código em português, tela em inglês.** Classes, métodos e comentários em
  português (`Corrida`, `SubirDeNivel`, `escalarPara`); todo texto visível ao
  jogador em inglês. Decisão de 2026-09: não há i18n.
- O código antigo, de 2025, mistura nomes em inglês (`Player`, `Enemy`,
  `Waves`, `strike`). Não há renomeação em massa: o novo sai em português.
- **Comentários explicam o porquê**, em blocos longos, com a decisão em
  MAIÚSCULAS no começo do parágrafo. Muitos registram o defeito que motivou a
  mudança. É o estilo da casa.
- **Enums gravados por ordinal só crescem no fim**: `ItemIDs`, `EntityIDs`,
  `ArmaExtra`, `Passiva`, `Evolucao`. Inserir no meio quebra mapas e saves.
- **Escala sempre inteira** para pixel art (`Configs.GameScale`, `UiScale`).
  Resolução lógica de 320×180.
- Assets de UI e boa parte dos sprites são **gerados** por `tools/Gen*.java` e
  versionados; a CI confere os de UI.
- Commits em inglês no formato Conventional Commits: `tipo(escopo): resumo`,
  seguido de tópicos `- ...` sem linha em branco entre eles e sem ponto final.
  A regra completa fica em `documentation/padroes/PADRAOCOMMITS.MD`, que é
  **local**: a pasta `documentation/` está no `.gitignore` e só existe na
  máquina de quem a tem. O mesmo vale para `documentation/padroes/ARTE-CENARIO.md`,
  citado em comentários de `tools/` e da mobília.
- Trabalho entra por branch e PR para a `develop` no repositório
  `luzonni/Cats-and-Dungeons`: CI verde mescla sozinho, e a `develop` testada vai
  sozinha para a `main`. Push direto em `develop` e `main` é recusado pelo hook.
  Ver [CONTRIBUINDO.md](CONTRIBUINDO.md).

## 6. Estado das funcionalidades

✅ pronto · 🟡 parcial · 🧪 só começado · ⏸️ em espera (no código, sem uso, por decisão)

| Funcionalidade | Estado | Nota |
|---|---|---|
| Menu, opções (vídeo, áudio, interface), pausa | ✅ | todas sobre `TelaDeMenu`; a mesma tela de opções no título e na pausa |
| Seleção de personagem (3 gatos) | ✅ | |
| Três espaços de save, retomar e apagar | ✅ | |
| Antecâmara com vendedor, placas e portão | ✅ | o portão leva à corrida |
| Corrida sem fim de 30 min em campo gerado | ✅ | |
| Horda por tempo, cercos, elites | ✅ | |
| Chefes a cada 5 min + Rato Rei aos 30 | ✅ | o Rato Rei usa o sprite do escudeiro ampliado |
| Vitória (encerrar ou continuar) | ✅ | |
| XP, nível e cartas de nível (reroll 2, skip 1) | ✅ | |
| Passivas (7 gerais + 1 exclusiva por gato) | ✅ | |
| Armas extras (6) e evoluções (6) | ✅ | |
| Baú e caça-níquel | ✅ | |
| Fúria Felina | ✅ | |
| Quebráveis (cura ou XP grande) | ✅ | |
| Escolha de elemento | ✅ | oferecida aos 45 s de corrida |
| Ouro e upgrades permanentes | ✅ | |
| Conquistas (10) | ✅ | |
| Controle de Xbox, foco por D-pad | ✅ | só Windows |
| Luz dinâmica, sombras, interpolação, VSync | ✅ | |
| Música que acompanha a corrida | ✅ | |
| Save no meio da corrida | ✅ | relógio, chefes vivos e números da corrida vão junto; ver ARQUITETURA |
| Dinheiro | ⏸️ | **em espera, por decisão**: o gato começa com 100, nada rende moeda, só a antecâmara vende. `Seller`, `Store`, `Coin` e `GumMachine` ficam no código sem uso até o dinheiro ganhar papel |
| Plano de cartas com encaixes e status (HABILIDADES.md) | 🧪 | nenhuma fase implementada |

Removidos em 2026-10-21 por serem inalcançáveis: o modo por ondas e salas
(dificuldades `normal` e `hard`, plantas `cistern_1..6`, JSON de `waves/`), a
`Recompensa` de fim de sala, o vendedor de fim de sala, o `TrapDoor`, a `Door`, o
`Crate` com loot, a `Chain`, o `MonarkMouse`, o `CryingCat` e o A\* (só o zumbi
usava, e só fora da corrida sem fim). Estão no histórico do git.

## 7. Decisões importantes

- **Sem engine** e sem framework gráfico. O pacote `engine` é código próprio.
- **Corrida sem fim** no lugar de salas: a pergunta é "quanto tempo eu
  aguento?" (`Waves`, `Gerador`). Commits de 2026-10-19.
- **Armas atacam sozinhas**; o dash é de fábrica e é a única ação de momento.
- **Uma família de arma por gato** (`Classe`): a escolha do gato decide o jeito
  de jogar.
- **Elemento é da corrida**, não da arma: escolhido uma vez, transmuta a arma.
- **Permadeath com três espaços**: cada partida grava sempre no mesmo espaço;
  morrer apaga o save.
- **Meta-progressão pequena** de propósito: upgrades de +6 de vida, +1 de dano.
- **Áudio em régua de loudness** (`tools/GenMixagem.java`) e curva logarítmica
  nos volumes. Ver [ARQUITETURA.md](ARQUITETURA.md#áudio).
- **Controle traduzido em teclas virtuais**: nenhuma tela conhece o controle.
- **Uma gramática de interface** (2026-10-21): toda tela de menu estende
  `TelaDeMenu`, todo hover e foco passa por `Realce` (crescer, brilhar, clarear,
  nunca contorno) e toda escolha por cartas usa `MaoDeCartas`. Ver
  [ARQUITETURA.md](ARQUITETURA.md#interface).
- **Recompensa sem parar o jogo**: a transmutação da arma dura 0,3 s e não
  bloqueia movimento nem ataque; a escolha já foi a pausa.

## 8. Onde estão os números de balanceamento

| O quê | Onde |
|---|---|
| Atributos base dos gatos | `resources/.../players/*.json` |
| Bônus de cada gato | `Bonus.java` (`area`, `critico`, `aplicarNoInicio`) e `Bonus.habilidade` |
| Curva de XP | `Player.getXpLength` — `15 + 10·n + 0,8·n²` |
| Sorte → chance; chance de baú | `Player.chanceDeSorte`, `Player.chanceDeBau` |
| Dash, invulnerabilidade | `Player` (`DASH`, `INVULNERAVEL = 24`) |
| Ritmo da horda, tipos por minuto, chefes, teto de vivos | `Waves.java` (`ESCALA`, `CHEFES`, `MAX_VIVOS`, `tick`) |
| Escala de inimigo por degrau (+7% vida, +4% dano) | `Enemy` (`CRESCIMENTO_VIDA`, `CRESCIMENTO_DANO`) |
| Elite | `Enemy.tornarElite`, chance em `Waves.prepararBicho` |
| Vida, dano e golpe de cada inimigo | construtor de cada classe em `enemies/` |
| Cartas de nível (pesos, raridade, valores) | `SubirDeNivel.sortearMao` |
| Prêmios do caça-níquel | `Roleta.sortear` |
| Valores das passivas | `Passiva.java` (`porNivel`) |
| Números das habilidades exclusivas | `Bonus.java` (Retaliação, Explosão Arcana, Voleio) |
| Armas extras | `Arsenal.java`, `ArmasDeArea.java` |
| Fúria | `Furia.java` (`MORTES = 30`) |
| Elementos (multiplicadores de dano e cadência) | `Elemento.java` |
| Raridades e desgaste das melhorias | `Raridade.java`, `Melhoria.desgaste` |
| Ouro e preços da meta | `Meta.java` |
| Metas das conquistas | `Conquistas.java` |

Ajuste fino da interface (cada classe lista os números no topo, numa seção
"ajuste fino"):

| O quê | Onde |
|---|---|
| Transição das telas de menu | `TelaDeMenu` (`TRANSICAO = 9` ticks, `SUBIDA = 6` px) |
| Hover e foco: velocidade, crescimento, brilho, setas | `Realce` (`VELOCIDADE`, `CRESCIMENTO`, `CAMADAS`, `ALFA`, `CLAREAMENTO`, `SETA`, `AFASTAMENTO`, `RESPIRO`, `PERIODO_DO_RESPIRO`) |
| Cartas: entrada, fechamento, tamanho, subida | `MaoDeCartas` (`ENTRADA = 26`, `FECHAR = 18`, `LARGURA`, `ALTURA`, `VAO`, `SUBIDA`) |
| Transmutação da arma | `Transmutacao` (`ESTOURAR = 6`, `ASSENTAR = 12`, `ALTURA`, `ESTOURO`, `SEGUNDOS_DO_NOME`, `RAIO_DO_ANEL`, `FORCA_DO_ANEL`) |
| Música na pausa | `Sound.ABAFADO_NA_PAUSA = 0.35` |

## 9. Próximos passos conhecidos

Tirados do código e do [roadmap](planejamento/ROADMAP.md); nenhum tem dono
definido:

- Tirar o dinheiro da espera: dar a ele um papel na corrida sem fim (moedas
  caindo, loja no meio da corrida) ou removê-lo.
- Arte própria do Rato Rei.
- Decidir se o plano de [HABILIDADES.md](planejamento/HABILIDADES.md) continua,
  agora que as cartas seguem o modelo do Vampire Survivors.
- Pendências de licença: fonte `septem`, faixas herdadas, *Pixel Bow Pack*
  ([TERCEIROS.md](TERCEIROS.md)).
