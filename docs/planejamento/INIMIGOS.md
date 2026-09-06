# Inimigos e bosses

A coluna **Classe** aponta o arquivo correspondente em
`src/main/java/com/retronova/game/objects/entities/enemies/`.

## Inimigos comuns

### 1. Rato Explosivo — `MouseExplode`

Corre em direção ao jogador até explodir.

- **Dano:** 10 → 15 por explosão
- **Resistência:** nenhuma
- **Fraqueza:** morre com um único ataque
- **Especial:** explode ao chegar perto do jogador

### 2. Rato Escudeiro 🛡️ — `MouseSquire`

Gato guerreiro com um pequeno escudo preso à cabeça como capacete. Avança
contra o jogador e rebate projéteis com a cabeça.

- **Dano:** 6 → 8 (investida com o escudo)
- **Resistência:** ataques frontais e projéteis
- **Fraqueza:** ataques laterais ou pelas costas
- **Especial:** abaixa a cabeça para refletir projéteis de volta

### 3. Rato Vampiro 🦇 — `MouseVampire`

Sugador de energia vital, recupera vida ao atacar.

- **Dano:** 8
- **Resistência:** ataques físicos
- **Fraqueza:** magia
- **Especial:** fica invisível por alguns segundos ao atacar; se for atingido,
  perde a invisibilidade e recebe dano extra

### 4. Esqueleto Feiticeiro — `Skeleton`

Lança orbes de energia maligna.

- **Dano:** 8 por orbe
- **Resistência:** dano mágico
- **Fraqueza:** corpo a corpo
- **Especial:** lança até 3 orbes

### 5. Slime Multiplicador — `Slime`

Divide-se ao ser atacado.

- **Dano:** a definir
- **Resistência:** nenhuma
- **Fraqueza:** a definir
- **Especial:** ao morrer, divide-se em slimes menores

### 6. Rato Zumbi — `Zombie`

O mob mais básico: fraco e sem resistência.

- **Dano:** pouco, porém suficiente
- **Resistência:** nenhuma
- **Especial:** nenhuma

## Bosses

### 1. Brinquedo de Gato Assombrado — `CatToyBoss`

Pelúcia possuída por uma entidade maligna. Os olhos brilham em vermelho
enquanto ele se move sozinho pelo ambiente.

- **Dano:** 7 (pancadas sombrias)
- **Resistência:** ataques físicos normais
- **Fraqueza:** fogo e ataques mágicos
- **Especial:** desmancha-se em pedaços ao ser atingido e se reconstrói

### 2. Rei Gato Amaldiçoado — `KingCursedCatBoss`

- **Resistência:** muito resistente a ataques físicos
- **Fraqueza:** ataques mágicos
- **Especial:** invoca gatos corrompidos

### 3. Rato Monarca — `MonarkMouse`

- **Resistência:** venenos e ataques físicos leves
- **Especial:** invoca ratos pequenos em grande quantidade; desaparece por um
  momento e reaparece em outra parte do mapa

### 4. Gato Chorão — `CryingCat`

Boss final. Sem especificação escrita até o momento.

> ⚠️ O sprite `cryingcat.png` está corrompido: é um arquivo **WebP** com
> extensão `.png`, e o `ImageIO` não consegue lê-lo. O boss carrega hoje com a
> textura de erro.
