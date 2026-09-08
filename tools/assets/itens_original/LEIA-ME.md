# Arte de item original

Backup dos sprites de item como estavam antes da repaginação, guardado a pedido:
nada aqui é lido pelo jogo nem por gerador nenhum.

## Por que a repaginação

Medido, e é o motivo de tudo: o corpo do gato tem **13px** de altura e as armas
ocupam **15 a 16px** do quadro de 16. Em diagonal isso dá **160 a 175% da altura
do gato** — a espada é mais comprida que o bicho que a segura.

| medida | valor |
|---|---|
| corpo do gato | 13 px |
| arma desenhada (média das 22) | ~21 px de diagonal |
| proporção | **168%** |
| alvo recomendado | 8–10 px, ~⅔ do gato |

## O que fazer se quiser algum de volta

Copiar o arquivo de volta para
`src/main/resources/com/retronova/resources/sprites/items/`. O nome importa: é
por ele que a classe do item procura o sprite (ver o terceiro argumento do
`super(...)` em `game/items/`).
