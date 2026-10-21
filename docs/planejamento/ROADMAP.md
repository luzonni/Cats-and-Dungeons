# Roadmap

Backlog de desenvolvimento por área. Convertido do antigo `TODO/TODO` em texto puro.

Legenda: `[x]` feito · `[ ]` a fazer · 🔄 em andamento · ~~riscado~~ abandonado
ou substituído (o motivo vai ao lado).

Revisado em **2026-10-21** contra o código (commit `8a9cbb8` e a limpeza que veio depois).

## Engine

- [x] Sistema de pilha para Activity
- [x] Persistência de configurações com gravação atômica
- [x] Escala de interface automática, derivada do tamanho da janela
- [x] Tamanho de interface no estilo do Minecraft (Auto / 1 / 2 / 3)
- [x] Pipeline OpenGL ligado de verdade (antes desenhava em software)
- [x] Interpolação de mundo, câmera e menus entre ticks; 165 e 240 FPS
- [x] Ritmo de quadros uniforme e opção de VSync
- [x] Medidor de quadros (`-Dcd.perf`) e modo bancada (`-Dcd.bench`)
- [x] Tirar o `Repulsion` da thread paralela e trazer para o tick principal
- [x] Encerrar as threads pendentes ao fechar o jogo — a que ficava presa era a
      do `Repulsion`, uma por troca de mapa; a das waves saiu com o modo por ondas
- [x] Chamar `resetWindow()` na EDT

## Física

- [x] Ajustar a movimentação do zumbi
- [x] Movimentações distintas por tipo de inimigo — golpe com aviso
      (`Investida`) em quatro ritmos, e os ratos cercam o gato
- [x] Corrigir o A\*: a refatoração da branch `alpha` foi integrada e a mistura
      de unidades (pixels contra índices de tile) foi resolvida. Coberto por testes.
      Removido em 2026-10-21: no campo aberto ninguém o usava.
- [x] Dash por impulso, de fábrica, com rastro

## Design

- [x] Consertar o F11
- [x] Atualizar os sprites dos gatos
- [x] Tela de Game Over (hoje é o resumo da corrida com a loja permanente)
- [x] Partículas de elementos, caminhada e efeito de poção
- [x] Botões e tela de opções na identidade visual do jogo
- [x] Outline nos sprites — inimigos com contorno, clarão ao apanhar e o que
      fere o gato marcado em vermelho
- [x] Definir que partícula cada `strike` emite (a folha segue o tipo de dano,
      em `Enemy.particulaDe`; terra e ar geradas por `tools/GenParticulasElementais`)
- [x] Luz dinâmica, sombras suaves e vinheta (`Atmosfera`)
- [x] Texto com contorno e sombra em todas as telas
- [ ] Ajustar as arenas — o campo gerado substituiu as arenas desenhadas na
      corrida; **a confirmar** se o item ainda vale para alguma delas
- [ ] Alterar os sprites dos itens
- [ ] Deixar mais claro que o vendedor é um vendedor
- ~~Mapas individuais por nível~~ — substituído pelo campo gerado a cada
      corrida (`Gerador`)
- 🔄 Sprites dos bosses — o Rato Rei usa o escudeiro ampliado três vezes.
      `CryingCat` e `MonarkMouse`, que não tinham sprite utilizável, saíram do
      código em 2026-10-21
- [ ] Criar os 12 itens adicionais
- [ ] Criar 6 inimigos

## Waves e corrida

- [x] Quantidade de waves
- [x] Corrida sem fim de 30 minutos em campo aberto, no ritmo do Vampire Survivors
- [x] Cercos, elites e endurecimento da horda pelo tempo
- [x] Chefe a cada cinco minutos e o Rato Rei aos 30, com tela de vitória
- [x] Fúria Felina e objetos quebráveis
- ~~Loja entre waves~~ — não há mais waves nem salas; só a antecâmara vende
- [x] Decidir o destino do modo por ondas (dificuldades `normal` e `hard`,
      `Recompensa`, `TrapDoor`, plantas `cistern_1` a `6`) — removido em
      2026-10-21, junto com o vendedor de fim de sala

## Dinheiro — ⏸️ em espera

Decisão de 2026-10-21: a mecânica fica **em espera**, sem ser removida nem
expandida. Hoje o gato começa com 100 moedas, nada dentro da corrida rende moeda
e só a loja da antecâmara as consome. O código continua de pé e marcado como em
espera: `Seller`, `Store`, `Coin`, `GumMachine`, `GumInterface`, `GumPanel` e o
campo `money` do `Player` (gravado no save).

- [ ] Decidir o papel do dinheiro na corrida sem fim (moedas caindo dos inimigos,
      loja no meio da corrida, máquina de chiclete) ou removê-lo
- [ ] Alinhar a loja da antecâmara com o `ItemIDs.EM_ESPERA`: a vitrine fixa
      (`Seller.VITRINE`) põe à venda as variantes elementais que o comentário do
      `ItemIDs` diz não serem oferecidas, e a `Feed` que o mapa pede nunca aparece
      (ver [ARMAS.md](ARMAS.md))

## Progressão

- [x] XP e nível com cartas sorteadas (atributo com raridade, passiva, arma),
      com reroll e skip
- [x] Baú com caça-níquel
- [x] Seis armas extras e seis evoluções
- [x] Passivas e uma habilidade exclusiva por gato
- [x] Ouro e upgrades permanentes entre corridas
- [x] Dez conquistas, com tela própria

## Som

- [x] Som de caminhada
- [x] Migrar as músicas para OGG com streaming (67 MB de WAV viraram 6 MB)
- [x] Normalizar o loudness dos 50 arquivos (`tools/GenMixagem.java`): as músicas
      iam de -6,9 a -33,8 LUFS e os efeitos de -13 a -43,6 dB RMS
- [x] Curva logarítmica nos controles de volume e slider de Master
- [x] Música da arena que acompanha a horda, calmaria antes do chefe e cruzamentos
- [ ] Ducking: abaixar a música quando um som importante toca
- [ ] Limite de instâncias por efeito (hoje só o `Item` tem freio, de 130 ms)

## Controles

- [x] Controle de Xbox (XInput), com vibração opcional
- [x] Modos de mouse e de controle; analógico direito espia a câmera
- [x] Navegação por foco (D-pad e setas) no inventário, nas lojas, na máquina
      de chiclete e nos upgrades do fim de corrida
- [ ] Organizar as teclas de ação
- [ ] Aba de Controles nas opções, com remapeamento de teclas

## Save

- [x] Activity de salvamento — tela `Saves` com três espaços, retomar e apagar
- [x] Persistir o estado da partida (`Corrida`)
- [x] Gravar o meio da corrida sem fim: relógio, chefes vivos e números da corrida
      vão junto; grava a cada minuto, depois de cada escolha e ao sair pela pausa
      (ver [ARQUITETURA.md](../ARQUITETURA.md#persistência))
- [x] Retomar não cura mais o gato com o upgrade Vitality
- [ ] Gravar os inimigos da tela, se a brecha de "sair limpa a tela" incomodar

## Cartas e habilidades

O plano completo — decisões, pesquisa e backlog — está em
[`HABILIDADES.md`](HABILIDADES.md). **Consultar antes de mexer, atualizar
depois.**

Estado em 2026-10-21: nenhuma fase do plano foi implementada. As cartas de
nível passaram a dar passivas e armas no modelo do Vampire Survivors, e cada
gato ganhou uma habilidade exclusiva — o que corresponde mais à fase 3 do que
às fases 0 a 2. **A confirmar** se o plano continua valendo.

- [ ] Fase 0 — vocabulário de status e os ganchos
- [ ] Fase 1 — habilidades globais que leem o elemento
- [ ] Fase 2 — globais que leem a classe sem serem escritas por classe
- 🔄 Fase 3 — assinaturas exclusivas: existem por **gato** (Voleio, Retaliação,
      Explosão Arcana), não por classe
- [ ] Fase 4 — pré-requisito e combinação

## Ideias de itens

Ainda sem implementação, mantidas como referência criativa.

### Clássicos com toque felino

| Item | Efeito | Estado |
|---|---|---|
| Claw Blades | Garras de curto alcance que regeneram vida ao acertar | Feito: arma da mão (`ClawBlades`), cura ao acertar |
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

Ideia registrada, sem detalhamento. Duas coisas no jogo se aproximam dela: o
caça-níquel do baú (`Roleta`) e a máquina de chiclete (`GumMachine`), que existe
no código mas não está em nenhum mapa.
