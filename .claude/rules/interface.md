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

- Tela inicial, pausa e opções usam os mesmos componentes de `menus/shared/`
  (`Button`, `Confirm`, `OptionRow`, `Cenario`). Tela nova de menu monta com eles;
  não crie botão, diálogo ou linha de opção próprios.
- A tela de Opções é uma só: `new Options()` a partir do menu e
  `Options.sobreposta(...)` a partir da pausa. Mudança em Opções vale para os dois
  acessos; não crie uma segunda tela de opções.
- Diálogo de confirmação é sempre `Confirm`.

## Hover e foco

- O hover padrão é o do `Button` da tela inicial: realce suavizado entre ticks,
  crescimento de no máximo uma unidade de escala, troca de estado do 9-slice e o
  som `Sounds.Hover` ao entrar. Elemento clicável novo reproduz esse comportamento.
- Nunca use contorno grosso para indicar hover ou foco.
- Foco de teclado e de controle usa o mesmo estado visual do hover (`setFocused`),
  e toda tela navegável aceita setas/D-pad e `Enter`, além do mouse.
- Mouse por cima pede o cursor de mão com `Engine.window.pointing()`.

## Cards

- Diretriz: buffs (cartas de nível) e elementos usam um único componente de card,
  no padrão dos cards de buff de `SubirDeNivel`. A unificação está **a confirmar**
  (em andamento na issue #28): ao mexer em `EscolhaDeElemento` ou em qualquer card
  novo, aproxime do padrão dos cards de buff em vez de criar um terceiro estilo.

## Desenho

- Cores de `Palette`; texto por `TextFx.draw`; molduras por `NineSlice` / `Button.frame`.
- Escala da interface por `Configs.UiScale()`, sempre inteira. Telas que podem não
  caber calculam uma escala menor que caiba (veja `escala()` em `GameOver` e `Vitoria`).
- Ícones pequenos: pixel art ampliada por fator inteiro, sem suavização
  (`Relevo`, `Icones`).
- Botões e ícone de UI saem de `tools/GenUiAssets.java`: mude o gerador e rode
  `./gradlew genUiAssets`; a CI recusa PNG fora de sincronia.
- HUD durante a corrida não cobre o centro da tela e não bloqueia a entrada.
