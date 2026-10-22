# Cats & Dungeons | Um Roguelike Felino Escrito do Zero

<div align="center">
  <img src="https://i.imgur.com/k7O5GhT.jpeg" alt="Cats & Dungeons em execução">
  <p>Um roguelike de arena em pixel art onde gatos enfrentam ondas de ratos, esqueletos e chefes amaldiçoados — sem engine, sem framework gráfico, só Java.</p>
</div>

<br />

<div align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">
  <img src="https://img.shields.io/badge/Swing_%2F_AWT-5382A1?style=for-the-badge&logo=java&logoColor=white">
  <img src="https://img.shields.io/badge/Pixel_Art-BE3144?style=for-the-badge&logo=aseprite&logoColor=white">
  <img src="https://img.shields.io/badge/Licen%C3%A7a-Propriet%C3%A1ria-872341?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iJTIzZmZmIj48cGF0aCBkPSJNMTIgMWE1IDUgMCAwMC01IDV2M0g1djE0aDE0VjloLTJWNmE1IDUgMCAwMC01LTV6bTAgMmEzIDMgMCAwMTMgM3YzSDlWNmEzIDMgMCAwMTMtM3ptMCAxMWEyIDIgMCAxMTAgNCAyIDIgMCAwMTAtNHoiLz48L3N2Zz4=">
</div>

---

## Visão Geral

*Cats & Dungeons* é um roguelike de sobrevivência em horda. O jogador escolhe um
entre três gatos — cada um com a própria família de arma, atributos e uma
habilidade exclusiva — e enfrenta uma horda sem fim de ratos num campo aberto
gerado a cada corrida. As armas atacam sozinhas; o jogador anda, dá dash e
escolhe, a cada nível, entre atributos, passivas e armas extras. A corrida dura
**30 minutos**, com um chefe a cada cinco e o Rato Rei no fim.

Morrer encerra a corrida e apaga o save dela, mas o ouro ganho compra upgrades
permanentes para as próximas.

O que distingue este projeto não é o gênero, é como ele foi feito. **Não há
engine.** Nada de Unity, Godot, libGDX ou LWJGL. O laço de jogo, o renderizador, o
sistema de física vetorial, o sistema de partículas e toda a
interface foram escritos do zero sobre a biblioteca padrão do Java — `Canvas` com
`BufferStrategy` para desenhar, `javax.sound.sampled` para o som. O pacote chamado
`engine` é camada interna do próprio time, não motor de terceiros.

## O Estado do Projeto

Este é um **projeto de faculdade**. Foi escrito enquanto os quatro autores ainda
estavam aprendendo a programar — aprendendo, ao mesmo tempo, a estruturar um
projeto, a organizar código e a fazer um jogo funcionar. Boa parte do valor está
justamente aí: é um jogo completo feito à mão, com todas as decisões, boas e
ruins, visíveis no código.

Depois de pouco mais de um ano parado, voltamos a mexer nele. E é honesto dizer:
**ainda há muita coisa para corrigir, melhorar e adaptar.** Existem sistemas que
precisam ser repensados, decisões antigas que não se sustentam mais e trechos que
ainda carregam o jeito de quem estava começando.

A ideia não é reescrever tudo de uma vez. É ir adaptando aos poucos, uma parte de
cada vez, sem quebrar o que já funciona. O que já foi refeito está nas
[notas de versão](docs/releases/); o que ainda falta está no
[roadmap](docs/planejamento/ROADMAP.md), junto com os
[pontos frágeis conhecidos](docs/ARQUITETURA.md#pontos-frágeis-conhecidos) —
registrados de propósito, para não se perderem de novo.

## Destaques

* **Zero engine, zero framework gráfico:** laço de tempo fixo a 60 Hz com render
  desacoplado, desenho em `BufferStrategy` com pipeline OpenGL, tudo em Java puro.
* **Física vetorial de verdade:** entidades acumulam vetores de força, com peso,
  atrito, arrasto e repulsão entre corpos — não é translação simples de posição.
* **Mapas desenhados como imagem:** cada sala é um PNG em que a cor do pixel
  define o tile, acompanhado de um JSON que descreve as entidades. Editar um mapa
  é pintar num editor de imagem. O campo da corrida é gerado na hora com a mesma
  tabela de cores.
* **Interface em 9-slice na identidade do jogo:** botões com orelhas de gato que
  escalam para qualquer largura sem deformar, em quatro estados, com escala sempre
  inteira para não borrar a pixel art.
* **Assets de UI gerados por código:** os sprites de botão e o ícone saem de um
  gerador procedural versionado, e a CI quebra o build se os PNGs saírem de
  sincronia com ele.
* **Cenário derivado por gerador:** os tiles e props da dungeon saem de um pacote
  CC0 remapeado por código para a paleta de pedra fria do jogo, em vez de copiados
  como estão — a arte de fora entra já dentro da identidade visual daqui.
* **Escala automática:** a interface acompanha a resolução escolhida a partir de
  uma referência de 640×360, em escala inteira e na mesma proporção do 720p ao 4K;
  o jogador ajusta interface, menus e HUD em por cento, e o HUD nunca
  cresce além do que cabe na tela.
* **Vídeo estável:** lógica em passo fixo de 60 Hz com desenho
  interpolado, limitador preciso (timer de 1 ms no Windows), limite "igual ao
  monitor" por padrão e tela cheia sem bordas, que não pisca no Alt+Tab.

## Stack Tecnológica

* **Linguagem:** Java 21 (o código usa `SequencedCollection`).
* **Build:** Gradle com Kotlin DSL, layout padrão `src/main/java`.
* **Gráficos:** `java.awt.Canvas` + `BufferStrategy`, com pipeline OpenGL do Java 2D.
* **Áudio:** TinySound sobre `javax.sound.sampled`; músicas em Ogg Vorbis via SPI.
* **Dados:** JSON via json-simple, para configurações, mapas e personagens.
* **Diagnóstico:** OSHI no overlay de depuração.
* **CI:** GitHub Actions — build, testes e verificação dos assets gerados; mescla os PRs na `develop` e promove a `main` sozinha quando tudo passa.

## Como Rodar

Requer **JDK 21 ou superior**. O Gradle vem embutido no wrapper.

```sh
# Rodar o jogo
./gradlew run

# Compilar e testar
./gradlew build

# Gerar um jar executável único
./gradlew fatJar

# Regerar os sprites de UI a partir do gerador procedural
./gradlew genUiAssets

# Rodar a CI inteira aqui, antes de empurrar
./gradlew ci
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

### Antes do primeiro push

```sh
./gradlew hooks
```

Isso aponta o git para `.githooks/`, e a partir daí **todo `git push` roda a CI
local antes** — se ela falhar, o push é cancelado. O comando precisa ser rodado
uma vez por clone porque o git não versiona `.git/hooks`.

O que a CI local checa, além de repetir os comandos do `ci.yml`: que nenhum
arquivo de código está escondido pelo `.gitignore` (a CI roda num clone, e o que
só existe na sua máquina não existe lá) e que nenhum passo novo entrou no
workflow sem entrar aqui. Ver `tools/CiLocal.java`.

Num caso pontual dá para pular com `git push --no-verify`.

### Fluxo de branches

O trabalho nasce da `develop`, numa branch própria, e volta por pull request.
Quando a CI do PR passa, o próprio workflow mescla na `develop`; a `develop` roda
a CI de novo e, verde, a `main` avança sozinha até ela. Push direto em `develop`
ou `main` é recusado pelo hook. O passo a passo está em
[`docs/CONTRIBUINDO.md`](docs/CONTRIBUINDO.md).

## Controles

| Tecla | Controle (Xbox) | Ação |
|---|---|---|
| `W` `A` `S` `D` ou setas | Analógico esquerdo · D-pad | Mover (em 360° pelo analógico) |
| `Espaço` | `LT` / `RT` | Dash |
| `E` | `Y` | Status do gato (atributos, armas e buffs) |
| `F` | `X` | Usar consumível |
| `Q` | `View` | Largar item |
| Rodinha · `1`–`9` | `LB` / `RB` | Trocar slot da hotbar · trocar aba em Settings (`Q`/`E` no teclado) |
| `Enter` | `A` | Confirmar |
| `Esc` | `B` (nas telas) · `Start` | Voltar · pausar |
| Clique no objeto | `B` (em jogo) | Pegar item, abrir caixa, falar com o vendedor, puxar alavanca, ler placa (o mais próximo) |
| | Analógico direito | Espiar: a câmera se afasta até 1,5 tile e volta ao soltar |
| `F11` | | Alternar tela cheia |
| `Ctrl` + `=` / `-` | | Zoom da câmera |
| `F3` | | Overlay de depuração, com gráfico de tempo de quadro |
| `F4` `F5` `F6` | | Hitbox de entidades · grade de tiles · bounds de partículas |

As armas atacam sozinhas, mirando o inimigo mais próximo: não há botão de ataque.

Na loja e na máquina de chiclete, as setas ou o D-pad andam pelas
casas e `Enter`/`A` faz o que o clique faria. Na loja, o
primeiro `A` seleciona e o segundo, na mesma casa, compra.

O controle funciona no Windows, com qualquer controle XInput (Xbox 360, One,
Series ou compatível). Em outros sistemas o jogo segue só com teclado e mouse.

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/CONTEXTO.md`](docs/CONTEXTO.md) | Ponto de partida: o jogo, onde fica cada sistema, estado de cada funcionalidade |
| [`docs/CONTRIBUINDO.md`](docs/CONTRIBUINDO.md) | Fluxo de branches, PR, CI e commits |
| [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) | Como o projeto é organizado e por quê |
| [`docs/planejamento/ROADMAP.md`](docs/planejamento/ROADMAP.md) | Backlog por área |
| [`docs/planejamento/INIMIGOS.md`](docs/planejamento/INIMIGOS.md) | Inimigos e chefes |
| [`docs/planejamento/ARMAS.md`](docs/planejamento/ARMAS.md) | Armas, armas extras, passivas e consumíveis |
| [`docs/planejamento/HABILIDADES.md`](docs/planejamento/HABILIDADES.md) | Plano de cartas e habilidades |
| [`docs/releases/`](docs/releases/) | Notas de versão |
| [`docs/TERCEIROS.md`](docs/TERCEIROS.md) | Bibliotecas de terceiros e suas licenças |

## Equipe

| | Quem | GitHub |
|---|---|---|
| <img src="https://github.com/luzonni.png" width="48" style="border-radius:50%"> | Lucas Zonzini | [@luzonni](https://github.com/luzonni) |
| <img src="https://github.com/Francisco-Neto13.png" width="48" style="border-radius:50%"> | Jose Francisco | [@Francisco-Neto13](https://github.com/Francisco-Neto13) |
| <img src="https://github.com/CaioRenatoDot.png" width="48" style="border-radius:50%"> | Caio Renato | [@CaioRenatoDot](https://github.com/CaioRenatoDot) |
| <img src="https://github.com/CaioGabrielMenezes.png" width="48" style="border-radius:50%"> | Caio Gabriel | [@CaioGabrielMenezes](https://github.com/CaioGabrielMenezes) |

## Licença

**Proprietário. Todos os direitos reservados.**

Este projeto **não** é software livre nem código aberto. O código-fonte estar
visível aqui não concede licença de uso: copiar, modificar, redistribuir ou
reaproveitar qualquer parte — código, sprites, músicas ou textos — exige
autorização prévia e por escrito dos autores.

Você pode ler o código para estudo pessoal e clonar o repositório para avaliação.
Veja [`LICENSE`](LICENSE) para os termos completos e
[`docs/TERCEIROS.md`](docs/TERCEIROS.md) para as bibliotecas e a arte de
terceiros, que seguem as próprias licenças. Parte do cenário deriva do pacote
[Tiny Dungeon](https://kenney.nl/assets/tiny-dungeon), do Kenney, em CC0.

---

<div align="center">
  <p>Escrito à mão, linha por linha, sem engine.</p>
</div>
