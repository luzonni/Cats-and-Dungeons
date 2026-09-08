# Bibliotecas de terceiros

O código e os assets de *Cats & Dungeons* são proprietários (ver `LICENSE`).
As bibliotecas abaixo **não** são: cada uma permanece sob a própria licença.

## Resolvidas do Maven Central

Declaradas em `build.gradle.kts`.

| Biblioteca | Versão | Licença | Uso |
|---|---|---|---|
| [json-simple](https://github.com/fangyidong/json-simple) | 1.1.1 | Apache-2.0 | Leitura de `config.json` e dos JSON de mapas e jogadores |
| [OSHI](https://github.com/oshi/oshi) | 6.12.0 | MIT | Informações de hardware no overlay de depuração (F3) |
| [JNA](https://github.com/java-native-access/jna) | 5.19.1 | Apache-2.0 / LGPL-2.1 | Dependência do OSHI |
| [SLF4J](https://www.slf4j.org/) | 2.0.19 | MIT | Fachada de log exigida pelo OSHI |

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
| [16x16 DungeonTileset II](https://0x72.itch.io/dungeontileset-ii) | 0x72 (recoloração de GrafxKid) | CC0 1.0 | Base das armas |
| [16x16 Assorted RPG Icons](https://opengameart.org/content/16x16-assorted-rpg-icons) | Shade | CC0 1.0 | Desenhos das espadas e as rampas elementais |
| [16x16 Weapon RPG Icons](https://opengameart.org/content/16x16-weapon-rpg-icons) | Shade | CC0 1.0 | Desenhos dos machados e cajados |
| [16x16 weapon sprites free](https://opengameart.org/content/16x16-weapon-sprites-free) | Bennyboi_hack | CC0 1.0 | Desenhos dos arcos |
| [Tiny gun icons (16x16)](https://opengameart.org/content/tiny-gun-icons-16x16) | congusbongus | CC0 1.0 | Desenho do laser |
| [Ninja Throwing Items Kit](https://opengameart.org/content/ninja-throwing-items-kit-32x32) | kungfu4000 | CC0 1.0 | Desenho da kunai |
| [16x16 RPG Items (DB32)](https://opengameart.org/content/16x16-rpg-items-db32) | ARoachIFoundOnMyPillow | CC0 1.0 | Varinhas de agua (orbe) e de fogo (tocha) |
| Pixel Bow Pack | **a confirmar** | **⚠ a confirmar** | Desenhos dos arcos |

As **variantes elementais** das armas são montadas por `tools/GenElementais.java`.

**Cada variante é um desenho diferente**, e não a mesma arma pintada de outra cor.
A primeira versão recolorava um sprite só, e o resultado era o que se esperaria:
sete espadas idênticas em sete cores, que não dizem nada. Os pacotes trazem
dezenas de silhuetas por família — trinta modelos de machado, trinta de cajado,
nove de espada, seis de arco —, então cada elemento leva um modelo próprio.

O gerador mexe em três coisas, para as armas de fora não destoarem das de dentro:
o contorno vira o `#222222` que todo o arsenal usa; o **metal** é remapeado para a
rampa do elemento; e a **madeira** do cabo é remapeada para os dois tons que o
jogo já tinha. Metal e madeira são separados pela saturação — o aço é cinza, o
cabo é marrom —, e não por posição, que mudaria de sprite para sprite. Arma que
só tem madeira, como os arcos do Bennyboi, recebe a rampa no desenho inteiro:
senão um arco de fogo sairia marrom, igual ao comum.

As rampas foram medidas na folha do Shade, que traz o mesmo desenho em oito
colorações. Água e ar não existem lá e são derivadas por matiz e saturação,
mantendo o mesmo desenho de sombra.

> **⚠ PENDÊNCIA DE LICENÇA — arcos.** O *Pixel Bow Pack* (`tools/assets/fonte/arcos/`)
> chegou como um zip sem nenhum arquivo de licença, readme ou crédito. O desenho é
> melhor que o anterior e por isso entrou, mas **a origem e a licença precisam ser
> confirmadas antes de qualquer distribuição pública do jogo** — sem isso não dá
> para saber se pode ser redistribuído. É a mesma pendência que já existe para a
> fonte `septem`.

Os arcos são desenhados em **24×24** e os itens do jogo têm 16. Reduzir não
funciona: a corda tem **um pixel** de largura, e nenhuma redução para dois terços
preserva uma linha de um pixel — tirar uma linha a cada três virou tracejado, e
amostrar pelo vizinho mais próximo também. Não é questão de achar o algoritmo
certo, é falta de espaço. Por isso eles são **aparados**, não reduzidos: cada pixel
mantém o tamanho, a corda continua contínua, e o preço é cortar as pontas das
hastes — igualmente dos dois lados, para o arco não ficar torto.

**Duas armas vêm de fora dessas três folhas, porque nenhuma delas tinha o item.**
O **laser** era o rifle do Bennyboi tingido de vermelho — arte de artista, sim, mas
de pólvora, e o vermelho por cima ainda comia o contraste entre cano e coronha.
Agora é um blaster de verdade, do pacote de armas do congusbongus. A **kunai** era
uma faca (o `weapon_knife` do DungeonTileset II) e depois uma adaga qualquer; a de
agora tem lâmina em folha e **anel** no punho, que é o que faz ler kunai.

Essas duas são desenhadas **deitadas**, apontando para a direita — é como os
pacotes de arma de fogo e de item solto desenham, ao contrário dos de RPG, que usam
a diagonal. Por isso elas usam `Rotate.PARA_DIREITA` e não `Rotate.DIAGONAL`.

**Consequência assumida:** a espada, o machado, o arco e o cajado **comuns**
também passaram a vir desses pacotes. A alternativa seria metade da família em pé
(a arte antiga, do DungeonTileset II) e metade na diagonal, que é como todo pacote
de ícone 16x16 desenha — e essa mistura salta aos olhos numa hotbar. Os PNGs
antigos estão em `tools/assets/itens_original/`. Por isso existe
`Rotate.DIAGONAL` e `Item.grausDaArte()`: a pose de porte foi calculada para arma
em pé, e sem descontar os 45 graus da arte uma espada de uma mão apareceria quase
deitada na mão do gato.

O **escudo** (`sprites/items/shield.png`) é desenhado por `tools/GenDesenhados.java`,
não baixado: nenhum dos pacotes acima tem escudo.

O **tridente** era desenhado ali também, e não é mais. Ele foi feito à mão porque a
pesquisa de arte afirmava que o pacote do Shade "anuncia um tridente e não tem".
**Tem trinta**, num grupo inteiro (colunas 21–23) que ninguém abriu. Fica o
registro: a pesquisa estava errada, e a verificação que faltou foi olhar a folha.
A **kunai** tem a mesma história — passou por três substitutos antes de eu abrir o
grupo de lâminas curtas da mesma folha, onde havia uma com lâmina em folha e anel
no punho.

As variantes **lendárias** vêm da folha `gold-weapons.png`, e não da nossa rampa
aplicada sobre o aço: são quatro folhas no pacote (bronze, ferro, aço e ouro) e por
muito tempo só a de aço foi usada. O brilho do ouro cai onde o desenhista decidiu. O único candidato do Tiny Dungeon
(`tile_0102`) é um quadrado com um chanfro de dois pixels, e recolorido para a
paleta do jogo lia como caixa. E o pacote só tem uma lança: o `trident.png` que
saiu dela era a ponta da lança encurtada — uma cabeça maciça num cabo, ou seja,
um martelo. Tridente sem três dentes não é tridente, e nenhum ajuste de tamanho
conserta a falta de dente. As silhuetas são nossas; a paleta é a mesma já medida
dos PNGs do DungeonTileset II, para os dois não destoarem do resto do arsenal.

O **DungeonTileset II** também não está versionado, pela mesma razão. Baixe-o em
`tools/assets/fonte/` e rode:

```
java tools/GenItens.java
```

As armas do jogo são derivadas, não cópias. O pack foi desenhado para heróis de
**20px de corpo** e as armas dele têm até 37px de altura; o gato tem 13px, e o
motor limita o quadro de item a 16x16 (`Item.java` conta os quadros por
largura/16). `tools/GenItens.java` encurta cada arma **removendo linhas
redundantes** — a lâmina e a haste são linhas repetidas que não carregam
informação; guarda, gume, ponta e punho carregam, e ficam pixel a pixel como o
autor desenhou. Reduzir a escala e cortar o cabo foram testados antes e
destroem o desenho; o javadoc do gerador registra os três experimentos.

O texto da licença, como o autor a declara:

> You can use this tileset for whatever you like. Credit is not necessary, but
> if you create something using this tileset I'd be happy to see your work.

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
| [Spooky Dungeon](https://opengameart.org/content/spooky-dungeon) | `audio/fight_spooky.ogg` | Memoraphile @ You're Perfect Studio | CC0 1.0 (também oferecida em CC-BY 4.0 e OGA-BY 3.0) | Trilha de combate "Spooky" |
| [8 Bit Battle Loop](https://opengameart.org/content/8-bit-battle-loop) | `audio/fight_8bit.ogg` | Wolfgang_ (Theodore Kerr) | CC0 1.0 | Trilha de arena "8-bit" |
| [8 Bit RPG Battle/Encounter Theme](https://opengameart.org/content/8-bit-rpg-battleencounter-theme) | `audio/fight_rpg.ogg` | Wolfgang_ (Theodore Kerr) | CC0 1.0 | Trilha de arena "RPG" |
| [Boss Battle Music](https://opengameart.org/content/boss-battle-music) | `audio/fight_boss.ogg` | Juhani Junkala (Subspace Audio) | CC0 1.0 | Trilha exclusiva de chefe |

**Cave Theme** foi reencodada em Vorbis com `-2,5 dB` de ganho, para o volume
médio bater com o das outras faixas (−16,6 dBFS) e sobrar margem de pico.
Substituiu `room.ogg`, que era clara e agitada demais para uma masmorra — e cuja
origem, ao contrário desta, nunca foi confirmada.

**Loopable Dungeon Ambience** — vento e goteiras — foi reencodada com `-6 dB` e
em qualidade menor, ficando em −35,8 dBFS de média. Ela toca JUNTO com a música do
menu, não no lugar dela: o nível baixo é o que a mantém como ambiente e não como
segunda faixa disputando com a primeira. Como o nível está gravado no arquivo, ela
continua obedecendo ao controle de música das opções.

**As trilhas de arena são quatro, e a escolha é do jogador** — Options > Audio >
Battle music. A original (`fight.ogg`) é animada e não combina com a masmorra,
mas tirá-la seria decidir por quem gosta dela; ela ficou como uma das opções.
*Spooky Dungeon* é chiptune escura, *8 Bit Battle Loop* e *RPG Battle Theme* são
chiptune de briga com loop limpo — as três fecham com a arte do jogo. O menu toca
a faixa escolhida na hora: escolher música lendo o nome numa lista não funciona.

**A trilha de chefe fica de fora dessa lista, de propósito.** `fight_boss.ogg`
entra sozinha quando um `Enemy.chefe()` está vivo na arena e sai quando ele morre.
Música de chefe que toca o tempo todo deixa de anunciar chefe nenhum: o que a
torna especial é ela *não* tocar no resto do tempo. Ela também tem loop sem
emenda, o que importa numa faixa de dois minutos que repete.

Todas foram reencodadas em Vorbis a 128 kbps, 44,1 kHz, só para casar com o
formato das outras.

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
> confirmar a origem e os direitos de cada uma antes de publicar. As demais da
> seção acima já estão resolvidas. Para o combate a pendência deixou de ser
> bloqueante: as quatro faixas novas são CC0 e qualquer uma delas pode virar o
> padrão se `fight.ogg` não puder ser distribuída.
