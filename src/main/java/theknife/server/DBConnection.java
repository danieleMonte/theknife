/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Tiene i parametri di connessione al database (chiesti all'avvio del
 * server, come vogliono le specifiche) e apre le connessioni JDBC per
 * i DAO. E' un singleton: i parametri si impostano una volta sola e
 * poi tutti li leggono da qui.
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
     * Salva i parametri e prova subito una connessione: meglio scoprire
     * le credenziali sbagliate all'avvio che alla prima richiesta di un client.
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
        // connessione di prova: fallisce subito se i parametri sono errati
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
     * Apre una connessione nuova ogni volta. Ho scelto di non condividere
     * un'unica connessione tra i thread: ognuno apre la sua, la usa e la
     * chiude, cosi' non ci sono problemi di concorrenza lato JDBC.
     *
     * @return nuova connessione JDBC (da chiudere con try-with-resources)
     * @throws SQLException in caso di errore di connessione
     */
    public Connection nuovaConnessione() throws SQLException {
        return DriverManager.getConnection(url, utente, password);
    }
}
