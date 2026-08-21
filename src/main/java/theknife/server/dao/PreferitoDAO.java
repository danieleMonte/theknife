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
 * Data Access Object per la tabella {@code Preferiti}: gestisce la lista
 * dei ristoranti preferiti di un cliente. La tabella e' costituita dalla
 * sola coppia (id_utente, id_ristorante), che ne rappresenta anche la
 * chiave primaria.
 *
 * @author Daniele Montefiore
 */
public class PreferitoDAO {

    /**
     * Inserisce nella tabella Preferiti la coppia (utente, ristorante),
     * aggiungendo il ristorante alla lista dei preferiti del cliente.
     * Un eventuale duplicato viene ignorato dal database tramite
     * {@code ON CONFLICT DO NOTHING} e l'operazione restituisce comunque
     * esito positivo: la condizione attesa dall'utente, ossia la presenza
     * del ristorante tra i preferiti, risulta soddisfatta in entrambi i
     * casi, per cui la segnalazione di un errore sarebbe fuorviante.
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
     * Restituisce i ristoranti preferiti del cliente, con media delle
     * stelle e numero di recensioni calcolati come nella ricerca; la
     * conversione delle righe e' delegata al metodo di lettura di
     * {@link RistoranteDAO}, che produce risultati nello stesso formato.
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
