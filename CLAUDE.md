# Cats & Dungeons — instruções para o agente

## Visão rápida

- Roguelike de sobrevivência em horda, em pixel art, no molde do *Vampire Survivors*:
  corrida contínua de 30 minutos, armas que atacam sozinhas, chefe final aos 30.
  O fluxo da corrida não para; o jogador só anda, dá dash e escolhe cartas.
- Java 21 puro, sem engine (o pacote `engine/` é código próprio), Gradle com Kotlin DSL,
  Java 2D com pipeline OpenGL, TinySound, JUnit 5. Versões e dependências: `build.gradle.kts`.
- Comandos (no Windows, `.\gradlew.bat`):
  - `./gradlew run` roda o jogo; `./gradlew test` roda os testes; `./gradlew ci` roda a CI local inteira.
  - `./gradlew hooks` uma vez por clone: instala o `pre-push` que roda a CI local.
- Documentação completa em `docs/`. Comece por `docs/CONTEXTO.md` (stack, mapa de
  pastas, onde fica cada sistema, convenções, números de balanceamento). Depois:
  `docs/ARQUITETURA.md`, `docs/CONTRIBUINDO.md`, `docs/planejamento/` (ROADMAP,
  ARMAS, INIMIGOS, HABILIDADES), `docs/releases/`.

## Antes de mexer

- Leia em `docs/` o que cobre a área: `docs/CONTEXTO.md` §4 diz em que arquivo fica
  cada sistema, e `docs/planejamento/` tem o que está decidido para armas, inimigos
  e habilidades.
- Leia o comentário do topo da classe e dos métodos que vai alterar. Os comentários
  registram o defeito que motivou cada decisão; não desfaça uma decisão registrada
  sem dizer por quê.
- Trabalhe numa branch criada da `develop`: `git switch -c <tipo>/<assunto> origin/develop`
  (tipos em `docs/CONTRIBUINDO.md`). Nunca trabalhe direto na `develop` ou na `main`.

## Regras de trabalho

- **Docs na mesma tarefa.** Toda funcionalidade nova ou mudança de comportamento
  atualiza `docs/` no mesmo PR:
  - `docs/CONTEXTO.md`: tabela de estado (§6), onde fica cada sistema (§4) e números de balanceamento (§8);
  - `docs/releases/em-desenvolvimento.md`: um tópico novo, citando o commit;
  - `docs/planejamento/ROADMAP.md`: marcar o item, se houver;
  - o documento de área (`ARMAS.md`, `INIMIGOS.md`, `HABILIDADES.md`, `ARQUITETURA.md`) quando o assunto for dele.
- **Releases antigas são histórico.** Não edite `docs/releases/0.1.*.md`; só acrescente em `em-desenvolvimento.md`.
- **"A confirmar", nunca inventado.** O que não der para verificar no código ou nos
  commits entra marcado **a confirmar**, em docs, PRs e respostas.
- **Commits separados por item**, cada um revertível sozinho; docs ou ferramenta em
  commit separado de código. Formato do time: `docs/CONTEXTO.md` §5 e `docs/CONTRIBUINDO.md`.
- **Antes de concluir**, rode e confira o resultado:
  - `./gradlew test` (e confira falhas em `build/test-results/test/*.xml`, a saída com `-q` é muda);
  - se mexeu em lógica de jogo: abra o fluxo afetado no modo bancada (abaixo) e
    procure `Exception` no log;
  - antes de um PR: `./gradlew ci`, ou deixe o hook `pre-push` rodar.
- **Teste novo para lógica pura.** Conta de balanceamento, regra de cache, fase de
  chefe: extraia em método estático e teste em `src/test/java`, no mesmo pacote.
- **Arquivos completos.** Entregue código que compila e roda, sem `TODO` no lugar de
  partes pedidas.
- **Balanceamento configurável.** Número novo de balanceamento ou de animação vira
  constante nomeada no topo da classe dona (ou em `Bonus`, `Passiva`, `Waves`, nos
  JSON de `resources/.../players/`), nunca literal solto no meio de um método, e
  entra na tabela de `docs/CONTEXTO.md` §8.
- **PR só quando pedido.** PR para a `develop` com CI verde é mesclado sozinho e
  promovido à `main`: abrir um PR é publicar. Base sempre `develop`.

## Testar no jogo

- Modo bancada e as flags `-Dcd.*`: `docs/CONTEXTO.md` §2.
- Para passar as flags ao `gradlew run`, use a variável de ambiente
  `JAVA_TOOL_OPTIONS` (ex.: PowerShell `$env:JAVA_TOOL_OPTIONS="-Dcd.bench=true"`).
  Ela vale para **toda** JVM que sobe, inclusive a do Gradle: nunca coloque nela
  `-XX:StartFlightRecording`, que faz as duas JVMs gravarem o mesmo arquivo e o
  corrompe. Para perfilar, abra o jogo e use `jcmd <pid> JFR.start ...` só no processo do jogo.
- O processo do jogo tem `com.retronova.Main` na linha de comando. Ao fechar janelas
  abertas para teste, feche só as que você abriu (confira a hora de criação);
  pode haver uma janela do usuário aberta.
- Medir desempenho: `-Dcd.perf=true` imprime custo por trecho do render a cada 2 s.

## Padrões do jogo

- **Código em português, tela em inglês.** Nomes, comentários e docs em português;
  todo texto visível ao jogador em inglês. Sem i18n.
- **Comentários explicam o porquê**, com a decisão em MAIÚSCULAS no início do
  parágrafo. Siga o estilo ao comentar código novo.
- **Feedback de gameplay rápido e sem bloquear.** Efeito de acerto, morte, dano ou
  nível dura frações de segundo e nunca trava a entrada; o que cobre a tela fica nas
  bordas. Telas que pausam (cartas, roleta, vitória) aceitam teclado, mouse e controle.
- **Interface:** regras em `.claude/rules/interface.md` (carregado ao mexer em menus, HUD e cartas).
- **Pixel art em escala inteira.** Use `Configs.GameScale()` / `Configs.UiScale()`;
  ao ampliar imagens, fator inteiro e `VALUE_INTERPOLATION_NEAREST_NEIGHBOR`.
- **Texto na tela** passa por `TextFx.draw` (contorno e sombra da casa) e cores de `Palette`.
- **Desempenho em código de render (roda por quadro, por entidade):**
  - não crie `BufferedImage` nem `Color`/`Font` derivados por chamada; guarde em cache
    por sprite (veja `Acelerada`, `Expressao`, `SpriteHandler.flip`);
  - não desenhe formas com antialiasing por entidade (`fillOval`, `fillRoundRect`,
    `GradientPaint`): o Marlin rasteriza na CPU. Pinte uma vez numa imagem e copie;
  - nada de busca em mapa, `String` ou `KeyBoard.KeyPressed` dentro de laço por entidade ou por quadro;
  - o que só faz sentido na tela é cortado pela área visível, como em `Game.renderEntities`.
- **Áudio:** efeito novo é WAV em `resources/.../audio/` + entrada em `Sounds`;
  música é OGG + entrada em `Musics`. Mixagem: `docs/ARQUITETURA.md` (Áudio).

## Armadilhas conhecidas

- **Arquivos com CRLF.** Muitos `.java` são CRLF: `sed`/`perl` com padrão de várias
  linhas não casa. Use a ferramenta de edição de arquivo, e confira o diff depois.
- **Saves e conquistas reais.** Sem `-Dcd.bench`, rodar o jogo grava em `saves/`
  e `config.json` do usuário. Teste que mexe em `Meta` ou `Conquistas` liga
  `cd.bench` (veja `VitoriaTest`).
- **Diagnósticos do editor atrasados.** Os avisos que aparecem logo após uma edição
  podem ser de um estado intermediário; quem decide é `./gradlew compileJava`.

## Nunca

- Nunca insira valor no meio de enum gravado por ordinal (`ItemIDs`, `EntityIDs`,
  `ArmaExtra`, `Passiva`, `Evolucao`): só no fim. Inserir no meio quebra mapas e saves.
- Nunca edite à mão PNG gerado por `tools/Gen*.java`: mude o gerador e rode
  `./gradlew genUiAssets` (UI) ou `java tools/<Gerador>.java`. A CI confere os de UI.
- Nunca mude número de balanceamento sem atualizar `docs/CONTEXTO.md` §8 e registrar
  a mudança em `docs/releases/em-desenvolvimento.md`.
- Nunca commite `build/`, `saves/`, `config.json`, jars gerados nem a pasta local
  `documentation/` (já estão no `.gitignore`; não force com `git add -f`).
- Nunca faça push direto em `develop` ou `main`, nem use `git push --no-verify`.
- Nunca reescreva histórico já enviado (`--force`, `rebase` de branch publicada);
  correção entra em commit novo.
- Nunca afirme que algo funciona sem ter rodado: diga o que foi verificado e o que não foi.
