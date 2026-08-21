/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestisce i parametri di connessione al database, acquisiti all'avvio
 * del server come previsto dalle specifiche, e fornisce ai DAO le
 * connessioni JDBC. Implementa il pattern Singleton: i parametri sono
 * configurati una sola volta e successivamente condivisi da tutti i
 * componenti che accedono alla base di dati.
 *
 * @author Daniele Montefiore
 */
public final class DBConnection {

    private static DBConnection istanza;

    private final String url;
    private final String utente;
    private final String password;

    private DBConnection(String host, String nomeDb, String utente, String password) {
        this.url = "jdbc:postgresql://" + host + "/" + nomeDb;
        this.utente = utente;
        this.password = password;
    }

    /**
     * Memorizza i parametri di connessione ed esegue immediatamente una
     * connessione di verifica, in modo da rilevare eventuali credenziali
     * errate gia' all'avvio anziche' alla prima richiesta di un client.
     *
     * @param host     host del DBMS (es. {@code localhost:5432})
     * @param nomeDb   nome del database (es. {@code dbtk})
     * @param utente   nome utente PostgreSQL
     * @param password password PostgreSQL
     * @throws SQLException se la connessione di prova fallisce
     */
    public static synchronized void inizializza(String host, String nomeDb,
                                                String utente, String password) throws SQLException {
        istanza = new DBConnection(host, nomeDb, utente, password);
        // Connessione di verifica: solleva immediatamente un'eccezione se i
        // parametri forniti non sono validi.
        istanza.nuovaConnessione().close();
    }

    /**
     * Restituisce l'istanza del singleton.
     *
     * @return istanza inizializzata
     * @throws IllegalStateException se {@link #inizializza} non e' stato chiamato
     */
    public static synchronized DBConnection getIstanza() {
        if (istanza == null) {
            throw new IllegalStateException("DBConnection non inizializzata");
        }
        return istanza;
    }

    /**
     * Apre una nuova connessione al database a ogni invocazione. Le
     * connessioni non sono condivise tra i thread: ciascuno ne apre una
     * propria, la utilizza e la chiude, evitando cosi' problemi di
     * concorrenza a livello JDBC.
     *
     * @return nuova connessione JDBC (da chiudere con try-with-resources)
     * @throws SQLException in caso di errore di connessione
     */
    public Connection nuovaConnessione() throws SQLException {
        return DriverManager.getConnection(url, utente, password);
    }
}
