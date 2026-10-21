# Em desenvolvimento (depois da 0.1.3-alpha)

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
