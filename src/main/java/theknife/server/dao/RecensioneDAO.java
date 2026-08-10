/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.server.dao;

import theknife.common.Recensione;
import theknife.common.Request;
import theknife.common.Response;
import theknife.server.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per la tabella {@code Recensioni}: tutto il ciclo di vita di una
 * recensione (inserimento, modifica, cancellazione, elenchi) piu' la
 * risposta del gestore.
 * <p>
 * Le specifiche vogliono le recensioni anonime, quindi l'id dell'autore
 * non viene proprio copiato negli oggetti spediti al client: quello che
 * non parte dal server non puo' finire in giro.
 *
 * @author Daniele Montefiore
 */
public class RecensioneDAO {

    /**
     * Elenca le recensioni di un ristorante.
     *
     * @param idRistorante identificativo del ristorante
     * @return risposta con la lista delle {@link Recensione}
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response visualizzaRecensioni(int idRistorante) throws SQLException {
        String sql = "SELECT * FROM Recensioni WHERE id_ristorante = ? ORDER BY id DESC";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRistorante);
            try (ResultSet rs = ps.executeQuery()) {
                List<Recensione> recensioni = new ArrayList<>();
                while (rs.next()) {
                    recensioni.add(daResultSet(rs));
                }
                return Response.ok(recensioni);
            }
        }
    }

    /**
     * Elenca le recensioni scritte da un utente, con il nome del
     * ristorante recensito (per la schermata "le mie recensioni").
     *
     * @param idUtente identificativo dell'autore
     * @return risposta con la lista delle {@link Recensione}
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response mieRecensioni(int idUtente) throws SQLException {
        String sql = "SELECT rec.*, r.nome AS nome_ristorante"
                + " FROM Recensioni rec"
                + " JOIN RistorantiTheKnife r ON r.id = rec.id_ristorante"
                + " WHERE rec.id_utente = ?"
                + " ORDER BY rec.id DESC";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtente);
            try (ResultSet rs = ps.executeQuery()) {
                List<Recensione> recensioni = new ArrayList<>();
                while (rs.next()) {
                    Recensione rec = daResultSet(rs);
                    rec.setNomeRistorante(rs.getString("nome_ristorante"));
                    recensioni.add(rec);
                }
                return Response.ok(recensioni);
            }
        }
    }

    /**
     * Inserisce una recensione. La regola "una sola recensione per
     * utente per ristorante" non la controllo io: la fa rispettare il
     * vincolo UNIQUE (id_utente, id_ristorante) della tabella, che
     * tiene anche nel caso di due inserimenti in contemporanea.
     *
     * @param idUtente  identificativo dell'autore (dalla sessione server)
     * @param richiesta richiesta con {@code idRistorante}, {@code stelle} e {@code testo}
     * @return risposta di successo, o errore se i dati non sono validi
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response aggiungiRecensione(int idUtente, Request richiesta) throws SQLException {
        Integer stelle = (Integer) richiesta.get("stelle");
        String testo = (String) richiesta.get("testo");
        if (stelle == null || stelle < 1 || stelle > 5) {
            return Response.errore("Il numero di stelle deve essere compreso tra 1 e 5");
        }
        if (testo == null || testo.isBlank()) {
            return Response.errore("Il testo della recensione e' obbligatorio");
        }
        String sql = "INSERT INTO Recensioni (id_utente, id_ristorante, stelle, testo) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtente);
            ps.setInt(2, (Integer) richiesta.get("idRistorante"));
            ps.setInt(3, stelle);
            ps.setString(4, testo);
            ps.executeUpdate();
            return Response.ok(null);
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) { // 23505 = violazione di UNIQUE
                return Response.errore("Hai gia' recensito questo ristorante: puoi modificare la recensione esistente");
            }
            throw e;
        }
    }

    /**
     * Modifica una recensione. L'{@code id_utente = ?} nel WHERE fa due
     * cose insieme: trova la recensione e verifica che sia davvero di
     * chi la sta modificando — se non e' sua, l'UPDATE tocca zero righe.
     *
     * @param idUtente  identificativo dell'autore (dalla sessione server)
     * @param richiesta richiesta con {@code idRecensione}, {@code stelle} e {@code testo}
     * @return risposta di successo, o errore se la recensione non esiste o non appartiene all'utente
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response modificaRecensione(int idUtente, Request richiesta) throws SQLException {
        Integer stelle = (Integer) richiesta.get("stelle");
        String testo = (String) richiesta.get("testo");
        if (stelle == null || stelle < 1 || stelle > 5) {
            return Response.errore("Il numero di stelle deve essere compreso tra 1 e 5");
        }
        if (testo == null || testo.isBlank()) {
            return Response.errore("Il testo della recensione e' obbligatorio");
        }
        String sql = "UPDATE Recensioni SET stelle = ?, testo = ? WHERE id = ? AND id_utente = ?";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stelle);
            ps.setString(2, testo);
            ps.setInt(3, (Integer) richiesta.get("idRecensione"));
            ps.setInt(4, idUtente);
            if (ps.executeUpdate() == 0) {
                return Response.errore("Recensione non trovata o non tua");
            }
            return Response.ok(null);
        }
    }

    /**
     * Elimina una recensione; vale lo stesso trucco della modifica,
     * l'{@code id_utente} nel WHERE protegge le recensioni degli altri.
     *
     * @param idUtente     identificativo dell'autore (dalla sessione server)
     * @param idRecensione identificativo della recensione da eliminare
     * @return risposta di successo, o errore se la recensione non esiste o non appartiene all'utente
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response eliminaRecensione(int idUtente, int idRecensione) throws SQLException {
        String sql = "DELETE FROM Recensioni WHERE id = ? AND id_utente = ?";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRecensione);
            ps.setInt(2, idUtente);
            if (ps.executeUpdate() == 0) {
                return Response.errore("Recensione non trovata o non tua");
            }
            return Response.ok(null);
        }
    }

    /**
     * La risposta del gestore a una recensione. Il {@code risposta IS
     * NULL} nel WHERE e' il pezzo importante: l'UPDATE riesce solo se la
     * risposta non c'e' ancora, e siccome un UPDATE e' atomico, anche se
     * due tentativi arrivano nello stesso istante ne passa uno solo —
     * "al massimo una risposta per recensione" senza bisogno di lock.
     *
     * @param idGestore identificativo del gestore (dalla sessione server)
     * @param richiesta richiesta con {@code idRecensione} e {@code risposta}
     * @return risposta di successo, o errore se la recensione non riguarda
     *         un ristorante del gestore o ha gia' una risposta
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response rispostaRecensione(int idGestore, Request richiesta) throws SQLException {
        String testoRisposta = (String) richiesta.get("risposta");
        if (testoRisposta == null || testoRisposta.isBlank()) {
            return Response.errore("Il testo della risposta e' obbligatorio");
        }
        String sql = "UPDATE Recensioni rec SET risposta = ?"
                + " FROM RistorantiTheKnife r"
                + " WHERE rec.id = ? AND rec.risposta IS NULL"
                + " AND r.id = rec.id_ristorante AND r.id_gestore = ?";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, testoRisposta);
            ps.setInt(2, (Integer) richiesta.get("idRecensione"));
            ps.setInt(3, idGestore);
            if (ps.executeUpdate() == 0) {
                return Response.errore("Recensione gia' risposta o non relativa a un tuo ristorante");
            }
            return Response.ok(null);
        }
    }

    /**
     * Trasforma la riga corrente del ResultSet in una Recensione.
     * L'id dell'autore, di proposito, non lo copio: e' cosi' che le
     * recensioni arrivano anonime al client.
     *
     * @param rs ResultSet posizionato su una riga della tabella Recensioni
     * @return recensione coi dati della riga
     * @throws SQLException in caso di errore di lettura
     */
    private Recensione daResultSet(ResultSet rs) throws SQLException {
        Recensione rec = new Recensione();
        rec.setId(rs.getInt("id"));
        rec.setIdRistorante(rs.getInt("id_ristorante"));
        rec.setStelle(rs.getInt("stelle"));
        rec.setTesto(rs.getString("testo"));
        rec.setRisposta(rs.getString("risposta"));
        return rec;
    }
}
