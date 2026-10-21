# Inimigos e bosses

A coluna **Classe** aponta o arquivo correspondente em
`src/main/java/com/retronova/game/objects/entities/enemies/`.

Revisado em **2026-10-21** contra o código. Cada inimigo tem duas partes: o
**plano**, como foi escrito antes, e **no jogo**, o que o código faz hoje. Os
especiais do plano que não aparecem no código estão marcados como não
implementados.

## Como a horda funciona hoje

Tudo nasce do `Waves`, na corrida sem fim de 30 minutos (ver
[ARQUITETURA.md](../ARQUITETURA.md#fluxo-de-uma-partida)):

| Minuto | Entra na horda |
|---|---|
| 0 | Rato Zumbi |
| 1 | Slime |
| 2 | Esqueleto (no máximo `2 + minuto` vivos, até 12) |
| 3 | Rato Escudeiro |
| 4,5 | Rato Vampiro |
| 6 | Rato Explosivo |
| 5, 10, 15, 20, 25 | chefe: Rei Gato Amaldiçoado e Brinquedo de Gato, alternados |
| 30 | **Rato Rei**, chefe final |

O sorteio favorece os tipos liberados mais recentemente. Regras gerais:

- **Golpe com aviso.** Todo inimigo corpo a corpo bate pela `Investida`, em
  quatro ritmos (rápida, leve, média, pesada): ele para, avisa e só então avança.
- **Endurecimento pelo tempo.** Um degrau a cada 24 s; cada degrau dá +7% de
  vida e +4% de dano (`Enemy.escalarPara`).
- **Vida de horda.** Os comuns nascem com 50% da vida, subindo até 100% no
  minuto cinco. Chefes nascem inteiros.
- **Elite.** Chance de `0,2% + 0,8% por minuto`, até 6%. Elite tem 5× a vida,
  6× o XP, +30% de dano, aura dourada, e sempre larga um baú.
- **Armadura.** Vida extra que não olha o tipo do dano, mostrada em amarelo
  sobre a barra de vida. Só chefes e o `MonarkMouse` têm.
- **Cerco.** A partir de 1min15, a cada 40 s, um anel de 8 a 30 zumbis em volta
  do gato, com o aviso "SURROUNDED!".
- **Coleira.** Inimigo a mais de 20 tiles do gato não age, e é trazido de volta
  para perto, fora da tela. No máximo 170 vivos ao mesmo tempo.
- **Quebráveis** (`Quebravel`): caixotes e barris são inimigos de 16 de vida
  para as armas acertarem neles, mas não seguram a horda nem atraem a mira.
  Largam cura (40%) ou XP grande.

## Inimigos comuns

### 1. Rato Explosivo — `MouseExplode`

Corre em direção ao jogador até explodir.

- **Plano:** dano de 10 a 15 por explosão; sem resistência; morre com um único
  ataque; explode ao chegar perto do jogador.
- **No jogo:** 200 de vida, velocidade 1,5, explosão de 33, 12,4 de XP. Acende o
  pavio perto do gato e explode. O "morre com um ataque" não vale com 200 de
  vida — **a confirmar** se o número é intencional.

### 2. Rato Escudeiro 🛡️ — `MouseSquire`

Gato guerreiro com um pequeno escudo preso à cabeça como capacete. Avança
contra o jogador e rebate projéteis com a cabeça.

- **Plano:** dano de 6 a 8 (investida com o escudo); resiste a ataques frontais
  e projéteis; fraco a ataques laterais ou pelas costas; abaixa a cabeça para
  refletir projéteis.
- **No jogo:** 70 de vida, velocidade 2,0, golpe médio de 8, resiste 60% a fogo,
  9 de XP. **Não implementado:** a defesa frontal e o reflexo de projéteis.

### 3. Rato Vampiro 🦇 — `MouseVampire`

Sugador de energia vital, recupera vida ao atacar.

- **Plano:** dano 8; resiste a ataques físicos; fraco a magia; fica invisível
  por alguns segundos ao atacar, e se for atingido perde a invisibilidade e
  recebe dano extra.
- **No jogo:** 60 de vida, velocidade 1,7, golpe rápido de 4, 7 de XP.
  **Não implementados:** roubo de vida, invisibilidade e resistências.

### 4. Esqueleto Feiticeiro — `Skeleton`

Lança orbes de energia maligna.

- **Plano:** dano 8 por orbe; resiste a dano mágico; fraco a corpo a corpo; lança
  até 3 orbes.
- **No jogo:** 60 de vida, velocidade 2,0, resiste 50% a fogo, 6 de XP. Atira
  caveiras (`Skull`) a até 7 tiles, com recarga de 110 ticks; o número de caveiras
  no ar tem teto. A resistência a perfuração e a veneno foi tirada de propósito: o
  Finn, preso ao arco, não tinha resposta a ela.

### 5. Slime Multiplicador — `Slime`

Divide-se ao ser atacado.

- **Plano:** dano e fraqueza a definir; sem resistência; divide-se em slimes
  menores ao morrer.
- **No jogo:** 10 de vida, velocidade 4,5, golpe rápido de 3, 6 de XP.
  **Não implementada:** a divisão.

### 6. Rato Zumbi — `Zombie`

O mob mais básico: fraco e sem resistência.

- **Plano:** pouco dano, sem resistência, sem especial.
- **No jogo:** 40 de vida, velocidade 1,2, golpe pesado de 4, resiste 50% a fogo,
  5 de XP. É o tipo dos cercos.

### 7. Rato Monarca — `MonarkMouse`

Ver em Bosses. No código é um inimigo comum com armadura (120 de vida, 60 de
armadura), mas está **fora da horda**: o sprite `monarkmouse.png` não existe e o
jogo caía quando ele nascia.

## Bosses

Todo chefe herda de `Chefe`, que dá fases por fração de vida e a barra de vida
na tela (`BarraDeChefe`). A trilha de chefe entra quando um chefe está vivo.

### 1. Brinquedo de Gato Assombrado — `CatToyBoss`

Pelúcia possuída por uma entidade maligna. Os olhos brilham em vermelho
enquanto ele se move sozinho pelo ambiente.

- **Plano:** dano 7 (pancadas sombrias); resiste a ataques físicos normais;
  fraco a fogo e ataques mágicos; desmancha-se em pedaços ao ser atingido e se
  reconstrói.
- **No jogo:** "Toy Cat", 900 de vida, 200 de armadura, velocidade 4, golpe
  pesado de 18. Duas fases: na segunda fica 25% mais rápido. **Não implementado:**
  desmanchar e reconstruir.

### 2. Rei Gato Amaldiçoado — `KingCursedCatBoss`

- **Plano:** muito resistente a ataques físicos; fraco a ataques mágicos; invoca
  gatos corrompidos.
- **No jogo:** "Cursed Cat King", 1300 de vida, 300 de armadura, velocidade 4,
  golpe pesado de 20. Duas fases: na segunda fica 25% mais rápido.
  **Não implementado:** a invocação.

### 3. Rato Monarca — `MonarkMouse`

- **Plano:** resiste a veneno e a ataques físicos leves; invoca ratos pequenos em
  grande quantidade; desaparece por um momento e reaparece em outra parte do
  mapa.
- **No jogo:** sem sprite e fora da horda (ver acima). **Não implementados:**
  invocação e teletransporte.

### 4. Gato Chorão — `CryingCat`

Sem especificação escrita até o momento.

- **No jogo:** 1500 de vida, velocidade 4, golpe médio de 22. Nunca nasce: não
  está na horda nem em nenhum mapa.

> ⚠️ O sprite `cryingcat.png` continua corrompido: é um arquivo **WebP** com
> extensão `.png`, e o `ImageIO` não consegue lê-lo. O boss carrega com a
> textura de erro. Conferido em 2026-10-21.

### 5. Rato Rei — `RatoRei` (chefe final)

Novo desde a versão anterior deste documento. Aparece aos 30 minutos; derrotá-lo
abre a tela de vitória, com a escolha entre encerrar a corrida e continuar.

- 6000 de vida, 400 de armadura, velocidade 2,6, golpe pesado de 40 com alcance
  de 2,4 tiles, 300 de XP.
- **Fase 1, perseguição** (acima de 66%): só ele e o golpe pesado.
- **Fase 2, a corte** (de 66% a 33%): chama 3 ratos a cada 7 s, até 14.
- **Fase 3, a fúria** (abaixo de 33%): 30% mais rápido e, a cada 5 s, para e
  solta um anel de 14 caveiras depois de 0,75 s de aviso no chão.
- **Sem arte própria:** usa o sprite do escudeiro ampliado três vezes.
