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
- [x] Definir que partícula cada `strike` emite (a folha segue o tipo de dano,
      em `Enemy.particulaDe`; terra e ar geradas por `tools/GenParticulasElementais`)
- [ ] Criar os 12 itens adicionais
- [ ] Criar 6 inimigos

## Waves

- [x] Quantidade de waves
- [ ] Loja entre waves

## Som

- [x] Som de caminhada
- [x] Migrar as músicas para OGG com streaming (67 MB de WAV viraram 6 MB)
- [x] Normalizar o loudness dos 50 arquivos (`tools/GenMixagem.java`): as músicas
      iam de -6,9 a -33,8 LUFS e os efeitos de -13 a -43,6 dB RMS
- [x] Curva logarítmica nos controles de volume e slider de Master
- [ ] Ducking: abaixar a música quando um som importante toca
- [ ] Limite de instâncias por efeito (hoje só o `Item` tem freio, de 130 ms)

## Controles

- [ ] Organizar as teclas de ação
- [ ] Aba de Controles nas opções, com remapeamento de teclas

## Save

- [ ] Activity de salvamento entre waves
- [ ] Persistir o estado da partida

## Cartas e habilidades

O plano completo — decisões, pesquisa e backlog — está em
[`HABILIDADES.md`](HABILIDADES.md). **Consultar antes de mexer, atualizar
depois.** Resumo do estado: as cartas hoje dão só atributo; o alvo é que deem
habilidade, com o Elemento no papel que o Deus ocupa no Hades.

- [ ] Fase 0 — vocabulário de status e os ganchos
- [ ] Fase 1 — habilidades globais que leem o elemento
- [ ] Fase 2 — globais que leem a classe sem serem escritas por classe
- [ ] Fase 3 — assinaturas exclusivas por classe
- [ ] Fase 4 — pré-requisito e combinação

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
