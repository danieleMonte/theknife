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
 * DAO per la tabella {@code Utenti}: qui stanno le query di
 * registrazione e login. Tutto quello che riguarda le password
 * (hash BCrypt, confronto, mai inviarle al client) passa da qui.
 *
 * @author Daniele Montefiore
 */
public class UtenteDAO {

    /**
     * Controlla le credenziali: prendo l'hash salvato nel database e
     * lascio a BCrypt.checkpw il confronto con la password ricevuta
     * (la password in chiaro non viene mai salvata da nessuna parte).
     *
     * @param username username o e-mail
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
                    utente.setPassword(null); // nemmeno l'hash deve arrivare al client
                    return Response.ok(utente);
                }
                return Response.errore("Username o password errati");
            }
        }
    }

    /**
     * Inserisce un nuovo utente; la password viene passata a BCrypt
     * prima dell'INSERT. Il caso "username gia' preso" non lo controllo
     * io con una SELECT: lascio che sia il vincolo UNIQUE del database
     * a bloccarlo, cosi' funziona anche con due registrazioni in
     * contemporanea.
     *
     * @param richiesta richiesta contenente i dati dell'utente
     *                  (nome, cognome, username, password, dataNascita,
     *                  domicilio, ruolo)
     * @return risposta di successo, o errore se lo username e' gia' in uso
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response registrazione(Request richiesta) throws SQLException {
        String sql = "INSERT INTO Utenti (nome, cognome, username, password, data_nascita, domicilio, ruolo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (String) richiesta.get("nome"));
            ps.setString(2, (String) richiesta.get("cognome"));
            ps.setString(3, (String) richiesta.get("username"));
            ps.setString(4, BCrypt.hashpw((String) richiesta.get("password"), BCrypt.gensalt()));
            LocalDate dataNascita = (LocalDate) richiesta.get("dataNascita");
            ps.setDate(5, dataNascita != null ? Date.valueOf(dataNascita) : null);
            ps.setString(6, (String) richiesta.get("domicilio"));
            ps.setString(7, ((Ruolo) richiesta.get("ruolo")).name().toLowerCase());
            ps.executeUpdate();
            return Response.ok(null);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) { // 23505 = violazione di UNIQUE
                return Response.errore("Username gia' in uso");
            }
            throw e;
        }
    }

    /**
     * Trasforma la riga corrente del ResultSet in un oggetto Utente.
     *
     * @param rs ResultSet posizionato su una riga della tabella Utenti
     * @return utente coi dati della riga
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
