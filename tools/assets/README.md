# Fontes de arte versionadas

Só entra aqui o que for pequeno demais para valer um download, e sempre em CC0.
O resto — como o pacote do Kenney — fica de fora e é baixado na hora de gerar.

| Arquivo | Origem | Autor | Licença |
|---|---|---|---|
| `torch_anim_cc0.png` | [Simple Torch Animation 16x16](https://opengameart.org/content/simple-torch-animation-16x16) | Natural_Privateer | CC0 1.0 |

`torch_anim_cc0.png` é a folha original, 48x32, com cinco quadros de chama de
16x16 (o sexto está vazio). `tools/GenKenney.java` a consome para montar a tocha
e o braseiro; os PNG do jogo são derivados, não cópias.

## Arte original preservada

| Arquivo | O que é |
|---|---|
| `finn_original_idle.png`, `finn_original_walking.png` | Sprites do Finn como o artista os entregou |
| `gato_antigo_muffin.png`, `gato_antigo_azrael.png`, `gato_antigo_finn.png` | Os três jogáveis antes do redesenho |
| `portao_original_gate.png` | A moldura do portão antes de virar folha animada |

Os `gato_antigo_*` são a **fonte de cor** de `tools/GenGatos.java`, e é só isso
que sobrou deles em uso. O gerador redesenha os três com a construção do gato
vendedor — rabo no chão, barriga clara, corpo compacto — mas amostra dali cada
pelagem, cada orelha, cada olho e cada veste. É o que garante que o Muffin
continue sendo o cinza que ele era, e não um cinza parecido.

Os `finn_original_*` são o Finn de antes de virar gato da sorte, guardados como
referência histórica: nada mais lê deles desde que `GenFinn.java` saiu.

`portao_original_gate.png` é lido por `tools/GenPortao.java`, que reaproveita a
moldura intacta e redesenha só o miolo em oito quadros. Sem essa cópia, a segunda
execução leria a folha de oito quadros como se fosse a moldura de um quadro só.
