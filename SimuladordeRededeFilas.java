import java.util.PriorityQueue;
import java.util.Random;
import java.util.HashMap;
import java.util.Comparator;

public class SimuladordeRededeFilas {

    // Constantes de Simulação
    private static final int NUM_RANDOMS = 100000;
    private static final Random random = new Random(42); // Semente fixa para resultados reproduzíveis
    private static int randomsUsed = 0;

    // Classe para a entidade Fila
    static class Fila {
        private final int id;
        private final int numServers;
        private final int capacity;
        
        // Parâmetros de chegada externa (usados apenas pela Fila 1)
        private final double minArrival;
        private final double maxArrival;
        
        // Parâmetros de serviço
        private final double minService;
        private final double maxService;
        
        // Roteamento Probabilístico: {ID_Fila_Destino: Probabilidade} (0 para saída)
        private final HashMap<Integer, Double> routingProbabilities;

        private int customers; // Clientes atualmente no sistema (espera + serviço)
        private int lostCustomers;
        private double lastEventTime;
        private double totalTime;
        private final HashMap<Integer, Double> accumulatedTimeByState;

        /**
         * Construtor uniformizado para todas as filas.
         * @param id ID da fila
         * @param numServers Número de servidores
         * @param capacity Capacidade máxima (incluindo servidores)
         * @param minArr Mínimo tempo entre chegadas (apenas para chegada externa)
         * @param maxArr Máximo tempo entre chegadas (apenas para chegada externa)
         * @param minServ Mínimo tempo de serviço
         * @param maxServ Máximo tempo de serviço
         * @param routingProbs Mapa de {Destino ID : Probabilidade}
         */
        public Fila(int id, int numServers, int capacity, 
                    double minArr, double maxArr, double minServ, double maxServ,
                    HashMap<Integer, Double> routingProbs) {
            this.id = id;
            this.numServers = numServers;
            this.capacity = capacity; 
            this.minArrival = minArr;
            this.maxArrival = maxArr;
            this.minService = minServ;
            this.maxService = maxServ;
            this.routingProbabilities = routingProbs;
            
            this.customers = 0;
            this.lostCustomers = 0;
            this.lastEventTime = 0.0;
            this.totalTime = 0.0;
            
            this.accumulatedTimeByState = new HashMap<>();
            // Inicializa os estados possíveis de 0 até a capacidade (K)
            for (int i = 0; i <= capacity; i++) {
                this.accumulatedTimeByState.put(i, 0.0);
            }
        }

        // Métodos de manipulação
        public boolean in(double eventTime) {
            updateAccumulatedTime(eventTime);
            if (this.customers < this.capacity) {
                this.customers++;
                return true; // Cliente entrou
            } else {
                this.lostCustomers++;
                return false; // Cliente perdido
            }
        }

        public void out(double eventTime) {
            updateAccumulatedTime(eventTime);
            this.customers--;
        }

        public void updateAccumulatedTime(double currentTime) {
            double timeInState = currentTime - this.lastEventTime;
            if (timeInState > 0) {
                // O estado a ser registrado é o estado ANTERIOR à mudança
                // Se customers=N, o tempo foi gasto no estado N-1 (para uma 'in') ou N+1 (para uma 'out')
                // No entanto, para simplicidade e precisão, registramos o tempo no estado 'this.customers'
                // que é o estado atual do sistema ANTES que o evento que chama 'updateAccumulatedTime' mude o estado.
                
                // Como 'updateAccumulatedTime' é chamada ANTES de customers ser incrementado/decrementado em 'in'/'out',
                // o valor de 'this.customers' neste ponto é o estado ANTERIOR.
                int stateToUpdate = this.customers;
                this.accumulatedTimeByState.put(stateToUpdate, 
                    this.accumulatedTimeByState.getOrDefault(stateToUpdate, 0.0) + timeInState);
                this.totalTime += timeInState;
            }
            this.lastEventTime = currentTime;
        }

        // Métodos de acesso
        public int getCustomers() { return customers; }
        public int getNumServers() { return numServers; }
        public int getLostCustomers() { return lostCustomers; }
        public double getMinService() { return minService; }
        public double getMaxService() { return maxService; }
        public double getMinArrival() { return minArrival; }
        public double getMaxArrival() { return maxArrival; }
        public HashMap<Integer, Double> getRoutingProbabilities() { return routingProbabilities; }

        public void printResults() {
            String title;
            String details;
            
            // Descrições baseadas no diagrama
            if (this.id == 1) {
                title = "Fila 1";
                details = "G/G/1, Chegadas Externas (2..4 min), Serviço (1..2 min), K=Infinito (1000)";
            } else if (this.id == 2) {
                title = "Fila 2";
                details = "G/G/2/5, Serviço (4..6 min)";
            } else if (this.id == 3) {
                title = "Fila 3";
                details = "G/G/2/10, Serviço (5..15 min)";
            } else {
                title = "Fila " + this.id;
                details = "Desconhecido";
            }
            
            System.out.println("=====================================================");
            System.out.println("  " + title + " - " + details);
            System.out.println("=====================================================");
            
            System.out.printf("Tempo Acumulado Total: %.4f unidades de tempo%n", this.totalTime);
            System.out.printf("Número de Clientes Perdidos: %d%n", this.lostCustomers);

            System.out.println("\n--- Tempos Acumulados por Estado do Sistema ---");
            accumulatedTimeByState.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(HashMap.Entry.comparingByKey())
                .forEach(entry -> System.out.printf("Estado %d: %.4f unidades de tempo%n", entry.getKey(), entry.getValue()));
            
            System.out.println("\n--- Distribuição de Probabilidades dos Estados do Sistema ---");
            if (this.totalTime > 0) {
                accumulatedTimeByState.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(HashMap.Entry.comparingByKey())
                    .forEach(entry -> System.out.printf("Estado %d: %.4f%%%n", entry.getKey(), (entry.getValue() / this.totalTime) * 100));
            } else {
                System.out.println("N/A (Tempo total da fila é zero)");
            }
            System.out.println();
        }
    }

    // Classe interna para representar eventos
    static class Event implements Comparable<Event> {
        private final double eventTime;
        private final String eventType;
        private final int queueId;

        public Event(double eventTime, String eventType, int queueId) {
            this.eventTime = eventTime;
            this.eventType = eventType;
            this.queueId = queueId;
        }

        public double getEventTime() { return eventTime; }
        public String getEventType() { return eventType; }
        public int getQueueId() { return queueId; }

        @Override
        public int compareTo(Event other) {
            // Ordena pelo tempo do evento (menor tempo primeiro)
            int timeComparison = Double.compare(this.eventTime, other.eventTime);
            if (timeComparison != 0) {
                return timeComparison;
            }
            // Critério de desempate: 'Departure' tem prioridade sobre 'Arrival' no mesmo instante
            // Isso simula o cliente saindo e o próximo entrando na rede imediatamente (tempo 0 de trânsito)
            if (this.eventType.equals("Departure") && other.eventType.equals("Arrival")) {
                return -1;
            } else if (this.eventType.equals("Arrival") && other.eventType.equals("Departure")) {
                return 1;
            }
            return 0;
        }
    }

    // Método para gerar números aleatórios dentro de um intervalo (Uniforme)
    private static double generateRandom(double min, double max) {
        if (randomsUsed >= NUM_RANDOMS) {
            return 0; // Retorna 0 se acabarem os aleatórios, garantindo que o loop termine
        }
        double r = random.nextDouble() * (max - min) + min;
        randomsUsed++;
        return r;
    }
    
    // Método para escolher a próxima fila com base nas probabilidades de roteamento
    private static int chooseNextQueue(HashMap<Integer, Double> routingProbs) {
        if (randomsUsed >= NUM_RANDOMS) {
            return 0; // Força a saída se acabarem os aleatórios
        }
        double r = random.nextDouble();
        randomsUsed++;
        double cumulativeProb = 0.0;

        // Itera sobre as probabilidades para sortear o destino
        for (HashMap.Entry<Integer, Double> entry : routingProbs.entrySet()) {
            cumulativeProb += entry.getValue();
            if (r <= cumulativeProb) {
                return entry.getKey(); // Retorna o ID da próxima fila (0 = Externo)
            }
        }
        return 0; // Padrão: sai do sistema
    }

    public static void main(String[] args) {
        // 1. Definição das Rotas Probabilísticas (0 = Saída Externa)
        
        // Fila 1: 0.8 -> Fila 2, 0.2 -> Fila 3
        HashMap<Integer, Double> route1 = new HashMap<>();
        route1.put(2, 0.8);
        route1.put(3, 0.2);

        // Fila 2: 0.2 -> Externo (0), 0.3 -> Fila 1, 0.5 -> Fila 3
        HashMap<Integer, Double> route2 = new HashMap<>();
        route2.put(0, 0.2); 
        route2.put(1, 0.3); 
        route2.put(3, 0.5); 

        // Fila 3: 0.3 -> Externo (0), 0.7 -> Fila 2
        HashMap<Integer, Double> route3 = new HashMap<>();
        route3.put(0, 0.3); 
        route3.put(2, 0.7); 

        // 2. Inicializa as Filas
        // Fila(id, numServers, capacity, minArr, maxArr, minServ, maxServ, routingProbs)

        // Fila 1: G/G/1 (Cap. grande - 1000)
        Fila fila1 = new Fila(1, 1, 1000, 2.0, 4.0, 1.0, 2.0, route1); 
        
        // Fila 2: G/G/2/5 (Cap. 5) - Chegada apenas interna (minArr/maxArr = 0)
        Fila fila2 = new Fila(2, 2, 5, 0, 0, 4.0, 6.0, route2); 
        
        // Fila 3: G/G/2/10 (Cap. 10) - Chegada apenas interna (minArr/maxArr = 0)
        Fila fila3 = new Fila(3, 2, 10, 0, 0, 5.0, 15.0, route3); 

        // Mapeamento para acesso rápido
        HashMap<Integer, Fila> queues = new HashMap<>();
        queues.put(1, fila1);
        queues.put(2, fila2);
        queues.put(3, fila3);
        
        // O escalonador de eventos
        PriorityQueue<Event> eventScheduler = new PriorityQueue<>();
        
        // 3. Agendar a primeira chegada na Fila 1 no tempo 2.0
        eventScheduler.add(new Event(2.0, "Arrival", 1));
        
        // 4. Agendar a próxima chegada externa na Fila 1
        double firstInterArrivalTime = generateRandom(fila1.getMinArrival(), fila1.getMaxArrival());
        eventScheduler.add(new Event(2.0 + firstInterArrivalTime, "Arrival", 1));

        double globalTime = 0.0;
        
        // 5. Loop principal da simulação
        while (!eventScheduler.isEmpty() && randomsUsed < NUM_RANDOMS) {
            Event currentEvent = eventScheduler.poll();
            globalTime = currentEvent.getEventTime();
            String eventType = currentEvent.getEventType();
            int queueId = currentEvent.getQueueId();
            Fila currentFila = queues.get(queueId);

            if (currentFila == null) continue; 

            if (eventType.equals("Arrival")) {
                boolean accepted = currentFila.in(globalTime);

                if (accepted) {
                    // Se o cliente foi aceito e há capacidade de serviço (Clientes <= Servidores), agendar saída
                    // O cliente que acabou de chegar pode ocupar um servidor
                    if (currentFila.getCustomers() <= currentFila.getNumServers()) {
                        double serviceTime = generateRandom(currentFila.getMinService(), currentFila.getMaxService());
                        eventScheduler.add(new Event(globalTime + serviceTime, "Departure", queueId));
                    }
                }
                
                // Se for a Fila 1 e este for um evento de chegada externa, agendar a próxima chegada externa
                // Evita agendar chegadas para filas internas
                if (queueId == 1 && globalTime > 0) { 
                    double interArrivalTime = generateRandom(fila1.getMinArrival(), fila1.getMaxArrival());
                    if (randomsUsed < NUM_RANDOMS) {
                        eventScheduler.add(new Event(globalTime + interArrivalTime, "Arrival", 1));
                    }
                }

            } else if (eventType.equals("Departure")) {
                currentFila.out(globalTime); // Cliente sai do serviço

                // 1. Tentar iniciar atendimento para o próximo cliente na fila atual (se houver)
                // Se o número de clientes restantes for maior ou igual ao número de servidores, há espera e o servidor liberado
                // pode começar um novo serviço.
                if (currentFila.getCustomers() >= currentFila.getNumServers()) {
                    double serviceTime = generateRandom(currentFila.getMinService(), currentFila.getMaxService());
                    eventScheduler.add(new Event(globalTime + serviceTime, "Departure", queueId));
                }

                // 2. Roteamento probabilístico do cliente que acabou de sair
                int nextQueueId = chooseNextQueue(currentFila.getRoutingProbabilities());

                if (nextQueueId != 0) { // Se não for para a saída (ID 0)
                    // Chegada imediata (tempo 0 de trânsito) na próxima fila
                    eventScheduler.add(new Event(globalTime, "Arrival", nextQueueId));
                }
            }
        }

        // 6. Atualizar tempos acumulados finais e imprimir resultados
        for (Fila fila : queues.values()) {
            fila.updateAccumulatedTime(globalTime);
        }

        System.out.println("=====================================================");
        System.out.println("  FIM DA SIMULAÇÃO DA REDE DE FILAS");
        System.out.println("=====================================================");
        System.out.printf("Tempo Global da Simulação: %.4f unidades de tempo%n", globalTime);
        System.out.println("Total de Aleatórios Usados: " + randomsUsed);
        System.out.println();
        
        fila1.printResults();
        fila2.printResults();
        fila3.printResults();
    }
}
