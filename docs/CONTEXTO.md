# Contexto do projeto

Ponto de partida para quem chega sem saber nada — pessoa ou agente. Lendo só
este arquivo dá para retomar o trabalho; os outros documentos aprofundam.

Estado descrito: **2026-10-21**, commit `8a9cbb8` (merge do PR #31). Itens
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
| Testes | JUnit 5, headless — 17 classes, 132 testes passando em 2026-10-21 |
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
│   └── objects/               entidades, tiles, partículas, física, A*
│       └── entities/          Player, Bonus, Arsenal, ArmasDeArea, Furia,
│                              Habilidades, enemies/, furniture/, utilities/, NPCs/
└── menus/                     Menu, Saves, Personagens, Options, Pause,
                               GameOver, Vitoria, TelaDeConquistas
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
| Partida | `game/Game.java` |
| Corrida sem fim, horda, chefes | `game/map/arena/Waves.java` (`tickSemFim`) |
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
- Trabalho entra por branch e PR no repositório `luzonni/Cats-and-Dungeons`.

## 6. Estado das funcionalidades

✅ pronto · 🟡 parcial · 🧪 só começado · ⚰️ sem uso (código morto ou inalcançável)

| Funcionalidade | Estado | Nota |
|---|---|---|
| Menu, opções (vídeo, áudio, interface), pausa | ✅ | |
| Seleção de personagem (3 gatos) | ✅ | |
| Três espaços de save, retomar e apagar | ✅ | ver o problema da corrida sem fim abaixo |
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
| Save no meio da corrida sem fim | 🟡 | só grava ao entrar e após a escolha de elemento |
| Dinheiro (moedas) dentro da corrida | 🟡 | começa com 100; nada rende moeda; só a antecâmara vende |
| Plano de cartas com encaixes e status (HABILIDADES.md) | 🧪 | nenhuma fase implementada |
| Arenas por ondas e salas (`normal`, `hard`, `cistern_1..6`) | ⚰️ | o jogo só entra na dificuldade 0, que é a corrida sem fim |
| Recompensa de fim de sala (3 cartas de `Melhoria`) | ⚰️ | só abre quando as ondas acabam, o que não acontece na corrida sem fim |
| Vendedor ao fim da sala, alçapão | ⚰️ | mesmo motivo |
| `MonarkMouse` | ⚰️ | sem sprite; fora da horda |
| `CryingCat` | ⚰️ | sprite em WebP com extensão `.png`; nunca nasce |
| Máquina de chiclete (`GumMachine`) | ⚰️ | nenhum mapa a coloca |
| Moeda (`Coin`) | ⚰️ | nada a cria |

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

## 8. Onde estão os números de balanceamento

| O quê | Onde |
|---|---|
| Atributos base dos gatos | `resources/.../players/*.json` |
| Bônus de cada gato | `Bonus.java` (`area`, `critico`, `aplicarNoInicio`) e `Bonus.habilidade` |
| Curva de XP | `Player.getXpLength` — `15 + 10·n + 0,8·n²` |
| Sorte → chance; chance de baú | `Player.chanceDeSorte`, `Player.chanceDeBau` |
| Dash, invulnerabilidade | `Player` (`DASH`, `INVULNERAVEL = 24`) |
| Ritmo da horda, tipos por minuto, chefes, teto de vivos | `Waves.java` (`ESCALA`, `CHEFES`, `MAX_VIVOS`, `tickSemFim`) |
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

## 9. Próximos passos conhecidos

Tirados do código e do [roadmap](planejamento/ROADMAP.md); nenhum tem dono
definido:

- Decidir o destino do código das salas por ondas (normal/hard, `Recompensa`,
  vendedor de fim de sala): integrar à corrida sem fim ou remover.
- Fazer o save da corrida sem fim guardar o relógio e o progresso do meio da
  corrida (ver [ARQUITETURA.md](ARQUITETURA.md#pontos-frágeis-conhecidos)).
- Dar uso ao dinheiro dentro da corrida ou tirá-lo.
- Arte própria do Rato Rei; sprites de `MonarkMouse` e `CryingCat`.
- Tirar o `Repulsion` da thread paralela.
- Decidir se o plano de [HABILIDADES.md](planejamento/HABILIDADES.md) continua,
  agora que as cartas seguem o modelo do Vampire Survivors.
- Pendências de licença: fonte `septem`, faixas herdadas, *Pixel Bow Pack*
  ([TERCEIROS.md](TERCEIROS.md)).
