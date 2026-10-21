# Armas e passivos

Planejamento do arsenal. ✅ marca o que já existe no jogo.

Revisado em **2026-10-21** contra o código, depois da limpeza do modo por salas. Desde a versão anterior deste
documento (2026-09-06) o arsenal passou a ter três camadas: a **arma da mão**,
que define o gato; as **armas extras**, que se acumulam durante a corrida; e as
**passivas**, que mudam as regras. Os itens consumíveis continuam existindo, mas
saíram do sorteio.

## Armas da mão

Todas atacam sozinhas, mirando o inimigo alcançável mais próximo. Cada gato só
usa armas da própria família (`Classe`, derivada do nome do ID):

| Família | Gato | Armas |
|---|---|---|
| Corpo a corpo | Muffin (começa com a espada) | `Sword`, `Axe`, `BloodyAxe`, `Sickle`, `ClawBlades`, `Shield` (ID `DangerousWand`) |
| Magia | Azrael (começa com a varinha) | `Wand`, `Laser` |
| Distância | Finn (começa com o arco) | `Bow`, `BowEletric`, `Kunai`, `Trident`, `Silk`, `Furball`, `Bomb`, `GasBomb` |

O **elemento** é da corrida, e não da arma: escolhido aos 45 s
(`EscolhaDeElemento`), ele transmuta a arma da mão na variante elemental, que é um
desenho próprio e não um recolorido. Espada, machado, arco e varinha têm
variantes de fogo, gelo, água, terra, ar e lendária. Cada elemento muda dano e
cadência (`Elemento.java`): terra bate 45% mais e é 40% mais lenta; ar bate 22%
menos e é 25% mais rápida.

### Onde se consegue outra arma da mão

Só na **loja da antecâmara**, antes de a corrida começar — e ela faz parte da
mecânica de dinheiro, que está **em espera** (ver
[ROADMAP.md](ROADMAP.md), seção "Dinheiro"). O gato começa com 100 moedas e
nada dentro da corrida rende mais.

A loja monta uma vitrine fixa, uma fileira por família (`Seller.VITRINE`), com
**todas as armas**, inclusive as variantes elementais, a 10 moedas cada; o JSON
do mapa só muda preço e quantidade das que cita. O gato só consegue comprar as da
própria família (`Store`, por `Player.sabeUsar`).

`ItemIDs.EM_ESPERA` (consumíveis, `Bomb`, `GasBomb` e as variantes elementais)
tira itens do que é oferecido **fora** da vitrine fixa. **Inconsistência
registrada:** o comentário do `ItemIDs` diz que as variantes elementais não são
oferecidas na loja, mas a vitrine as lista uma a uma, então elas aparecem. Já os
consumíveis, `Bomb` e `GasBomb` não aparecem — nem a `Feed` que o mapa da
antecâmara pede.

### O plano original, item a item

| # | Arma | Variações planejadas | Estado |
|---|---|---|---|
| 1 | Espada | Espada flamejante · Espada de vida (restaura % ao usar) | ✅ espada e espada de fogo. A "espada de vida" não existe com esse nome; quem rouba vida é o `BloodyAxe` (6% do dano) — **a confirmar** se ela foi substituída por ele |
| 2 | Arco | Arco elétrico | ✅ |
| 3 | Bomba | Bomba de gás (dano em área enquanto o gás durar) | ✅ existem, mas fora da loja (`EM_ESPERA`): nenhum caminho do jogo as entrega hoje |
| 4 | Laser | — | ✅ |
| 5 | Garra (tipo luva) | Dá dano e empurra o jogador | ✅ `ClawBlades`: golpe curto e repetido que cura ao acertar. **A confirmar** se o empurrão no jogador foi abandonado |
| 6 | Bola de lã | — | ✅ |
| 7 | Vara de gato | Dano à distância | Planejado |
| 8 | Lâmina giratória | Gira ao redor do gato | Parecido já existe: `Sickle` e `Shield` orbitam o gato, e o novelo das armas extras também |

## Armas extras

Ganhas no **caça-níquel do baú** (`Roleta`) e subidas pelas cartas de nível.
Atacam por conta própria, ao mesmo tempo que a arma da mão, como no Vampire
Survivors. Cada uma vai até o **nível 5**. A lógica fica em `Arsenal` (orbitais)
e `ArmasDeArea` (área).

| Arma | ID | O que faz | Evolução | Passiva parceira |
|---|---|---|---|---|
| Yarn Ball | `NOVELO` | bolas de lã orbitando | Giant Yarn — bolas enormes, dano dobrado | Magnet |
| Grimoire | `GRIMORIO` | livros girando que empurram | Ancient Tome — órbita maior, dano e empurrão dobrados | Area |
| Catnip Aura | `AURA` | queima o que está perto | Holy Catnip — aura enorme que cura | Regeneration |
| Electric Arc | `RAIO` | raio que pula entre inimigos | Thunderstorm — o dobro de saltos, dano dobrado | Critical |
| Quake | `TREMOR` | pancada no chão que fere e empurra em volta | Earthshaker — raio e dano dobrados | Armor |
| Meteor Rain | `METEORO` | meteoros onde a horda se junta | Armageddon — o dobro de meteoros, dano dobrado | Haste |

**Evolução:** arma no nível 5 + a passiva parceira faz o próximo baú sair com a
evolução garantida (`Evolucao`, `Melhorias.evolucaoDisponivel`).

## Passivas

Vêm das cartas de nível (`SubirDeNivel`) e vão até o nível 5. O efeito é lido por
`Bonus`.

| Passiva | Por nível |
|---|---|
| Magnet | +40% de alcance de coleta de XP |
| Armor | −6% de dano recebido |
| Regeneration | +0,4 HP por segundo |
| Critical | +6% de chance de crítico |
| Area | +12% de área das armas |
| Haste | −8% de intervalo das armas extras (piso de 40%) |
| Growth | +10% de XP |

### Habilidades exclusivas de cada gato

São passivas que só aparecem para um gato, com teto de **três** níveis e carta
marcada como "CAT SKILL":

| Gato | Habilidade | Efeito |
|---|---|---|
| Finn | Volley | +1 flecha por disparo por nível; as flechas extras procuram outros alvos e cada uma leva `1/√n` do dano |
| Muffin | Retaliate | ao levar dano, onda de choque que fere e empurra; recarga de 3 s / 2,5 s / 2 s |
| Azrael | Arcane Burst | as bolas da varinha explodem e ferem em volta com 35% / 50% / 65% do golpe |

Além delas, cada gato tem um bônus fixo: Finn +10% de crítico, Muffin −10% de
dano recebido, Azrael +20% de área.

## Itens consumíveis

O plano antigo chamava estes itens de "passivos". As classes existem, mas **hoje
nenhum caminho do jogo entrega um consumível**: estão fora da loja
(`ItemIDs.EM_ESPERA`), o caixote com loot que os soltava saiu com o modo por
salas, e a máquina de chiclete, que sorteia alguns deles, está em espera com o
dinheiro. Continuam construíveis, e o save sabe gravá-los.

| # | Item planejado | Efeito planejado | No jogo |
|---|---|---|---|
| 1 | Peixe | — | **a confirmar** se algum item corresponde |
| 2 | Ração de energia | Aumenta energia | Não há "energia" no jogo |
| 3 | Ração de cura | Restaura vida | `Feed`: +10 de vida máxima e −1 de sorte |
| 4 | Catnip | "Maconha de gato" | `Catnip`: +3 de velocidade, −3 de alcance |
| 5 | Coleira de ouro | Agora você tem um dono :3 | Não existe |
| 6 | Chapéu de melancia | Concede esquiva (dodge) | `Watermelon`: +5% de esquiva |
| — | — | — | `Acorn`: concede o modificador de dash (hoje o dash já é de fábrica) |
| — | — | — | `MagneticOrb`: puxa todo o XP do mapa. Marcado no código como "retirado do jogo"; o ID fica pelo ordinal |
