# Roadmap

Backlog de desenvolvimento por área. Convertido do antigo `TODO/TODO` em texto puro.

## Engine

- [x] Sistema de pilha para Activity
- [x] Persistência de configurações com gravação atômica
- [x] Escala de interface automática, derivada do tamanho da janela
- [ ] Tirar o `Repulsion` da thread paralela e trazer para o tick principal
- [ ] Encerrar as threads pendentes ao fechar o jogo (o processo hoje sobrevive)

## Física

- [x] Ajustar a movimentação do zumbi
- [ ] Movimentações distintas por tipo de inimigo
- [x] Corrigir o A\*: a refatoração da branch `alpha` foi integrada e a mistura
      de unidades (pixels contra índices de tile) foi resolvida. Coberto por testes.

## Design

- [x] Consertar o F11
- [x] Atualizar os sprites dos gatos
- [x] Tela de Game Over
- [x] Partículas de elementos, caminhada e efeito de poção
- [x] Botões e tela de opções na identidade visual do jogo
- [ ] Outline nos sprites
- [ ] Ajustar as arenas
- [ ] Alterar os sprites dos itens
- [ ] Deixar mais claro que o vendedor é um vendedor
- [ ] Mapas individuais por nível
- [ ] Sprites dos bosses
- [ ] Definir que partícula cada `strike` emite
- [ ] Criar os 12 itens adicionais
- [ ] Criar 6 inimigos

## Waves

- [x] Quantidade de waves
- [ ] Loja entre waves

## Som

- [x] Som de caminhada
- [x] Migrar as músicas para OGG com streaming (67 MB de WAV viraram 6 MB)

## Controles

- [ ] Organizar as teclas de ação
- [ ] Aba de Controles nas opções, com remapeamento de teclas

## Save

- [ ] Activity de salvamento entre waves
- [ ] Persistir o estado da partida

## Ideias de itens

Ainda sem implementação, mantidas como referência criativa.

### Clássicos com toque felino

| Item | Efeito | Estado |
|---|---|---|
| Claw Blades | Garras de curto alcance que regeneram vida ao acertar | Em produção |
| Whisker Whip | Chicote de bigodes mágicos, atinge à distância com chance de atordoar | Ideia |
| Tail Dagger | Lâmina presa ao rabo, para ataques furtivos | Ideia |

### Longo alcance

| Item | Efeito |
|---|---|
| Hairball Launcher | Bolas de pelo grudentas: deixam lento ou cegam temporariamente |
| Yarn Grenade | Bola de lã explosiva que enrola e prende o inimigo por um turno |
| Laser Pointer Wand | Inimigos seguem o laser por 1 turno |

### Caóticos

| Item | Efeito |
|---|---|
| Spray Bottle of Doom | Borrifa água, causa medo e faz fugir temporariamente |
| Canned Tuna Bomb | Atrai inimigos por 1 turno antes de explodir |
| Catnip Claws | Aumentam a força, com chance de confundir o próprio jogador |

### Mágicos

| Item | Efeito |
|---|---|
| Feline Flame Fang | Lança uma labareda em forma de gato |
| Meowgic Staff | Miados sônicos com dano em área |
| Scratch of Shadows | Teleporta o jogador atrás do inimigo para um ataque surpresa |

### Máquina de gacha

Ideia registrada, sem detalhamento.
