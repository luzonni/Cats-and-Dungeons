# Cartas e habilidades

Documento de trabalho do sistema de progressão dentro da corrida. **É para ser
consultado antes de mexer e atualizado depois de mexer** — o que estiver aqui
não precisa ser redescoberto nem rediscutido.

Benchmark declarado do projeto: **Hades**. Os números citados como "no Hades"
saem do dump de dados real das bênçãos ([orlp/hades-boons,
`trait_data.json`](https://github.com/orlp/hades-boons/blob/master/trait_data.json),
165 entradas), e não de impressão de jogada.

---

## 1. O objetivo

Hoje as cartas dão só atributo. O objetivo é que elas deem **habilidade** — que
a corrida vire interessante por causa do que o gato *passa a fazer*, e não do
quanto os números dele subiram.

Exemplos que originaram este documento:

- ao dar dash, sai uma aura que causa dano conforme o elemento
- a arma de fogo queima o inimigo ao longo de alguns segundos
- espada dupla, uma em cada mão, do mesmo elemento
- passiva que faz X a cada Y segundos

---

## 2. Decisões tomadas

### 2.1 Global antes de classe — **decidido**

**Evidência:** das 165 bênçãos do Hades, **apenas 7 (4%)** são presas a uma
arma, e as sete são o mesmo caso (a variante de Cast do aspecto *Load Ammo* do
escudo). Noventa e seis por cento do conteúdo do benchmark é agnóstico de arma.
A camada por arma (*Aspects*) é um sistema separado, por cima de uma base de
bênçãos já sólida.

**Razão econômica:** habilidade de classe custa 3× para cobrir o elenco e cada
jogador vê 1/3 dela por corrida. Global aparece em 100% das corridas.

**Razão específica deste projeto:** Magia tem 2 armas (Wand, Laser) contra 6 de
melee, e o Azrael já é o mais fraco em DPS — 68 contra 150 do Muffin e 108 do
Finn. Camada de classe agora aprofunda um buraco existente.

**Consequência:** a espada dupla (a única ideia presa a classe da lista) fica
para a fase 3.

### 2.2 Dois tipos de carta, cortados por **encaixe vs empilhável** — decidido

Esta decisão **revisa** uma anterior, que dividia por "número vs
comportamento". O dado mostra que o eixo que sustenta o sistema é outro.

No Hades há cinco **encaixes** — Attack, Special, Cast, Dash, Call. Cada um
segura **uma** bênção; pegar outra para o mesmo encaixe **descarta a anterior**.
As passivas não obedecem a isso e acumulam. Distribuição real entre as 126
bênçãos regulares:

| | quantas |
|---|---|
| Passivas (empilham) | **74** |
| Cast | 16 |
| Attack | 10 |
| Call | 9 |
| Special | 9 |
| Dash | 8 |

O descarte é o ponto inteiro: é ele que transforma a carta numa **decisão**
("abro mão do Dash de Fogo pelo de Gelo?") em vez de mais um número somando.

**Para o Cats & Dungeons:**

- **`Melhoria` fica como está.** Vigor, Fúria, Couro, Patas, Frenesi, Instinto,
  Presságio já são o balde de passivas: empilham e têm curva de desgaste. Sem
  retrabalho.
- **Nasce `Encaixe`**, um por ação, trocar descarta. Quatro encaixes, não cinco:

| Encaixe | A ação que ele modifica |
|---|---|
| `ATAQUE` | o golpe da arma |
| `DASH` | o arranco |
| `PULSO` | a passiva de tempo (a cada N segundos) |
| `REVIDE` | ao levar dano |

### 2.3 Proporção alvo — decidido

O Hades tem 76% regulares, 17% duo (exigem duas divindades) e 7% lendárias
(exigem bênção específica). **72 das 165 (44%) têm pré-requisito.**

Para um baralho de ~30 cartas aqui: **~18 passivas, ~12 de encaixe**, das quais
4 ou 5 exigindo combinação.

### 2.4 O Elemento ocupa o papel do Deus — decidido

São 5 elementos contra 9 deuses do Hades, o que é escala razoável para o
tamanho do projeto. Toda habilidade que **lê o elemento** nasce como cinco
habilidades sem ser escrita cinco vezes. É a alavanca principal de conteúdo.

---

## 3. O que já existe no código

Levantado, não presumir de novo:

| Peça | Onde | Estado |
|---|---|---|
| `Melhoria` (7 atributos + `Modifiers`) | `game/items/Melhoria.java` | pronto |
| `Melhorias` (livro-caixa, `oferecer`, serialização) | `game/items/Melhorias.java` | pronto |
| `Recompensa` (tela de 3 cartas) | `game/map/arena/Recompensa.java` | pronto |
| `Raridade` | `game/items/Raridade.java` | pronto |
| `Elemento` (5 + NENHUM + LENDARIA, cor, som, tipo de dano) | `game/items/Elemento.java` | pronto |
| `Classe` (melee / magia / distância) | `game/items/Classe.java` | pronto |
| `EFFECT_FIRE(seg, reps)` | `entities/Entity.java` | existe, **só a Lava usa** |
| `EFFECT_STUNNED(seg)` | `entities/Entity.java` | existe, só a bomba de gás usa |
| `EFFECT_POISON(seg, reps)` | `entities/Entity.java` | existe, **ninguém usa** |
| `EFFECT_REGENERATION(seg, reps)` | `entities/Entity.java` | existe, **ninguém usa** |
| `addEffect(nome, aplicador, seg, reps)` | `entities/Entity.java` | é a base dos EFFECT_* |
| Partículas elementais por tipo de dano | `enemies/Enemy.java` → `particulaDe` | pronto |

Metade do vocabulário de status já está construída e sem uso, na mesma
categoria dos sons e das partículas elementais que estavam órfãos.

---

## 4. Arquitetura a construir

### 4.1 Vocabulário de status — **primeiro passo**

É o que faz habilidade conversar com habilidade **sem a conversa ser escrita**.

No Hades a sinergia não é autorada par a par: existem 7 status compartilhados e
bênçãos de **deuses diferentes** falam a mesma palavra.

| Status do Hades | Bênçãos que mexem nele |
|---|---|
| Weak | 14 |
| Chill | 13 |
| Deflect | 11 |
| Hangover | 11 |
| Doom | 7 |

Ninguém escreveu "Ares + Deméter"; escreveram bênçãos que aplicam Doom e
bênçãos que reagem a Chill, e a combinação caiu de pé.

**Vocabulário proposto**, um por elemento mais os neutros:

| Status | Elemento | O que faz | Base existente |
|---|---|---|---|
| Queimado | Fogo | dano ao longo do tempo | `EFFECT_FIRE` ✔ |
| Congelado | Gelo | lentidão | — |
| Encharcado | Água | aumenta dano de outros status | — |
| Atordoado | Terra | perde o turno de ataque | `EFFECT_STUNNED` ✔ |
| Exposto | Ar | recebe dano extra | — |
| Envenenado | (neutro) | dano percentual | `EFFECT_POISON` ✔ |

Encharcado é o status-multiplicador de propósito: é o que dá margem a
combinação sem precisar de carta duo escrita à mão.

### 4.2 Ganchos — **segundo passo**

**Quantos:** o BaseMod do Slay the Spire expõe **30-40** ganchos depois de anos
e de um ecossistema inteiro de mods; o toolkit do StS2 chega a 136. Começar com
**quatro ou cinco**.

**Duas naturezas, desde o primeiro dia.** Os ganchos do BaseMod são de dois
tipos: os que só **avisam** e os que **modificam o valor em trânsito** (o
`MaxHpChange` recebe o número e devolve outro; o StS2 fala explicitamente em
*value modifiers* e *boolean gates*).

Importa direto aqui: "queima ao acertar" é só aviso, mas "+30% de dano contra
inimigos queimando" precisa **interceptar o dano antes de ele cair**. Nascendo
só como aviso, acrescentar interceptação depois obriga a mexer em todos os
pontos de chamada.

| Gancho | Natureza | Onde encaixa |
|---|---|---|
| `aoDarDash` | aviso | `Player`, junto de onde nasce o `Rastro` |
| `aoAcertar(alvo, dano, tipo)` | **filtro** (devolve o dano) | `Enemy.strike` |
| `aoLevarDano(dano)` | **filtro** | `Player.strike` |
| `aCadaPulso` | aviso | tick do `Player`, contador próprio |
| `aoMatar(alvo)` | aviso | `Enemy.die` |

**Armadilha documentada:** o BaseMod tem um `unsubscribeLater()` só para evitar
`ConcurrentModificationException` quando alguém se desinscreve dentro do próprio
callback. O `Game.getMap()` tem a mesma forma — nascer com a lista protegida.

### 4.3 Pré-requisito — **último passo**

44% das bênçãos do Hades têm pré-requisito: o baralho lê o que você já tem e
oferece de acordo. É o que gera as duo e as lendárias. Só faz sentido quando já
houver o que combinar.

---

## 5. Backlog

### Fase 0 — infraestrutura (não aparece na tela)

- [ ] Vocabulário de status: criar `Status` com os seis, reaproveitando os
      `EFFECT_*` que já existem
- [ ] Ligar os `EFFECT_*` órfãos (veneno, regeneração) ao vocabulário
- [ ] Os cinco ganchos, com as duas naturezas (aviso e filtro)
- [ ] Proteger a lista de inscritos contra modificação durante o callback
- [ ] Tipo `Encaixe` + o livro-caixa aceitando os dois tipos de carta
- [ ] `Recompensa` oferecendo carta de encaixe e mostrando o **descarte**
      quando o encaixe já está ocupado

### Fase 1 — habilidades globais que leem o elemento

- [ ] **Aura de Dash** — o arranco causa dano na cor do elemento *(uma carta,
      cinco comportamentos; é a `AthenaRushTrait` do Hades)*
- [ ] **Golpe Elemental** — o ataque aplica o status do elemento *(a queimadura
      da arma de fogo cai aqui; `EFFECT_FIRE` já funciona)*
- [ ] **Pulso** — a cada N segundos, um estouro elemental em volta do gato
- [ ] **Revide** — ao levar dano, aplica o status do elemento em quem encostou

### Fase 2 — globais que leem a classe sem serem escritas por classe

- [ ] **Alcance** — "seu ataque vai mais longe": significa coisas diferentes
      para espada, varinha e arco, mas é **uma** carta
- [ ] **Todos os ângulos** — "seu ataque atinge em volta"
- [ ] **Perfuração** — "seu ataque atravessa o primeiro alvo"

### Fase 3 — assinaturas exclusivas (a camada *Aspect*)

- [ ] **Espada dupla** (melee) — segundo render, segunda caixa de ataque
- [ ] Uma para magia
- [ ] Uma para distância

### Fase 4 — combinação

- [ ] Pré-requisito nas cartas
- [ ] 4 ou 5 cartas exigindo dois status ou dois encaixes

---

## 6. Riscos anotados

**O baralho atual é o modo de falha documentado.** A queixa mais recorrente de
jogadores sobre roguelites é exatamente pool de "+10% de dano, +10 de vida,
+10% de velocidade de movimento", descrita como "extremamente insatisfatória",
porque todos os builds acabam iguais. O baralho de hoje é literalmente isso. O
remédio não é apagar — o Hades tem número aos montes — é a **proporção**.

**Azrael é o mais fraco:** 68 de DPS contra 150 do Muffin e 108 do Finn, e
Magia só tem 2 armas na loja. Carta global conserta os três de uma vez; carta
de classe piora.

---

## 7. Fontes

- [orlp/hades-boons — `trait_data.json`](https://github.com/orlp/hades-boons/blob/master/trait_data.json) — os 165 registros
- [Boons — Hades Wiki (Fextralife)](https://hades.wiki.fextralife.com/Boons) — encaixes, passivas, duo/lendária, status
- [Boons — Hades Wiki (Fandom)](https://hades.fandom.com/wiki/Boons)
- [BaseMod Hooks — Slay the Spire](https://github.com/daviscook477/BaseMod/wiki/Hooks) — quantidade e natureza dos ganchos
- [Roguelike Item Orthogonality — Game Developer](https://www.gamedeveloper.com/design/roguelike-item-orthogonality)
- [Do you like meta progression in your roguelikes/roguelites? — ResetEra](https://www.resetera.com/threads/do-you-like-meta-progression-in-your-roguelikes-roguelites.1341955/)

---

## 8. Registro

| Data | O que mudou |
|---|---|
| 2026-09-19 | Documento criado. Decisões 2.1 a 2.4 tomadas com base no dump do Hades. Nada implementado ainda. |
