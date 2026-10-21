# Como contribuir

Como o trabalho entra no projeto: branches, pull requests, CI e o caminho até a
`main`.

## O fluxo

```
develop ──▶ <tipo>/<assunto> ──PR + CI verde──▶ develop ──CI verde──▶ main
            (sua branch)          merge automático        promoção automática
```

1. **Toda implementação nasce da `develop`**, numa branch própria.
2. **Ela volta por pull request para a `develop`.** A CI roda no PR; quando todos
   os checks passam, o próprio workflow mescla o PR e apaga a branch. Ninguém
   precisa clicar em merge.
3. **A `develop` mesclada roda a CI de novo**, sobre o commit que ela tem de fato.
   Verde, a `main` avança até esse commit.

A `main` é sempre um commit que já passou pela CI inteira duas vezes: no PR e na
`develop`. Ela nunca recebe nada que não esteja na `develop`.

## Passo a passo

```sh
git fetch origin
git switch -c feature/save-da-corrida origin/develop
# ... commits ...
git push -u origin feature/save-da-corrida
gh pr create --base develop --fill        # ou pela tela do GitHub
```

Pronto. Se a CI passar, o PR entra sozinho na `develop`, e a `develop` vai para a
`main` em seguida.

**Quer ver a CI sem mesclar ainda?** Abra o PR como **rascunho**
(`gh pr create --draft`). Rascunho roda os checks, mas não é mesclado. Quando
estiver pronto, marque como "Ready for review" e a CI roda de novo; verde, entra.

**A CI falhou?** Corrija na mesma branch e dê push. O PR roda de novo e, verde,
entra.

**Conflito com a `develop`?** Traga a `develop` para a sua branch e resolva lá:
`git fetch origin && git merge origin/develop`.

## Nomes de branch

`<tipo>/<assunto-curto>`, com o mesmo tipo do commit:

| Tipo | Para |
|---|---|
| `feature/` | funcionalidade nova |
| `fix/` | correção |
| `refactor/` | reorganização sem mudar comportamento |
| `docs/` | só documentação |
| `chore/` | ferramentas, build, assets gerados |
| `test/` | só testes |

O assunto em português ou inglês, com hífen: `feature/rato-rei`,
`fix/save-cura-o-gato`. A CI não cobra o prefixo — ela só recusa PR para a
`develop` vindo da `main` ou da própria `develop`, e PR para a `main` vindo de
qualquer branch que não seja a `develop`.

## Commits

Em inglês, no formato Conventional Commits: um resumo `tipo(escopo): resumo` e,
logo abaixo, tópicos explicando a mudança real — sem linha em branco entre eles e
sem ponto final:

```text
fix(save): keep the run clock when resuming
- Store the Waves state, live bosses and run stats in the save
- Apply start bonuses before the saved health so resuming no longer heals
```

Um commit por mudança que pode ser revertida sozinha. Só docs ou só ferramenta
vão separados de código. A regra completa do time está em
`documentation/padroes/PADRAOCOMMITS.MD` (local, fora do repositório).

## O que garante o fluxo

| Camada | O que garante | Onde |
|---|---|---|
| CI no PR | build, testes, ferramentas, assets de UI e branch de origem; verde, mescla na `develop` | `.github/workflows/ci.yml`, jobs até "Mesclar na develop" |
| CI na `develop` | a mesma bateria sobre o commit mesclado, mais o fat-jar; verde, promove a `main` só por avanço (nunca reescreve) | job "Promover para a main" |
| Hook `pre-push` | a CI local antes de todo push e a recusa de push direto para `develop` e `main` | `.githooks/pre-push`, instalado com `./gradlew hooks` |

**O que falta, e por quê.** O repositório não tem proteção de branch no GitHub:
ligá-la é configuração de administrador, e o único administrador é o dono do
repositório, que não acompanha mais o projeto. Por isso o merge é feito pelo
workflow (com o token do Actions, que todo colaborador com escrita já tem) e não
pelo auto-merge do GitHub, e por isso a trava contra push direto é o hook local —
que se contorna com `git push --no-verify`. Se alguém do time passar a ter papel de
administrador, vale ligar um ruleset para `develop` e `main` exigindo PR e os checks
"Build e testes", "Assets de UI em dia" e "Branch de origem".

## Quando algo sai do trilho

- **"A main tem commits que a develop não tem."** Alguém empurrou direto na
  `main`. A promoção para de propósito, em vez de escolher um lado. Traga a `main`
  para a `develop` numa branch (`git switch -c fix/trazer-main origin/develop` e
  `git merge origin/main`), abra o PR para a `develop` e, quando ele entrar, a
  promoção volta a andar.
- **PR de fork.** O token de um fork não tem escrita, então o workflow não mescla.
  Alguém do time revisa e mescla pela tela.
- **A promoção não aconteceu depois do merge.** Veja a execução "CI" disparada na
  `develop` (aba Actions). Se ela não existir, rode-a à mão:
  `gh workflow run ci.yml --ref develop`.

## Primeira vez

Uma vez por clone, para o `git push` passar pela CI local e respeitar as travas:

```sh
./gradlew hooks
```

A `develop` foi criada a partir da `main` quando o fluxo entrou (2026-10). Quem
clonou antes só precisa de `git fetch origin`.
