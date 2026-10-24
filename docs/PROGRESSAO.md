# Progressão permanente e espelho do vendedor

Entrega na branch `feat/espelho`, em 2026-10-23. Implementação por personagem,
com moeda permanente separada dos dados da corrida. Pesquisa e decisões abaixo;
os valores são uma proposta inicial, sem alegar telemetria de jogadores.

## Diagnóstico e decisões

O save anterior de corrida já registrava vida, XP, nível, inventário, elemento,
cartas, passivas, armas extras, chefes vivos, relógio, Fúria e estatísticas.
Três espaços independentes em `saves/corrida-N.json`; o antigo `corrida.json`
é migrado para o primeiro espaço. Os checkpoints ocorrem na entrada da arena,
nas escolhas, a cada minuto e na saída pela pausa. A retomada consome o arquivo
e o regrava no primeiro tick; inimigos comuns e terreno são recriados.

`Meta` guardava ouro e cinco upgrades globais em `saves/meta.json`. A morte
abria uma loja junto do resumo. O vendedor da sala `beginning` possuía uma
loja de armas; o dinheiro local (`Player.money`, inicialmente 100) e as classes
`Coin`/`GumMachine` não tinham uma economia ativa na arena. **Não há coleta de
moeda física sendo convertida**: a nova moeda é calculada pelo tempo e abates.

Agora, apenas o vendedor abre `TelaDoEspelho`. `Try Again` leva à antecâmara,
onde é possível comprar ou seguir direto ao portão. `Run Over` tem somente
`Run report` e `Final build`, moedas recebidas e saldo do gato. O antigo `Meta`
foi substituído por `save/Progresso` e `espelho/Espelho`. A vitrine antiga,
inventário oculto, chiclete e moedas de chão permanecem em standby.

## Pesquisa e aplicação

Fontes primárias consultadas em 2026-10-23. As decisões de valores para este
jogo são nossas; não reproduzem a economia de outra obra.

| Referência | Evidência e decisão aplicada |
|---|---|
| [Supergiant: FAQ de Hades](https://www.supergiantgames.com/blog/hades-faq/) | A progressão permanente acompanha modificadores de dificuldade. Aplicamos bônus limitados, sem transformar moedas em uma condição de entrada na run. |
| [Supergiant: Welcome to Hell](https://www.supergiantgames.com/blog/hades-welcome-to-hell-update-patch-notes/) e [atualizações de Hades](https://www.supergiantgames.com/blog/hades-updates/) | Custos e ordenação do Mirror foram ajustados durante desenvolvimento, inclusive com devolução de Darkness. Adotamos tabela editável, níveis finitos e redistribuição. |
| [Supergiant: Warsong, Hades II](https://www.supergiantgames.com/blog/hades2-warsong-update/) e [notas oficiais](https://store.steampowered.com/news/app/1145350/view/503942841152569404) | O Altar combina efeitos distintos; as notas alteram mobilidade, recuperação e invulnerabilidade, custo de Grasp e custo de ampliar esse limite. Isso inspira alternativas de utilidade e feedback, além de dano/vida. Não introduzimos um segundo orçamento de ativação como Grasp nesta versão. |
| [Teddy Lee, designer de Rogue Legacy: Game Design Deep Dive](https://www.gamedeveloper.com/design/game-design-deep-dive-how-i-rogue-legacy-i-handles-tutorials-without-being-boring) | O designer descreve o ciclo entre escolha de herdeiro, melhorias, castelo e morte. A compra fica no intervalo entre tentativas, com retorno natural ao hub. |
| [Motion Twin: apresentação de Dead Cells](https://store.steampowered.com/app/588650/Dead_Cells/) | A descrição do desenvolvedor distingue a morte da preservação de caminhos, habilidades e armas desbloqueados. Separamos dados da tentativa e conquistas permanentes. Não copiamos seu risco de recursos. |
| [poncle: Vampire Survivors](https://store.steampowered.com/app/1794680/Vampire_Survivors/) | As dicas oficiais incentivam gastar ouro em melhorias e experimentar caminhos com reembolso gratuito. Aqui o reembolso é integral e gratuito por gato. |
| [Blobfish: Brotato](https://store.steampowered.com/app/1942280/Brotato/) | A apresentação destaca personagens, builds e compra entre ondas. Usamos a clareza das escolhas como referência; sua loja durante a tentativa não foi trazida para este fluxo. |
| [Oracle: Files, Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html) e [FileChannel](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/channels/FileChannel.html) | Validar, escrever em temporário, `force(true)` e trocar com `ATOMIC_MOVE`; guardar backup válido. Onde a troca atômica não existe, há fallback e recuperação. |

O material da GDC Vault sobre Rogue Legacy foi procurado, mas o endereço
redirecionou para manutenção. A fundamentação verificável usa o artigo do
próprio designer, em vez de atribuir conclusões a uma palestra não acessível.

**Recomendação de design:** futuramente considerar uma carteira compartilhada,
com níveis ainda específicos de cada gato. Evitaria que experimentar outro
personagem exigisse repetir toda a economia. A entrega mantém **carteiras e
melhorias por gato**, conforme solicitado; mudar isso exige migração explícita.

## Economia e tabela editável

Arquivo único: `src/main/resources/com/retronova/resources/progressao/espelho.json`.
`id` é uma chave de save estável; não renomear sem migração. `custos` contém
o preço de cada nível e define seu limite. `porNivel` usa unidades do jogo;
`exibicao`, `casas` e `texto` controlam a apresentação. Custos são positivos e
crescentes; efeitos e IDs duplicados são validados na carga.

`floor((segundos / 6 + abates / 8 + bônus de vitória) × fração × ganho do espelho)`.
Bônus de vitória: 200. Morte e vitória: 100%; abandono/reinício/substituição
ou exclusão do espaço: 50%. A vitória já registrada prevalece sobre abandono.
Tempo e abates são divisões reais, com arredondamento somente no total.

A morte remunera o esforço inteiro; os 50% de abandono desincentivam reiniciar
só para receber. Essa diferença é uma decisão deste jogo, configurável no JSON.
Sair salvando pela pausa **suspende**, não abandona e não deposita ainda.
Fechamento forçado no meio da run volta ao último checkpoint, sem liquidar a run.
Vencer e escolher continuar mantém o bônus pendente até finalizar a tentativa.

| ID / melhoria | Por nível | Máximo | Custos por nível | Total |
|---|---|---:|---|---:|
| vida / Vitality | +6 vida | 5 (+30) | 40, 80, 140, 220, 320 | 800 |
| dano / Might | +1 dano | 5 (+5) | 60, 120, 200, 300, 420 | 1100 |
| alcance / Keen Eye | +0,5 alcance | 4 (+2) | 50, 110, 190, 290 | 640 |
| resistencia / Thick Fur | +3 pontos percentuais de redução | 4 (+12 pp) | 80, 160, 260, 380 | 880 |
| velocidade / Swiftness | +0,08 velocidade | 5 (+0,40) | 40, 80, 140, 220, 320 | 800 |
| coleta / Magnet | +10 pp no multiplicador de coleta | 5 (+50 pp) | 30, 60, 100, 150, 210 | 550 |
| critico / Precision | +2 pp de crítico | 5 (+10 pp) | 60, 120, 200, 300, 420 | 1100 |
| sorte / Fortune | +1 sorte | 5 (+5) | 40, 80, 140, 220, 320 | 800 |
| rerolls / Second Thoughts | +1 troca por run | 3 (+3) | 100, 220, 400 | 720 |
| cartas / Wider Hand | +1 opção na mão | 1 (+1) | 600 | 600 |
| moedas / Greed | +10 pp no multiplicador de ganho | 5 (+50 pp) | 60, 130, 220, 330, 460 | 1200 |

São **47 níveis e 9190 moedas por gato**, 27570 para os três. A resistência
total mantém o teto existente de 80%, somando habilidade, espelho e cartas.
Sorte também influencia crítico, esquiva e baús pela curva já existente.

### Estimativa de ritmo

Hipóteses ilustrativas, não médias observadas. Sem Greed, sem abandono:

| Tentativa modelada | Recompensa | Runs para a primeira compra (30–80) | Runs para 9190 |
|---|---:|---:|---:|
| 5 min, 160 abates, morte | 70 | 1–2 | 132 |
| 10 min, 400 abates, morte | 150 | 1 | 62 |
| 15 min, 1000 abates, morte | 275 | 1 | 34 |
| 30 min, 2500 abates, vitória | 812 | 1 | 12 |

No cenário de 15 min, o limite idealizado com Greed máximo desde o começo
seria 23 runs (412 moedas cada), mas ele custa 1200 e precisa ser comprado;
o ritmo real fica entre esse limite e as 34 tentativas sem ganho extra.
No cenário de 10 min são 41–62; no de 30 min, 8–12. Os primeiros níveis
devem caber nas primeiras 1–2 runs; a conclusão inteira é opcional.
Se jogadores iniciantes ficarem presos no cenário de 5 min, o custo completo
será longo demais: revisar preços/recompensa com tempo, abates e moedas
observados antes de tratar a proposta como balanceamento final.

## Saves, migração e recuperação

`saves/progresso.json`, versão 2:

```json
{"versao":2,"gatos":{"Muffin":{"moedas":120,"ganhas":900,"melhorias":{"vida":2}}},"creditadas":["id-da-run"]}
```

`ganhas` é o total histórico de entradas, independente de gastos. IDs pagos
não expiram; limpar a progressão mantém os IDs para não reabrir créditos antigos.
Não compartilhar uma mesma pasta de saves entre duas instâncias simultâneas:
a sincronização é interna ao processo; não há lock entre processos nem nuvem.

`corrida-N.json`, versão 2, contém `id` e os dados da tentativa. Versão 1
ganha um identificador determinístico pelo conteúdo, estável em releituras,
e a próxima gravação usa versão 2. Nenhum ordinal de item foi removido.

O `meta.json` antigo tem `ouro`, `VIGOR`, `FORCA`, `AGILIDADE`, `SORTE`, `IMA`.
Se não há progresso novo, seus níveis são copiados para cada gato, com IDs
vida/dano/velocidade/sorte/coleta. Ouro é dividido exatamente entre os três;
resto na ordem Muffin, Azrael, Finn (100 vira 34/33/33). Somente após gravar
o novo progresso, o original vira `meta.json.migrado`. Falhar não arquiva o
original nem confirma a migração. Um progresso atual existente prevalece.

Toda gravação valida o conteúdo antes de substituir, força o temporário ao
dispositivo e preserva o principal válido em `.bak`, também por troca de arquivo.
Leitura inválida separa o principal em `.corrompido` e tenta o backup. O backup
recuperado fica disponível em memória e a próxima gravação recria o principal.
Se ambos estão corrompidos, inicia estado vazio e registra o ocorrido no log;
os arquivos de evidência permanecem para recuperação manual. Erros de acesso
e formatos de versão incompatível não são tratados como progresso novo.

**Crédito interrompido:** antes de depositar, é gravado `credito-<hash>.json`,
com id, gato e moedas finais. Moedas e id pago entram juntos no progresso.
Depois, o recibo é removido e o save da run é descartado. Reabrir o jogo
reprocessa recibos pendentes antes de oferecer a corrida, respeitando o id
pago. Fechar entre depósito e limpeza não duplica; falhar ao gravar não muda
o saldo em memória. Compras, reembolso e reset também persistem uma cópia antes
de publicar o estado. Falhas recuperáveis oferecem tentativa de gravação.

Para recuperar manualmente: fechar o jogo, copiar a pasta inteira e restaurar
o `.bak` como arquivo principal. Um backup representa o estado anterior;
recuperá-lo pode reverter a última operação. Não há promessa de resistência a
qualquer falha de hardware: `force` e atomicidade dependem do dispositivo e
sistema de arquivos. Esta proteção cobre a interrupção do processo e a
recuperação do último estado válido, não adulteração deliberada dos saves.

`Settings → Interface → Reset progress`, disponível fora da partida, exige
confirmação. Zera todos os gatos, mantendo saves de run e tombstones dos créditos.
Uma run antiga retomada depois do reset recebe os permanentes atuais (zerados)
e seus buffs próprios; o bônus permanente não está embutido nos buffs salvos.

## Camadas, interface e standby

Base imutável em `players/*.json` → bônus do espelho em um gato novo → buffs
da run. Na retomada, aplica primeiro os permanentes, depois os buffs e a vida
salva. O espelho aplica só a diferença nas compras no hub, pois aquele gato
já será o da run. `Status` mostra `Base / Mirror / Run`; crítico inclui o
efeito indireto da sorte na parte Run, explicado na ajuda.

Compra: moedas, próximo efeito/custo, limite e níveis visíveis; som de moeda,
clarão de 18 ticks e preço subindo por 36. Recusa: tremor de 10 ticks.
Setas/D-pad escolhem; Enter/A compra; clique compra; Esc/B volta. `Refund all`
tem confirmação e devolve tudo daquele gato. Usa componentes e paleta dos menus.

Somente **Sword/Muffin, Wand/Azrael e Bow/Finn**, com elementos, ficam ativas.
`ItemIDs.EM_STANDBY` isola as outras armas de mão; `ArmaExtra.ATIVAS=false`
desliga seis orbitais/armas de área, evoluções e suas ofertas. A passiva Haste,
que só afetava essas armas, também deixa de ser oferecida. Dados antigos
permanecem serializados; uma arma antiga equipada usa a padrão como reserva,
sem apagar o inventário. Para reativar, revisar `ItemIDs`, a proteção da mão
em `Player.tickItemHand`, `ArmaExtra.ATIVAS`, ofertas e testes de baú.
Não basta religar a vitrine antiga: compras continuam exclusivas do espelho.

## Validação e roteiro manual

Automatizado: `gradlew test` cobre persistência, crédito idempotente, recibo
interrompido, falhas de compra/reembolso/migração, backup da corrida, id legado,
versão futura preservada, regras de ganho e tabela. Testes de corrida usam
`@TempDir`; não movem saves pessoais.

`gradlew verificarProgressao` abre uma bancada em diretório temporário dentro
de `build/`, com Foto, sem áudio/foco/saves reais. Verifica atributos reais,
template intacto, compra das melhorias, camadas com cartas, retomada,
reembolso, separação de gatos/rerolls, armas em standby e morte com descarte
da corrida. Salva PNGs do espelho, Status e Run Over. Outra resolução:
`gradlew verificarProgressao -PbancadaResolucao=1280x720`.

Roteiro de aceitação em uma cópia dos saves:

- [ ] Iniciar com cada gato, visitar vendedor, comprar e fechar/reabrir o jogo.
- [ ] Comparar Base/Mirror/Run, receber cartas, suspender e retomar sem duplicar bônus ou curar a vida salva.
- [ ] Morrer, conferir moedas recebidas e saldo; `Try Again` volta ao hub sem compra obrigatória.
- [ ] Vencer: encerrar e conferir bônus; repetir continuando e suspendendo antes de encerrar.
- [ ] Abandonar, reiniciar, substituir e excluir um espaço; os outros espaços e melhorias permanecem.
- [ ] Reembolsar/cancelar confirmação; comprar sem saldo e no máximo; trocar de gato sem herdar rerolls.
- [ ] Navegar todas as opções por mouse, teclado e controle físico; confirmar áudio/feedback.
- [ ] Testar modo janela/tela cheia e escalas de interface; verificar ausência de cortes em resoluções menores.
- [ ] Interromper o processo antes/depois do depósito usando dados descartáveis; crédito único ao voltar.
- [ ] Bloquear gravação em uma pasta descartável; saída/compra não confirmam sucesso e permitem nova tentativa.
- [ ] Corromper principal com backup bom; recuperar. Testar ambos ruins e versão futura, preservando evidências.
- [ ] Migrar meta/corrida antigos; confirmar valores e original preservado. Resetar só após conferir a confirmação.

Controle físico, percepção de timbre, sessão longa de 30 min e ritmo médio de
jogadores continuam **a confirmar** por aceitação manual; não são resultados
inferidos dos testes automatizados.
