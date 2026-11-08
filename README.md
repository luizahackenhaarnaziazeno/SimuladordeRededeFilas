<div align="center">
  
## Simulador de Rede de Filas
  
## 🎯 Sobre o Projeto
O projeto consiste em um simulador de eventos discretos desenvolvido em Java para modelar uma rede de três filas interconectadas. O algoritmo avalia o comportamento de um sistema de atendimento através do cálculo de tempos acumulados, probabilidades de estado e contagem de clientes perdidos. Para garantir a reprodutibilidade dos testes, utiliza-se uma semente fixa (42) na geração de 100.000 números pseudoaleatórios.

A rede simula o tráfego pelas seguintes instâncias:
* **Fila 1:** Modelo G/G/1 com capacidade considerada infinita (1000 posições), recebendo chegadas externas (intervalos de 2 a 4 minutos) e processando atendimentos de 1 a 2 minutos.
* **Fila 2:** Modelo G/G/2/5 com tempo de serviço entre 4 e 6 minutos. 
* **Fila 3:** Modelo G/G/2/10 com tempo de serviço de 5 a 15 minutos.

As transições entre as filas ocorrem através de um roteamento com probabilidades predefinidas (por exemplo, 80% dos clientes da Fila 1 seguem para a Fila 2, e 20% para a Fila 3).

## ⚙️ Pré-requisitos e Dependências
O simulador foi construído de forma nativa e não requer bibliotecas ou frameworks externos. Ele faz uso exclusivo de estruturas padrão do pacote `java.util` (como `PriorityQueue`, `Random` e `HashMap`).
* Java Development Kit (JDK) 8 ou superior instalado.

## 📁 Estrutura de Diretórios
Por se tratar de um script único, a estrutura do projeto é minimalista:
```text
/
└── SimuladordeRededeFilas.java   # Arquivo fonte contendo o simulador, classes de Fila e de Eventos
```

## 🚀 Como Executar
Abra o terminal na pasta onde o arquivo se encontra e execute os comandos de compilação e execução padrão do Java:

1. **Compilação:**
   ```bash
   javac SimuladordeRededeFilas.java
   ```
2. **Execução:**
   ```bash
   java SimuladordeRededeFilas
   ```
Os resultados da simulação serão impressos diretamente na saída padrão (console).

## 🛠️ Pipeline de Execução
O fluxo de simulação é baseado no processamento cronológico de eventos:

* **1. Inicialização:** As rotas probabilísticas e as três filas são instanciadas com seus respectivos parâmetros de servidores, capacidade e tempos. Uma chegada externa inicial é engatilhada na Fila 1 no tempo 2.0.
* **2. Escalonamento:** A fila de prioridade (`PriorityQueue`) enfileira eventos (`Event`), garantindo que o evento de menor tempo ocorra primeiro. Em caso de empate de tempo, eventos de saída (`Departure`) têm prioridade sobre chegadas (`Arrival`).
* **3. Processamento de Chegadas (`Arrival`):** Quando um cliente entra no sistema, o cronômetro do estado atual da fila é contabilizado. Se houver espaço, o cliente é aceito; caso a fila esteja cheia, é marcado como cliente perdido. Se houver capacidade de atendimento livre, um evento de saída é agendado. Se o evento ocorrer na Fila 1, a próxima chegada externa já é colocada no calendário de eventos.
* **4. Processamento de Saídas (`Departure`):** O cliente deixa o serviço e o tempo ocupado pelo estado anterior é registrado. O servidor liberado avalia se há outros clientes esperando; se sim, sorteia-se um novo tempo de serviço e um novo evento de saída é agendado. O cliente recém-atendido é então direcionado (roteado probabilisticamente) de imediato para a próxima fila ou para fora do sistema.
* **5. Finalização:** O laço de repetição finaliza quando o limite de números aleatórios se esgota ou quando não há mais eventos agendados. Em seguida, relatórios estatísticos contendo o tempo acumulado e a distribuição das probabilidades para cada estado de cada fila são apresentados ao usuário.


# Autora:

| [<img loading="lazy" src="https://avatars.githubusercontent.com/u/142232479?v=4" width=115><br><sub>Luiza Hackenhaar Naziazeno</sub>](https://github.com/luizahackenhaarnaziazeno)|
| :---: |
