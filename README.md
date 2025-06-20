![Image](https://i.imgur.com/k7O5GhT.jpeg)

# 🎮 Roguelike Game

Um jogo roguelike desenvolvido em **Java**, focado em exploração e sobrevivência.

## 🚀 Tecnologias Utilizadas
- **Java** – Para a lógica do jogo e mecânicas.
- **Biblioteca TinySound** – Para reprodução de sons.
- **JSON** – Para salvar estados.

## 🧠 Técnicas e Algoritmos
- **Distância Euclidiana** - Usada para calcular a distância direta entre dois pontos (ex: inimigo e jogador), essencial para lógica de perseguição e detecção de proximidade;
- **Array** – Utilizado para gerenciar o inventário do jogador, com estrutura simples e eficiente para os 20 slots de armazenamento com 5 de uso rápido; permite inserções e consultas rápidas;
- **Algoritmo A\*** – Empregado para pathfinding, permitindo que os inimigos calculem o melhor caminho até o jogador de forma inteligente e estratégica, considerando obstáculos e terrenos;
- **Colisão Retangular (AABB)** – Detecta colisão entre objetos com axis-aligned bounding boxes, ideal para ambientes 2D por sua simplicidade e baixo custo computacional;
- **Física Vetorial** - Utiliza vetores para representar e atualizar movimento e direção das entidades no espaço, garantindo movimentação fluida e reações realistas;
- **Física Avançada** - Incorpora conceitos de peso, força e fricção para simular interações físicas mais convincentes, como aceleração, quedas e deslizamentos, elevando a imersão no gameplay.

## 🎲 Características
- **Sistema de combate estratégico** ⚔️
- **Progressão dinâmica do personagem** 🏆
- **Ambiente interativo e desafiador** 🔥
- **História Divertida** 🎮

## 🧍 Personagem
- O jogador poderá escolher entre três variantes de personagens, cada um com características específicas.  
O personagem terá atributos que, com o avanço das ondas de monstros, poderão ser melhorados.

## 📈 Atributos
- **Nível do personagem**: Influencia a sorte, descoberta de estruturas, itens, etc.
- **Habilidades**: Melhoram as características do personagem e ajudam a enfrentar ondas mais difíceis.
- **Experiência**: Ganha-se com o tempo e é usada para desbloquear e melhorar habilidades.

## 🎯 Objetivos
O foco do jogo é sobreviver ao maior número de ondas de monstros.  
A cada pausa entre ondas, o jogador poderá usar coletáveis para comprar ou trocar itens numa loja.  
Estruturas interativas trarão novos desafios, recompensas e caminhos.

## 🏰 Estruturas
Surgirão aleatoriamente em ondas mais avançadas, oferecendo novos mapas com desafios, puzzles e chefes.  
São fontes de itens raros e progressão.

## 🎒 Inventário
- Até 15 slots para armazenamento
- Até 5 slots de uso rápido
- Inventário volátil: morte implica na perda de itens, habilidades e nível.

## 🧪 Itens
Itens aparecem em locais específicos, exigem habilidades ou níveis para uso, e são sempre voláteis ao ser derrotado.

## 👥 Equipe
- Lucas Zonzini
- Caio Renato
- Caio Gabriel
- Jose Francisco

## 📄 Documento PRD
[Link para o PRD](https://1drv.ms/w/c/c48d086d91293c36/EUd1CRhXAlRMs13bkpAg2qIBFBFy7Ttzm244rH9gDyEhtQ?e=7l7BbQ)
