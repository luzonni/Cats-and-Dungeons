---
paths:
  - "src/main/java/com/retronova/menus/**"
  - "src/main/java/com/retronova/game/hud/**"
  - "src/main/java/com/retronova/game/interfaces/**"
  - "src/main/java/com/retronova/game/map/arena/SubirDeNivel.java"
  - "src/main/java/com/retronova/game/map/arena/EscolhaDeElemento.java"
  - "src/main/java/com/retronova/game/map/arena/Roleta.java"
  - "src/main/java/com/retronova/engine/graphics/**"
  - "tools/GenUiAssets.java"
---

# Regras de interface

## Sistema único de menus

- Toda tela de menu estende `TelaDeMenu` (`menus/shared/`): título, área de
  conteúdo, rodapé de dicas (`Rodape`), entrada de 0,15 s e Esc/B para voltar.
  Abra com `TelaDeMenu.abrir(...)`. Componentes: `Button`, `Confirm`, `OptionRow`,
  `Abas`, `Cenario`. Não crie botão, diálogo ou linha de opção próprios.
- Settings é um painel só: `menus/Configuracoes.java`. `new Configuracoes()` pela
  tela inicial e `Configuracoes.pausa()` dentro da corrida, que acrescenta a aba
  Run. Mudança em Settings vale para os dois acessos; não crie uma segunda tela.
- Diálogo de confirmação é sempre `Confirm`.

## Hover e foco

- Todo hover e foco passa por `Realce` (`menus/shared/Realce.java`): cresce a
  partir do centro, brilho suave por trás e clareia, em rampa (`Realce.Estado`), e
  o som `Sounds.Hover` ao entrar. Elemento clicável novo usa o `Realce`.
- Nunca use contorno grosso para indicar hover ou foco.
- Foco de teclado e de controle é o mesmo realce mais as setas de
  `Realce.marcadorDeFoco`; toda tela navegável aceita setas/D-pad e `Enter`, além do
  mouse. Elementos focáveis lado a lado ficam a `Realce.folgaEntreVizinhos`.
- Mouse por cima pede o cursor de mão com `Engine.window.pointing()`.

## Cards

- Toda escolha por cartas usa `MaoDeCartas` (`menus/shared/`), o padrão das cartas
  de buff: cada carta só varia no conteúdo (`MaoDeCartas.Carta`). Não crie outro
  estilo de carta.

## Desenho

- Cores de `Palette`; texto por `TextFx.draw`; molduras por `NineSlice` / `Button.frame`.
- Escala da interface por `Configs.UiScale()`, sempre inteira. Telas que podem não
  caber calculam uma escala menor que caiba (veja `escala()` em `GameOver` e `Vitoria`).
- Posição e tamanho saem de `Engine.window.largura()` / `altura()`: o tamanho da
  imagem do jogo, que é a resolução escolhida. Não use o tamanho do canvas.
- Ícones pequenos: pixel art ampliada por fator inteiro, sem suavização
  (`Relevo`, `Icones`).
- Botões e ícone de UI saem de `tools/GenUiAssets.java`: mude o gerador e rode
  `./gradlew genUiAssets`; a CI recusa PNG fora de sincronia.
- Nada que conte tempo ou avance animação mora em `render`: só no `tick`.

## HUD

- HUD durante a corrida não cobre o centro da tela e não bloqueia a entrada.
- A escala do HUD é `Configs.HudScale()`, já limitada ao que cabe
  (`LayoutDoHud`). Peça nova no topo ou na base do HUD entra nas medidas de
  `LayoutDoHud` (e no `LayoutDoHudTest`), senão volta a se sobrepor às outras.
