import java.util.*;

public class Escalonador {

    enum TipoIO { DISCO, FITA, IMPRESSORA }

    static final int Processos_maximos = 10; // número máximo de processos
    static final int Prioridade_maxima = 3; // 0 = alta, 1 = baixa, 2 = I/O
    static final int[] Quantums = {4, 8, 0}; // quantum de cada fila

    // Dados dos processos
    static int[] id = new int[Processos_maximos]; // identificação dos processos
    static int[] chegada = new int[Processos_maximos]; // demonstra o momento de chegada de cada processo
    static int[] burst = new int[Processos_maximos]; // tempo dos processos
    static int[] restante = new int[Processos_maximos]; // tempo restante do processo
    static int[] prioridade = new int[Processos_maximos]; // prioridade do processos
    static int[] espera = new int[Processos_maximos]; // demonstra a espera de cada processo
    static int[] cicloDeVida = new int[Processos_maximos]; // define quantos ciclos durou o processo
    static boolean[] terminou = new boolean[Processos_maximos]; // demonstra quando o processo terminou

    // Controle de I/O
    static TipoIO[] tipoIO = new TipoIO[Processos_maximos];
    static int[] tempoRetornoIO = new int[Processos_maximos];

    static Queue<Integer>[] filas = new LinkedList[Prioridade_maxima];
    static int n;
    static int tempo = 0;
    static Random rand = new Random();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Digite a quantidade de processos (máx. " + Processos_maximos + "): ");
        n = sc.nextInt();
        if (n > Processos_maximos)
            n = Processos_maximos;

        for (int i = 0; i < Prioridade_maxima; i++) filas[i] = new LinkedList<>();
        Arrays.fill(tipoIO, null);
        Arrays.fill(tempoRetornoIO, 0);

        // Entrada de processos
        for (int i = 0; i < n; i++) {
            id[i] = i + 1;
            chegada[i] = 0;
            burst[i] = rand.nextInt(16) + 5; // Aleatório entre 5 e 2   0
            System.out.printf("Processo P%d criado com burst time de %d unidades\n", id[i], burst[i]);
        }

        // Inicialização
        for (int i = 0; i < n; i++) {
            restante[i] = burst[i];
            prioridade[i] = 0; // todos começam na fila alta
            terminou[i] = false;
            filas[0].add(i);
        }

        simula();

        System.out.println("\nResumo Final:");
        System.out.println("P# | Espera | cicloDeVida");
        for (int i = 0; i < n; i++) {
            System.out.printf("P%d | %6d | %10d\n", id[i], espera[i], cicloDeVida[i]);
        }

        sc.close();
    }

    static boolean temProcessos() {
        for (int i = 0; i < Prioridade_maxima; i++) if (!filas[i].isEmpty()) return true;
        return false;
    }

    static void simula() {
        while (temProcessos()) {

            // Verifica processos que terminaram I/O
            Queue<Integer> prontosParaVoltar = new LinkedList<>();
            for (int i = 0; i < n; i++) {
                if (prioridade[i] == 2 && tempo >= tempoRetornoIO[i] && restante[i] > 0) {
                    prontosParaVoltar.add(i);
                }
            }

            for (int idx : prontosParaVoltar) {
                filas[2].remove(idx);

                // Direciona de acordo com o tipo de I/O
                if (tipoIO[idx] == TipoIO.DISCO) {
                    prioridade[idx] = 1; // baixa prioridade
                    filas[1].add(idx);
                } else {
                    prioridade[idx] = 0; // alta prioridade
                    filas[0].add(idx);
                }
                System.out.printf("Tempo %d: P%d retornou da I/O (%s) e vai para fila %s\n",
                        tempo, id[idx], tipoIO[idx], prioridade[idx] == 0 ? "alta" : "baixa");
            }

            boolean executado = false;

            // Executa fila alta (0) ou baixa (1)
            for (int prio = 0; prio < 2; prio++) {
                if (!filas[prio].isEmpty()) {
                    int idx = filas[prio].poll();
                    int quantum = Quantums[prio];
                    int execTime = Math.min(quantum, restante[idx]);

                    System.out.printf("Tempo %d: Executando P%d [Prioridade %d] por %d unidades\n",
                            tempo, id[idx], prio, execTime);

                    tempo += execTime;
                    restante[idx] -= execTime;

                    atualizaEspera(idx, execTime);

                    if (restante[idx] == 0) {
                        terminou[idx] = true;
                        cicloDeVida[idx] = tempo - chegada[idx];
                        System.out.printf(" -> P%d Finalizado no tempo %d\n", id[idx], tempo);
                    } else {
                        // Preempção ou envio para I/O
                        if (rand.nextBoolean()) {
                            // Vai para I/O
                            TipoIO tipo = TipoIO.values()[rand.nextInt(TipoIO.values().length)];
                            tipoIO[idx] = tipo;

                            // Tempo de I/O aleatório entre 3 e 15
                            int duracaoIO = rand.nextInt(13) + 3;
                            tempoRetornoIO[idx] = tempo + duracaoIO;

                            prioridade[idx] = 2;
                            filas[2].add(idx);
                            System.out.printf(" -> P%d foi para I/O (%s) por %d unidades, retorna no tempo %d\n",
                                    id[idx], tipo, duracaoIO, tempoRetornoIO[idx]);
                        } else if (prio == 0) {
                            // Preempção da fila alta → vai para baixa
                            prioridade[idx] = 1;
                            filas[1].add(idx);
                            System.out.printf(" -> P%d foi preemptado e vai para fila baixa\n", id[idx]);
                        } else {
                            // Permanece na fila baixa
                            filas[1].add(idx);
                        }
                    }

                    executado = true;
                    break; // só executa um processo por ciclo
                }
            }

            if (!executado) {
                System.out.printf("Tempo %d: CPU ociosa\n", tempo);
                tempo++;
            }
        }
    }

    static void atualizaEspera(int idxExecutado, int execTime) {
        for (int i = 0; i < n; i++) {
            if (i != idxExecutado && !terminou[i] && prioridade[i] != 2) {
                espera[i] += execTime;
            }
        }
    }
}

