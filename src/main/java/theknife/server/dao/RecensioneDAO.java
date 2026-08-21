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
 * Data Access Object per la tabella {@code Recensioni}: gestisce l'intero
 * ciclo di vita di una recensione (inserimento, modifica, cancellazione,
 * consultazione) e la risposta del gestore.
 * <p>
 * Le specifiche richiedono che le recensioni siano visualizzate in forma
 * anonima: l'identificativo dell'autore non viene pertanto copiato negli
 * oggetti trasmessi al client, in modo che il dato non lasci mai il
 * server.
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
     * Inserisce una nuova recensione. Il vincolo di una sola recensione
     * per utente e per ristorante non e' verificato a livello applicativo
     * ma garantito dal vincolo UNIQUE (id_utente, id_ristorante) definito
     * sulla tabella, valido anche in presenza di inserimenti concorrenti.
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
     * Modifica una recensione esistente. La condizione
     * {@code id_utente = ?} nella clausola WHERE assolve contestualmente
     * a due funzioni: individua la recensione e ne verifica la
     * proprieta'. Se la recensione non appartiene all'utente richiedente,
     * l'UPDATE non modifica alcuna riga.
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
     * Elimina una recensione. Analogamente alla modifica, la presenza di
     * {@code id_utente} nella clausola WHERE impedisce l'eliminazione di
     * recensioni altrui.
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
     * Registra la risposta del gestore a una recensione relativa a un
     * proprio ristorante. La condizione {@code risposta IS NULL} nella
     * clausola WHERE consente l'aggiornamento solo in assenza di una
     * risposta precedente; poiche' l'istruzione UPDATE e' atomica, in
     * caso di tentativi concorrenti uno solo va a buon fine. Il vincolo
     * di una sola risposta per recensione e' pertanto garantito senza
     * ricorrere a meccanismi di lock applicativi.
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
     * Costruisce un oggetto {@link Recensione} a partire dalla riga
     * corrente del ResultSet, centralizzando la conversione anziche'
     * replicarla in ciascun metodo di lettura.
     * <p>
     * L'identificativo dell'autore non viene deliberatamente copiato: e'
     * questo il meccanismo con cui le recensioni raggiungono il client in
     * forma anonima, come richiesto dalle specifiche.
     *
     * @param rs ResultSet posizionato su una riga della tabella Recensioni
     * @return recensione popolata con i dati della riga
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
