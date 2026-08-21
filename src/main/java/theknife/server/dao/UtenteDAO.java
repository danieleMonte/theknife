/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.server.dao;

import org.mindrot.jbcrypt.BCrypt;
import theknife.common.Request;
import theknife.common.Response;
import theknife.common.Ruolo;
import theknife.common.Utente;
import theknife.server.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Data Access Object per la tabella {@code Utenti}: implementa le
 * operazioni di registrazione e autenticazione. La classe centralizza
 * inoltre l'intera gestione delle password (calcolo dell'hash BCrypt,
 * verifica delle credenziali ed esclusione dalle risposte inviate al
 * client).
 *
 * @author Daniele Montefiore
 */
public class UtenteDAO {

    /**
     * Verifica le credenziali fornite confrontando la password ricevuta
     * con l'hash memorizzato nel database mediante
     * {@code BCrypt.checkpw}. La password in chiaro non viene in alcun
     * caso memorizzata.
     *
     * @param username indirizzo e-mail dell'utente, che ne costituisce
     *                 l'identificativo di accesso
     * @param password password in chiaro fornita dal client
     * @return risposta con l'oggetto {@link Utente} (senza hash) se
     *         l'autenticazione riesce, errore altrimenti
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM Utenti WHERE username = ?";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && BCrypt.checkpw(password, rs.getString("password"))) {
                    Utente utente = daResultSet(rs);
                    utente.setPassword(null); // neppure l'hash viene trasmesso al client
                    return Response.ok(utente);
                }
                return Response.errore("E-mail o password errati");
            }
        }
    }

    /**
     * Registra un nuovo utente, cifrando la password con BCrypt prima
     * dell'inserimento. L'identificativo di accesso e' l'indirizzo e-mail,
     * di cui viene verificato il formato; l'unicita' non e' invece
     * controllata con una SELECT preliminare ma delegata al vincolo UNIQUE
     * definito nello schema: la soluzione resta corretta anche in presenza
     * di registrazioni concorrenti, non presentando la finestra temporale
     * tipica del controllo seguito da inserimento.
     *
     * @param richiesta richiesta contenente i dati dell'utente
     *                  (nome, cognome, username, password, dataNascita,
     *                  domicilio, ruolo)
     * @return risposta di successo, o errore se l'indirizzo non e' valido
     *         o risulta gia' registrato
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response registrazione(Request richiesta) throws SQLException {
        String email = (String) richiesta.get("username");
        if (!Utente.emailValida(email)) {
            return Response.errore("Indirizzo e-mail non valido (es. nome@dominio.it)");
        }
        String sql = "INSERT INTO Utenti (nome, cognome, username, password, data_nascita, domicilio, ruolo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (String) richiesta.get("nome"));
            ps.setString(2, (String) richiesta.get("cognome"));
            ps.setString(3, email);
            ps.setString(4, BCrypt.hashpw((String) richiesta.get("password"), BCrypt.gensalt()));
            LocalDate dataNascita = (LocalDate) richiesta.get("dataNascita");
            ps.setDate(5, dataNascita != null ? Date.valueOf(dataNascita) : null);
            ps.setString(6, (String) richiesta.get("domicilio"));
            ps.setString(7, ((Ruolo) richiesta.get("ruolo")).name().toLowerCase());
            ps.executeUpdate();
            return Response.ok(null);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) { // 23505 = violazione di UNIQUE
                return Response.errore("Indirizzo e-mail gia' registrato");
            }
            throw e;
        }
    }

    /**
     * Costruisce un oggetto {@link Utente} a partire dalla riga corrente
     * del ResultSet.
     *
     * @param rs ResultSet posizionato su una riga della tabella Utenti
     * @return utente popolato con i dati della riga
     * @throws SQLException in caso di errore di lettura
     */
    private Utente daResultSet(ResultSet rs) throws SQLException {
        Date dataNascita = rs.getDate("data_nascita");
        return new Utente(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("cognome"),
                rs.getString("username"),
                rs.getString("password"),
                dataNascita != null ? dataNascita.toLocalDate() : null,
                rs.getString("domicilio"),
                Ruolo.valueOf(rs.getString("ruolo").toUpperCase()));
    }
}
