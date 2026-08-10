/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.server;

import java.io.Console;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Il main del server. All'avvio chiede da terminale host e credenziali
 * del database (come richiesto dalle specifiche), poi si mette in
 * ascolto sulla porta 4444: per ogni client che si connette prende un
 * thread dal pool e gli affida un {@link ClientHandler}, cosi' piu'
 * utenti possono lavorare in parallelo.
 *
 * @author Daniele Montefiore
 */
public final class ServerTK {

    /** Porta TCP su cui il server accetta le connessioni dei client. */
    public static final int PORTA = 4444;

    private ServerTK() { }

    /**
     * Punto d'ingresso del server: chiede i parametri del DB,
     * verifica la connessione e avvia il ciclo di accettazione dei client.
     *
     * @param args non utilizzati
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== TheKnife - serverTK ===");

        System.out.print("Host del DB [localhost:5432]: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "localhost:5432";

        System.out.print("Nome del database [dbtk]: ");
        String nomeDb = scanner.nextLine().trim();
        if (nomeDb.isEmpty()) nomeDb = "dbtk";

        System.out.print("Utente DB: ");
        String utente = scanner.nextLine().trim();

        // Console.readPassword non fa vedere la password mentre la digiti,
        // ma funziona solo in un terminale vero: dall'IDE (o con input
        // rediretto) restituisce null, e allora leggo normalmente da stdin.
        Console console = System.console();
        String password = null;
        if (console != null) {
            char[] caratteri = console.readPassword("Password DB: ");
            if (caratteri != null) {
                password = new String(caratteri);
            }
        }
        if (password == null) {
            System.out.print("Password DB: ");
            password = scanner.hasNextLine() ? scanner.nextLine() : "";
        }
        // chiude anche System.in: da qui in poi il server non legge piu' da tastiera
        scanner.close();

        try {
            DBConnection.inizializza(host, nomeDb, utente, password);
            System.out.println("Connessione al database riuscita.");
        } catch (SQLException e) {
            System.err.println("Impossibile connettersi al database: " + e.getMessage());
            return;
        }

        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Server in ascolto sulla porta " + PORTA + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuovo client connesso: " + clientSocket.getInetAddress());
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Errore del server: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}
