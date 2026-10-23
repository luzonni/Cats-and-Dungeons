# Em desenvolvimento (depois da 0.1.3-alpha)

- **Permissão para pular a abertura** (2026-10-23; `a20c558`).
  Por padrão, teclas, cliques e controle não pulam a abertura.
  Settings → Interface → Allow intro skip libera esse comportamento; a escolha
  é salva para os próximos inícios e pode ser desligada novamente.

- **Crédito da Runforce** (2026-10-23; `9f9b08a`).
  O nome Runforce substitui o marcador `[team name]` na abertura do jogo.

- **Tela cheia informa o modo ao Windows** (2026-10-23; `7a3b7c0`).
  Desligar o redimensionamento sozinho não bastou no uso real.
  `TelaCheiaDoWindows` usa `ITaskbarList2.MarkFullscreenWindow` para manter a barra
  atrás do jogo ativo; reaplica depois de mostrar/ativar o frame e ao aplicar
  vídeo, removendo a marca no modo janela. Validado também com o jogo completo,
  com capturas do sistema na abertura e no menu em 1920×1080.

- **Janela em primeiro plano ao iniciar** (2026-10-23; `7a3b7c0`).
  `FocoDoWindows` pede ativação nativa uma vez na abertura, com
  desbloqueio por Alt pressionado/solto se o pedido normal for recusado e nenhum
  modificador estiver pressionado, conforme a implementação de inicialização da
  Tao. Substitui a elevação temporária por `alwaysOnTop`, que não garantia
  ativação: a barra permanecia até o clique. Validado iniciando o jogo pelo
  Gradle, com `GetForegroundWindow` confirmando ativação, zero apertos recebidos
  pela abertura e capturas sem a barra. Alt+Tab e modo foto não repetem a tentativa.

> **Reconstruído a partir do histórico do git em 2026-10-21.** Nada disto saiu
> numa versão: não há tag depois de `v0.1.3-alpha`, e o `build.gradle.kts`
> continua em `0.1.3-alpha`. O número da próxima versão está **a confirmar**.
> Cobre os commits de 2026-09-07 a 2026-10-21 (até `8a9cbb8`, PR #31). Cada item
> cita o commit ou o PR de onde saiu.

## A corrida virou sem fim

A maior mudança do período. O jogo deixou de ser uma sequência de salas com
ondas e passou a ser uma corrida contínua de 30 minutos, no modelo do *Vampire
Survivors* (PRs #20 a #22, 2026-10-19).

- Campo aberto de 160×160 tiles gerado a cada corrida, no lugar das arenas
  desenhadas (`4519f3b`).
- Horda em fluxo contínuo de pequenos grupos, mais numerosa e mais frágil, que
  cresce com o relógio (`8b182ed`, `a7d714a`, `c530cf8`).
- Cercos, elites com aura e endurecimento da horda pelo tempo (`f2aa949`).
- Ratos que cercam o gato em vez de vir em fila (`403a491`).
- Caixotes e barris quebráveis com cura ou XP grande (`4d0bb6e`).
- Fúria Felina: onda de choque automática a cada 30 abates (`1091899`).
- Relógio de 30 minutos no HUD e o Rato Rei como chefe final (`c0956aa`).
- Rato Rei em três fases e base comum de chefe com barra de vida na tela
  (`d49ef5d`, `ed4c721`).
- Tela de vitória, com a escolha entre encerrar e continuar (`af0c19e`).

## Progressão

- Melhorias de atributo em cartas com quatro raridades (`1e46eac`, 2026-09-19).
- XP com subida de nível e escolha de atributo (`3410051`); depois, cartas
  sorteadas com raridade, reroll e skip (`97d949e`).
- Sorte em pontos, sem teto de porcentagem (`1ea577f`).
- Baús e caça-níquel (`f2aa949`), depois redesenhado com palco, holofotes e giro
  suave (`18aa302`). Baús oito vezes mais raros (`77217c9`).
- Armas extras orbitando o gato (`bb70683`) e três de área: Electric Arc, Quake e
  Meteor Rain (`12549bd`).
- Passivas, evoluções de arma e estatísticas da corrida (`abc4ba4`).
- Bônus próprio de cada gato e uma habilidade exclusiva: Volley do Finn
  (`bd458ab`), Retaliate do Muffin (`ad5e998`), Arcane Burst do Azrael
  (`c7109b0`), com texto por nível na carta (`6e048e0`).
- Ouro e upgrades permanentes entre corridas (`ddda5f2`), com loja na tela de fim
  de corrida (`b7baf55`).
- Dez conquistas guardadas entre corridas (`4a6cb3a`), tela própria a partir do
  troféu do menu (`ee09f48`, `72a338d`) e avisos no fim da corrida (`c946e61`).

## Combate

- Elemento escolhido para a corrida inteira, e não por arma, com animação de
  transmutação da arma (`1e46eac`). Na corrida sem fim a escolha aparece aos 45 s.
- Golpes de inimigo com aviso (`Investida`) para todos os inimigos (`1e46eac`).
- Armadura de inimigo, que absorve dano antes da vida (`1e46eac`).
- Dano dos inimigos crescendo por sala: +7% de vida e +4% de ataque (`1e46eac`).
- Uma família de arma por gato (`Classe`) (`1e46eac`).
- Dash por impulso, com rastro de imagens (`1e46eac`).
- 24 ticks de invulnerabilidade depois de levar dano (`1e46eac`).
- Arco da espada mirado, com curva suave e rastro em crescente (`b5a8d25`).
- Correções de mira e de projéteis (`ce55759`) e de pathfinding e coleta de XP
  (`8e0ba0e`).
- Esqueletos com teto de caveiras no ar e caveiras de vida mais curta
  (`8fe3ddf`).

## Mundo

- Conteúdo de masmorra e fluxo de combate (`baed76b`, 2026-09-07).
- Combate elemental (`16a12cb`, 2026-09-08).
- Arenas da cisterna inundada (`34bfcc4`) e sequência de chegada e de
  nascimento por portal (`0596098`), em 2026-09-09. Essas plantas deixaram de
  ser usadas quando a corrida virou sem fim.

## Save

- Corrida gravada entre salas, com três espaços (`1e46eac`).
- Tela `Saves` com gato, nível e tempo de cada espaço, retomar e apagar
  (`1e46eac`); o X de apagar alcançável pelas setas e pelo D-pad (`fa7debd`).
- Arquivos de save fora do controle de versão (`9db8220`).

## Visual e desempenho

- Sombras suaves, luz dinâmica e o pipeline OpenGL ligado de verdade
  (`9fbe52f`).
- Mundo e câmera interpolados entre ticks (`4067a47`); menus também, com 165 e
  240 FPS (`32c3f4c`).
- Ritmo de quadros uniforme e opção de VSync (`508384d`).
- Inimigos com contorno, clarão ao apanhar, movimento procedural e morte
  (`7384b5b`); corpo do gato com movimento e projéteis com rastro (`bb10c65`).
- O que fere o gato marcado em vermelho (`6fe4d3f`); XP como gema flutuante
  (`7a6909f`).
- Texto com contorno e sombra em todas as telas (`489b204`); popup de crítico
  (`85aa8bb`); ícones de elemento nítidos (`d72817a`); ícones novos de atributo
  (`426c0af`).
- Flash vermelho discreto na borda ao levar dano e pulso com pouca vida
  (`818845f`).
- Câmera: zoom até 70% (`4622a84`) e mais afastada por padrão (`df7408c`).
- Desempenho: menos alocação por quadro (`cd779b3`), sprites amigáveis à GPU e só
  os tiles visíveis desenhados (`f8e77a4`), trabalho fora da tela pulado e
  rastros na GPU (`b5fffd5`), ícones do caça-níquel em cache (`f50024d`).

## Interface e menus

- Menus e widgets refeitos (`978fa25`); HUD e menus refeitos (`3113aa1`).
- Tamanho de GUI no estilo do Minecraft: Auto, 1, 2 ou 3 (`b862ad6`).
- Menu principal anima a saída antes das opções (`3cb2ad0`).
- Opções mais espaçosas, destaque de aba deslizante e Back que sempre cabe
  (`f260ef7`).
- Ponteiro de mão sobre os botões (`2eb9880`).
- Correção do botão de comprar ao mudar o tamanho do HUD (`6ef59a7`).

## Controle

- Controle de Xbox via XInput, com movimento analógico (`e26ac28`, PR #25).
- B interage com o objeto mais próximo em jogo; diálogos de confirmação
  navegáveis (`4bab6c8`).
- Modos de mouse e de controle; o analógico direito espia com a câmera em vez de
  mover um cursor (`08e1772`).
- Foco por D-pad no inventário, nas lojas, na máquina de chiclete e nos upgrades
  do fim de corrida (`f356453`).

## Áudio

- Sons de impacto elementais, jingle de conquista e ronco do gato (`e366d19`).
- Os 50 arquivos de áudio na mesma régua de loudness (`2c3e113`).
- Curva logarítmica de volume com controle de Master (`c10a37c`).
- Música da arena que acompanha a horda, com calmaria antes do chefe e
  cruzamentos (`c26b3fe`).

## Ferramentas e projeto

- CI local e hook de pre-push (`b2e67a6`, `b9d3dcc`).
- Geradores de assets em `tools/` (`06c0e2d`, `65cd8ef`, `abba097`) e partículas
  elementais de terra e ar (`8d79484`).
- Modo bancada e medidor de quadros (`6000d1d`), com as opções `-Dcd.fim`
  (`8ade2df`), `-Dcd.rei` (`07e0b96`, `2400271`), `-Dcd.gato` e `-Dcd.habilidade`
  (`9fbb8c5`).
- Testes de curva de áudio, save, classes, melhorias e pathfinding (`05c9e2e`);
  depois de controle, conquistas, vitória, feedback de dano, grade de foco,
  música da arena, habilidades dos gatos e chefes. São 132 testes em 2026-10-21.

## Correções

- Textura faltando ao escolher fogo no meio da corrida: `swordfire` renomeado
  para `sword_fire` (`a0029e3`).

## Save da corrida e limpeza (2026-10-21, branch `docs/estado-atual`)

Esta seção não é reconstrução: descreve a mudança feita depois da revisão da
documentação.

- **Save da corrida sem fim completo.** O save passa a guardar o relógio da
  corrida, os abates, qual chefe vem a seguir, se o Rato Rei já nasceu, o compasso
  do cerco, os chefes vivos (tipo e fração de vida), níveis pendentes, trocas e
  pulos das cartas, carga da Fúria, números da tela final e a vitória. Antes,
  retomar zerava o relógio e devolvia o gato ao estado dos 45 s.
- **Quando grava:** ao entrar no campo, depois de cada escolha (elemento, carta de
  nível, prêmio do caça-níquel), a cada minuto de corrida e ao sair pela pausa.
  O aviso da pausa diz isso e avisa que os inimigos da tela não são guardados.
- **Retomar não cura mais.** O upgrade permanente Vitality era aplicado depois da
  vida gravada e enchia a barra.
- **Removido o que nenhum caminho do jogo alcançava:** o modo por ondas e salas
  (dificuldades `normal` e `hard`, plantas `cistern_1` a `6`, JSON de
  `maps/waves/`), a `Recompensa` de fim de sala, o vendedor de fim de sala, o
  `TrapDoor`, a `Door`, o `Crate` com loot, a `Chain`, o `MonarkMouse`, o
  `CryingCat`, o A\* e `Player.takeLevel`.
- **Dinheiro em espera:** a mecânica fica no código, marcada, sem uso dentro da
  corrida (`Seller`, `Store`, `Coin`, `GumMachine`).
- **Threads:** o `Repulsion` saiu da thread própria (que nunca terminava) para o
  tick do jogo; `resetWindow()` passa pela EDT.
- **Correções:** o relógio interno do `Game` contava 61 ticks por segundo; a
  morte do gato deixava o resto do tick rodando depois da troca de tela.
- O corte do Presságio "nas últimas salas" saiu junto com as salas: ele é sempre
  oferecido.
- Comentários que descreviam o fluxo por salas foram atualizados.

## Interface única (2026-10-21, branch `feature/menus-e-ui`)

Esta seção também não é reconstrução. Ver
[ARQUITETURA.md](../ARQUITETURA.md#interface).

- **Um sistema de menus.** Toda tela de menu (título, saves, seleção de gato,
  opções, pausa, conquistas, fim de corrida, vitória) estende `TelaDeMenu`: o
  mesmo título, a mesma área de conteúdo, um rodapé com as teclas do modo de
  entrada atual e a mesma entrada de 0,15 s. Esc e B sempre voltam.
- **Opções é uma tela só**, aberta da tela inicial e da pausa, com abas Video,
  Audio e Interface trocadas por Q/E ou LB/RB. Apply e Back alcançáveis pelo
  teclado e pelo controle.
- **Pilha de telas consertada.** "Main Menu" sempre volta à tela inicial;
  "Try Again" e "Main Menu" deixaram de empilhar uma tela nova a cada corrida.
- **A pausa congela de verdade:** o corpo dos bichos, o tremor da câmera, a aura
  das elites e o pulso da Fúria param, e a música abaixa.
- **Um hover e um foco** (`Realce`), tirados dos botões da tela inicial: cresce,
  brilha e clareia, sem contorno grosso. O foco de teclado e controle soma duas
  setas que respiram. Vale para botões, inventário, loja, abas, opções, saves,
  gatos, troféu e loja do fim de corrida. O item do inventário não encolhe mais
  sob o mouse.
- **Cartas de elemento iguais às de buff** (`MaoDeCartas`): mesmo tamanho,
  moldura, faixa, medalhão, tipografia, entrada, hover e teclas 1 a 3.
- **Transmutação da arma em 0,3 s**, sem tirar a arma da mão por dois segundos:
  estoura em branco, troca, um anel na cor do elemento abre e o nome da arma
  nova sobe sobre o gato. Antes eram 2,3 s.
- **Modo foto** (`-Dcd.foto`) para conferir telas por roteiro sem tocar nos
  saves nem nas configurações.
- **Pausa e Settings num painel só** (`Configuracoes`). A pausa deixou de ser
  uma coluna de botões com "Options" levando a outra tela: é o painel de
  Settings, com a aba Run na frente (Continue, Restart, Main Menu, Quit, resumo
  da corrida e o aviso do save) e as abas Video, Audio e Interface. Pela tela
  inicial, o mesmo painel abre sem a aba Run. "Options" passou a se chamar
  "Settings". O HUD some enquanto um menu está aberto sobre a corrida.
- **Tela de Status na tecla E.** O E deixou de abrir o inventário (desativado,
  não removido: o jogo não tem inventário como mecânica). A tela nova pausa a
  corrida e mostra os atributos finais com o bônus entre parênteses, a arma da
  mão com o elemento, as armas extras, os números da corrida e todos os buffs
  com nível, quantidade e total.
- **Portal dos inimigos em carmim.** O verde não vinha da arte. Agora é o
  vermelho das bandeiras, com um brilho pulsante por baixo para ler sobre
  qualquer chão; o gato e o vendedor chegam por um portal na luz da lua.
- **Level Up na paleta do projeto.** Título, halo, véu, moldura das cartas e
  faixa da build vêm da `Palette`, no lugar do dourado alaranjado solto.

## Vídeo e HUD (2026-10-22, branch `fix/video-e-hud`)

Correções de regressões de vídeo. Ver [ARQUITETURA.md](../ARQUITETURA.md#laço-principal).

- **FPS estável sem VSync** (`d9a163b`). No Windows o `Thread.sleep(1)` dormia
  até 19 ms com o timer padrão, e o limitador perdia o horário do quadro. O jogo
  pede um timer de 1 ms (`timeBeginPeriod`), a espera mede o próprio sono e gira no
  trecho final, e quadro e tick seguem prazos absolutos. Novo limite "Monitor"
  (padrão) e "Unlimited". Sem VSync a 240: de 148–224 FPS com desvio de até 4,7 ms
  para 240 FPS com desvio de 0,4 ms.
- **Sem piscar no Alt+Tab nem ao trocar o vídeo** (`bc3eb29`). A perda de
  superfície não recria mais o `BufferStrategy`, e a janela não é mais destruída a
  cada resolução ou F11. Tela cheia é janela sem bordas no monitor da janela, em
  todo sistema.
- **A resolução escolhida vale** (`d9089d9`). Ela é o tamanho da imagem do jogo,
  escalada para a tela com tarjas quando não coincide com o canvas: em tela cheia,
  e em janela que não cabe. A lista mostra só o que o monitor comporta, mais a
  resolução dele.
- **HUD sem sobreposição** (`8c561a7`). A escala do HUD fica limitada ao que cabe
  na resolução atual, e o "+" de Settings para no teto.
- **Portal anima no tick** (`2b4bdca`): girava mais rápido a 240 FPS.
- **Gráfico de tempo de quadro no F3** (`663a0db`).
- **VSync ignorado detectado em 20 quadros** (`b6b8ae9`), e não em um segundo:
  ao abrir o jogo, no F11 e ao ligar o VSync, o primeiro segundo rodava solto a
  400+ FPS. Em Settings, limites acima da taxa do monitor vêm marcados com "!".

## A escolha de elemento como o momento da corrida (2026-10-22, branch `feat/destaque-do-elemento`)

A escolha de elemento acontece uma vez por corrida e decide a corrida inteira, mas
era apresentada como um level up. Agora ela tem uma cerimônia própria. Ver
[ARQUITETURA.md](../ARQUITETURA.md#escolha-de-elemento-a-cerimônia-escolhadeelemento).

- **Antecipação** (`2e93761`): o jogo congela, o HUD some, um véu e uma vinheta
  fecham a tela, dois anéis saem do gato e o título cresce. As cartas sobem uma a
  uma. Enter, Espaço, Esc, clique, A ou B pulam a cerimônia, sem escolher.
- **Cartas em destaque** (`bb132ca`): moldura dupla na cor do elemento, pedras nos
  cantos, brilho, aura e partículas no jeito de cada elemento; o foco intensifica.
- **Áudio** (`a5ac699`, `2b61806`): stinger, trilha própria em cruzamento com a da
  arena, som do elemento no foco e confirmação por elemento. Sons sintetizados no
  projeto (`tools/GenSonsDaEscolha.java`), provisórios no timbre.
- **Confirmação** (`5071c2d`): clarão na cor do elemento, explosão de partículas e
  a carta voando até o gato, onde a arma se transmuta; volta ao jogo em 0,6 s.

## Escala da interface proporcional à resolução (2026-10-22, branch `feat/escala-da-hud`)

Ver [ARQUITETURA.md](../ARQUITETURA.md#escala-da-interface-e-layout-do-hud).

- **Um sistema de escala** (`ce50477`). HUD size era um multiplicador cru de
  pixels, GUI size subtraía passos e Margin era em pixels absolutos: o mesmo valor
  ocupava o dobro da tela em 1280 do que em 2560. Agora tudo sai de uma referência
  de 640×360, em por cento (50% a 200%), multiplicado e arredondado uma vez. 100%
  ocupa a mesma fração em 720p, 1080p, 1440p e 4K. Novos: Interface size, Menu
  size, HUD size, Margin em pixels de referência e Reset interface; os valores
  antigos migram sozinhos.
- **HUD por âncoras** (`3e54d24`). Cada peça tem âncora, deslocamento e tamanho
  num lugar só (`LayoutDoHud`), e o teto da escala é o maior em que nada sai da
  tela nem se cruza. F7 desenha as caixas. Teste cobre a matriz de resoluções.
- **Barras arrastáveis e menu menor** (`3c84675`). Os valores numéricos de
  Settings viram uma barra no estilo do Forza Horizon, que se arrasta com o mouse
  e anda com as setas; os de sim/não e de lista continuam com botões. Os tamanhos
  andam de 5 em 5% (eram 25%) e o zoom da câmera também. O Menu size 100% passa a
  ser o antigo 75% (2 em 720p, 3 em 1080p), que é o tamanho escolhido nos testes.
- **Tamanhos em passos, sem piscar** (`894f483`). Arrastar o tamanho do menu
  fazia a tela piscar: a escala nova mudava a barra debaixo do mouse, que pegava
  outro valor, a cada quadro. E com escala inteira os 5% quase nunca mudavam
  nada. Menu size e HUD size viram blocos, um por tamanho que a tela comporta
  (Small, Default, Large...), aplicados ao soltar; o Interface size sai; a barra
  dos valores contínuos ganha contorno e preenchimento, como os toggles; e os
  menus passam a exigir 384×224 pixels de arte, para o Resume nunca encostar no
  rodapé.
- **Subtítulo da escolha de elemento legível** (`878ebbd`). O "It shapes your
  whole run" ficava entre o título e as cartas, e o brilho e os cantos da carta
  em foco passavam por cima dele. As cartas descem um pouco, o título sobe, e o
  subtítulo passa para a fonte dos textos das cartas, num tom mais claro.
- **Fim de corrida mais limpo** (`2825c39`). A tela de fim de corrida passa a ter
  fundo preto, sem a ilustração do título por trás. As linhas da loja ganham
  altura, os níveis se alinham ao preço, a dica vai em duas linhas (numa só
  vazava do painel), e os avisos de conquista saem depois de 4 s em vez de
  cobrir o ouro e a loja em 720p.
- **Fim de corrida na identidade visual** (`1aa2b63`). Painéis, loja, avisos de
  conquista e título passam para as cores da Palette e para o painel padrão dos
  menus (contorno, corpo fundo e cabeçalho como a aba ativa). Eram cabeçalhos
  ciano e amarelo, corpo em gradiente roxo e cantos arredondados.
- **Status fora da arena não derruba mais o jogo** (`ac7b298`). Abrir o Status (E)
  na sala do começo fechava o jogo: o bloco Run pedia a wave de uma arena que não
  existe. Agora a linha de abates só aparece dentro da arena.
- **Seleção de gato mais leve** (`ffb9ca2`). A tela refazia a medida do texto e a
  cópia do sprite a cada quadro. Agora guarda as duas, e o `FontHandler` guarda as
  fontes derivadas para todas as telas. Em 1080p sem VSync, o 1% mais lento foi de
  123–145 fps para 450–500 fps e o pior quadro de 12 ms para 4 ms.
- **Status rola para mostrar todos os buffs** (`84c446e`). A coluna que não cabia
  terminava em "+N more". Agora cada coluna rola pelas setas, pelo D-pad e pela
  roda do mouse, com avisos do que há acima e abaixo. Para testar:
  `-Dcd.buffs` (`7c97ff3`) dá um nível de cada buff na bancada.
- **Habilidade do gato dentro da ficha** (`c306e9b`). Na seleção de gato, papel e
  habilidade numa linha só passavam da chapa (o Azrael); agora a habilidade desce
  para a linha de baixo quando não cabe.
## O baú no lugar da roleta (2026-10-22, branch `feat/bau-no-lugar-da-roleta`)

Ver [ARQUITETURA.md](../ARQUITETURA.md#baú-a-abertura-aberturadobau-e-a-recompensa-recompensadobau).

- **Regra separada da tela** (`374b5c0`). O sorteio, a evolução garantida, a
  contagem e a entrega saíram da `Roleta` para `RecompensaDoBau`, com testes. Nada
  mudou no que o baú dá.
- **Arte e som** (`e7f2b90`). Quadros do baú abrindo, remapeados do DungeonTileset
  II (CC0) para a paleta do baú do jogo, e sons sintetizados: tremor, tampa e uma
  revelação por grau.
- **O baú abre na tela** (`b304cde`). Clique para abrir; o baú treme e acende na cor
  do grau, a tampa abre e o prêmio sobe de dentro. Enquanto espera, uma prévia
  mostra o que pode sair. Pular leva direto ao prêmio. Da tela aberta até poder
  coletar: 1,28 s (0,65 s pulando), contra 3,28 s (1,18 s) da roleta.
- **Roleta desligada** (`c9dec42`). O baú do chão abre a tela nova; a `Roleta` ficou
  no código, sem porta de entrada, e `-Dcd.roleta` abre o baú.
- **Sons de chegada e da prévia** (`c34ef71`, `693e3e7`). O baú soa ao aparecer na
  tela, e cada ícone da prévia dá um tic no grau dele (comum grave e seco,
  lendário agudo e brilhante), com a moldura na cor do grau. As melhorias da
  prévia vêm com o grau sorteado pelas chances reais.

## Abertura do jogo (2026-10-22, branch `feat/intro`)

Ver [ARQUITETURA.md](../ARQUITETURA.md#abertura-do-jogo-abertura).

- **Músicas em segundo plano** (`2f53632`). Carregar as faixas levava 3,4 s antes de
  a janela existir; agora elas carregam por trás, e uma faixa pedida antes de chegar
  toca quando ficar pronta.
- **Janela preta antes do primeiro quadro** (`8373da1`), sem o clarão do fundo
  padrão.
- **Logo e sons da abertura** (`015d2a7`). O letreiro extraído da ilustração numa
  camada própria e o ar, o crédito e a logo sintetizados.
- **A abertura** (`5f7e14a`). Do preto ao crédito da equipe, à logo e à tela inicial,
  que surge em volta da logo; os botões entram um a um. Completa na primeira vez
  (4,4 s), curta depois (1,6 s), pulável com qualquer tecla, clique ou botão. A janela
  aparece em 1,2 s, antes em 5 s.
- **Abertura mais lenta** (`d0aee84`, `65d5a17`). Depois de jogar: a completa passou
  de 4,4 s para 7,4 s e a curta de 1,6 s para 2,7 s, com tempo para ler o crédito e
  ver a luz atravessar a logo. O ar de fundo dura 8 s para acompanhar.
- **Abertura completa recorrente** (`a20c558`). Os 7,4 s
  se repetem a cada início, sem consultar nem gravar `introVista`. A curta de 2,7 s
  fica só para `-Dcd.intro=curta`; o pulo depende de Allow intro skip.

## Tela de Controles (2026-10-23, branch `feat/tela-de-controles`, alteração local, commits a confirmar)

Ver [ARQUITETURA.md](../ARQUITETURA.md#ações-e-a-tela-de-controles).

- **Fonte única das ações** (`Acao`). Cada ação diz as teclas virtuais que lê; a
  tecla física sai de `KeyBoard.Keys` e o botão, do mapa do `Gamepad`. As ações de
  jogo (pausar, Status, interagir, dash, usar e largar item, tela cheia) passaram
  a ler por ela, sem mudar nenhuma tecla. O `AcaoTest` falha se o código ler uma
  tecla que a tela não mostra.
- **Aba Controls no Settings**, pela tela inicial e pela pausa. Linha Device com
  duas visões, abrindo na do último aparelho usado. Teclado: lista por grupo com o
  desenho de cada tecla, WASD e setas em cruz. Controle: o Xbox One em pixel art
  com uma linha de cada botão até o rótulo, e um seletor In a run / In menus que
  troca o papel mostrado; botão sem papel no contexto sai esmaecido. Foco ou mouse
  acendem a peça e o rótulo.
- **Desenho do controle** a partir da carcaça do Xbox One do pacote do lightUP
  (uso livre; modificação e redistribuição **a confirmar**), na paleta do jogo, com
  volume esculpido, botão do meio liso no lugar do logotipo e as peças pintadas em
  código por cima.
- **Glyphs da Kenney** (Input Prompts Pixel 16×, CC0) para teclas, mouse e botões,
  com o contorno afinado para a paleta. O **rodapé** dos menus passou a usá-los, no
  teclado e no controle.
- **Painel do Settings mais largo** (240 para 288 px nativos) e mais alto, igual em
  todas as abas, para o desenho do controle e a lista do teclado caberem.
