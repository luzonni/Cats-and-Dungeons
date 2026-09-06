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

*Cats & Dungeons* é um roguelike de sobrevivência por ondas. O jogador escolhe um
entre três gatos — cada um com atributos próprios de vida, dano, sorte e
velocidade — e precisa aguentar o máximo de ondas possível numa arena. Entre uma
onda e outra há loja, e estruturas interativas aparecem em níveis avançados
trazendo puzzles, chefes e itens raros.

O inventário é volátil: morrer significa perder itens, habilidades e nível.

O que distingue este projeto não é o gênero, é como ele foi feito. **Não há
engine.** Nada de Unity, Godot, libGDX ou LWJGL. O laço de jogo, o renderizador, o
sistema de física vetorial, o pathfinding, o sistema de partículas e toda a
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
* **Mapas desenhados como imagem:** cada arena é um PNG em que a cor do pixel
  define o tile, acompanhado de um JSON que descreve as entidades. Editar um mapa
  é pintar num editor de imagem.
* **Interface em 9-slice na identidade do jogo:** botões com orelhas de gato que
  escalam para qualquer largura sem deformar, em quatro estados, com escala sempre
  inteira para não borrar a pixel art.
* **Assets de UI gerados por código:** os sprites de botão e o ícone saem de um
  gerador procedural versionado, e a CI quebra o build se os PNGs saírem de
  sincronia com ele.
* **Escala automática:** a interface acompanha o tamanho da janela em múltiplos
  inteiros de uma resolução lógica de 320×180, do 720p ao 4K.

## Stack Tecnológica

* **Linguagem:** Java 21 (o código usa `SequencedCollection`).
* **Build:** Gradle com Kotlin DSL, layout padrão `src/main/java`.
* **Gráficos:** `java.awt.Canvas` + `BufferStrategy`, com pipeline OpenGL do Java 2D.
* **Áudio:** TinySound sobre `javax.sound.sampled`; músicas em Ogg Vorbis via SPI.
* **Dados:** JSON via json-simple, para configurações, mapas e personagens.
* **Diagnóstico:** OSHI no overlay de depuração.
* **CI:** GitHub Actions — build, testes e verificação dos assets gerados.

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
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`.

## Controles

| Tecla | Ação |
|---|---|
| `W` `A` `S` `D` ou setas | Mover |
| `Espaço` | Dash (requer o modificador) |
| `E` | Inventário |
| `Esc` | Pausar |
| `F11` | Alternar tela cheia |
| `Ctrl` + `=` / `-` | Zoom da câmera |
| `F3` | Overlay de depuração |
| `F4` `F5` `F6` | Hitbox de entidades · grade de tiles · bounds de partículas |

## Documentação

| Documento | Conteúdo |
|---|---|
| [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) | Como o projeto é organizado e por quê |
| [`docs/planejamento/ROADMAP.md`](docs/planejamento/ROADMAP.md) | Backlog por área |
| [`docs/planejamento/INIMIGOS.md`](docs/planejamento/INIMIGOS.md) | Inimigos e chefes |
| [`docs/planejamento/ARMAS.md`](docs/planejamento/ARMAS.md) | Armas e passivos |
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
[`docs/TERCEIROS.md`](docs/TERCEIROS.md) para as bibliotecas de terceiros, que
seguem as próprias licenças.

---

<div align="center">
  <p>Escrito à mão, linha por linha, sem engine.</p>
</div>
