/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.server.dao;

import theknife.common.Response;
import theknife.common.Ristorante;
import theknife.server.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO per la tabella {@code Preferiti}: la lista dei ristoranti
 * preferiti di un cliente. E' il DAO piu' semplice: la tabella e' solo
 * una coppia (id_utente, id_ristorante).
 *
 * @author Daniele Montefiore
 */
public class PreferitoDAO {

    /**
     * Inserisce nella tabella Preferiti la coppia (utente, ristorante),
     * ovvero aggiunge il ristorante alla lista dei preferiti del cliente.
     * Se la coppia esiste gia' il database la ignora ({@code ON CONFLICT
     * DO NOTHING}) e il metodo risponde comunque successo: il risultato
     * che l'utente voleva — "il ristorante e' tra i miei preferiti" —
     * e' vero in ogni caso, quindi un errore sarebbe fuorviante.
     *
     * @param idUtente     identificativo del cliente (dalla sessione server)
     * @param idRistorante identificativo del ristorante da aggiungere
     * @return risposta di successo
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response aggiungiPreferito(int idUtente, int idRistorante) throws SQLException {
        String sql = "INSERT INTO Preferiti (id_utente, id_ristorante) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtente);
            ps.setInt(2, idRistorante);
            ps.executeUpdate();
            return Response.ok(null);
        }
    }

    /**
     * Rimuove un ristorante dalla lista dei preferiti del cliente.
     *
     * @param idUtente     identificativo del cliente (dalla sessione server)
     * @param idRistorante identificativo del ristorante da rimuovere
     * @return risposta di successo, o errore se il ristorante non era tra i preferiti
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response rimuoviPreferito(int idUtente, int idRistorante) throws SQLException {
        String sql = "DELETE FROM Preferiti WHERE id_utente = ? AND id_ristorante = ?";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtente);
            ps.setInt(2, idRistorante);
            if (ps.executeUpdate() == 0) {
                return Response.errore("Il ristorante non e' tra i preferiti");
            }
            return Response.ok(null);
        }
    }

    /**
     * I ristoranti preferiti del cliente, con media stelle e numero di
     * recensioni calcolati come nella ricerca (infatti riuso il metodo
     * di lettura di RistoranteDAO).
     *
     * @param idUtente identificativo del cliente (dalla sessione server)
     * @return risposta con la lista dei {@link Ristorante} preferiti
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response visualizzaPreferiti(int idUtente) throws SQLException {
        String sql = "SELECT r.*, COALESCE(AVG(rec.stelle), 0) AS media_stelle,"
                + " COUNT(rec.id) AS numero_recensioni"
                + " FROM Preferiti p"
                + " JOIN RistorantiTheKnife r ON r.id = p.id_ristorante"
                + " LEFT JOIN Recensioni rec ON rec.id_ristorante = r.id"
                + " WHERE p.id_utente = ?"
                + " GROUP BY r.id"
                + " ORDER BY r.nome";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUtente);
            try (ResultSet rs = ps.executeQuery()) {
                List<Ristorante> preferiti = new ArrayList<>();
                while (rs.next()) {
                    preferiti.add(RistoranteDAO.daResultSetConStatistiche(rs));
                }
                return Response.ok(preferiti);
            }
        }
    }
}
