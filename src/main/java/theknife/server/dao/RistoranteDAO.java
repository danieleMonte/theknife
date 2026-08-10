/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.server.dao;

import theknife.common.Request;
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
 * DAO per la tabella {@code RistorantiTheKnife}. E' il piu' corposo del
 * progetto perche' qui vive tutta la ricerca per distanza: baricentro
 * della citta', formula di Haversine in SQL e filtri combinabili.
 * Media stelle e numero recensioni li calcolo direttamente nella query
 * invece di tenerli come colonne, cosi' non possono mai essere
 * disallineati rispetto alle recensioni vere.
 *
 * @author Daniele Montefiore
 */
public class RistoranteDAO {

    /** Distanza massima (km) quando l'utente non ne sceglie una. */
    public static final double DISTANZA_MAX_PREDEFINITA = 30;

    /**
     * Quanto possono distare (km) le coordinate di un nuovo ristorante
     * dal baricentro della sua citta'. Oltre questa soglia le considero
     * un refuso e rifiuto l'inserimento: una coordinata sbagliata sposta
     * il baricentro e rompe la ricerca per tutta la citta' (successo
     * davvero durante i test, con un ristorante di Legnano finito nel
     * Caucaso per una longitudine 45 al posto di 8.9).
     */
    public static final double SCARTO_MAX_KM = 60;

    /** Distanza Haversine in km, scritta in SQL, dal punto nei tre '?'. */
    private static final String SQL_DISTANZA =
            "2 * 6371 * ASIN(SQRT("
            + " POWER(SIN(RADIANS(r.latitudine - ?) / 2), 2)"
            + " + COS(RADIANS(?)) * COS(RADIANS(r.latitudine))"
            + " * POWER(SIN(RADIANS(r.longitudine - ?) / 2), 2)"
            + " ))";

    /**
     * La ricerca dei ristoranti, cuore del progetto.
     * <p>
     * La citta' e' obbligatoria e fa da punto di partenza: come
     * riferimento uso il baricentro (media di latitudine e longitudine)
     * dei ristoranti che gia' conosco in quella citta' — il dataset
     * stesso fa da "gazetteer", non serve un servizio esterno di
     * geocoding. Da li' tengo i ristoranti la cui distanza Haversine,
     * calcolata in SQL, non supera il raggio scelto dall'utente
     * ({@code distanzaMax} km, di default {@value #DISTANZA_MAX_PREDEFINITA}),
     * ordinati per default dal piu' vicino (si puo' scegliere anche cucina,
     * prezzo o valutazione, vedi {@code ordinamento}). Non c'e' un raggio minimo: il punto
     * di partenza e' gia' la citta' indicata, quindi il minimo e' sempre 0.
     * La stessa query serve sia la schermata dei "ristoranti vicini"
     * sia la ricerca con i filtri.
     * <p>
     * Parametri riconosciuti: {@code citta} (String, obbligatorio),
     * {@code distanzaMax} (Double, km),
     * {@code tipoCucina} (String), {@code prezzoMin}/{@code prezzoMax} (Double),
     * {@code delivery}/{@code prenotazione} (Boolean), {@code stelleMin} (Double),
     * {@code ordinamento} (String: {@code "cucina"}, {@code "prezzoAsc"},
     * {@code "prezzoDesc"}, {@code "valutazione"}, o assente per la distanza).
     *
     * @param richiesta richiesta con i criteri di ricerca
     * @return risposta con la lista dei {@link Ristorante} trovati
     *         (con campo distanzaKm valorizzato), o errore se la citta'
     *         non corrisponde a nessun ristorante del database
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response cercaRistorante(Request richiesta) throws SQLException {
        String citta = (String) richiesta.get("citta");
        if (citta == null || citta.isBlank()) {
            return Response.errore("La locazione geografica e' obbligatoria");
        }
        Double distanzaMax = (Double) richiesta.get("distanzaMax");
        double kmMax = distanzaMax != null ? distanzaMax : DISTANZA_MAX_PREDEFINITA;
        if (kmMax < 0) {
            return Response.errore("La distanza massima non puo' essere negativa");
        }

        try (Connection conn = DBConnection.getIstanza().nuovaConnessione()) {

            // 1) da dove parto: baricentro dei ristoranti della citta'
            double[] riferimento = baricentroCitta(conn, citta);
            if (riferimento == null) {
                return Response.errore("Nessun ristorante conosciuto vicino a \"" + citta
                        + "\": scegli una citta' dall'elenco");
            }
            double latRif = riferimento[0];
            double lonRif = riferimento[1];

            // 2) costruisco la query un pezzo alla volta: ogni filtro presente
            //    aggiunge il suo AND e il suo parametro (sempre con '?', mai
            //    concatenando i valori: e' la difesa dalla SQL injection)
            StringBuilder sql = new StringBuilder(
                    "SELECT r.*, COALESCE(AVG(rec.stelle), 0) AS media_stelle,"
                    + " COUNT(rec.id) AS numero_recensioni,"
                    + " " + SQL_DISTANZA + " AS distanza_km"
                    + " FROM RistorantiTheKnife r"
                    + " LEFT JOIN Recensioni rec ON rec.id_ristorante = r.id"
                    + " WHERE TRUE");
            List<Object> parametri = new ArrayList<>();
            parametri.add(latRif);
            parametri.add(latRif);
            parametri.add(lonRif);

            String tipoCucina = (String) richiesta.get("tipoCucina");
            if (tipoCucina != null && !tipoCucina.isBlank()) {
                sql.append(" AND LOWER(r.tipo_cucina) LIKE LOWER(?)");
                parametri.add("%" + tipoCucina.trim() + "%");
            }
            Double prezzoMin = (Double) richiesta.get("prezzoMin");
            if (prezzoMin != null) {
                sql.append(" AND r.prezzo_medio >= ?");
                parametri.add(prezzoMin);
            }
            Double prezzoMax = (Double) richiesta.get("prezzoMax");
            if (prezzoMax != null) {
                sql.append(" AND r.prezzo_medio <= ?");
                parametri.add(prezzoMax);
            }
            Boolean delivery = (Boolean) richiesta.get("delivery");
            if (delivery != null) {
                sql.append(" AND r.delivery = ?");
                parametri.add(delivery);
            }
            Boolean prenotazione = (Boolean) richiesta.get("prenotazione");
            if (prenotazione != null) {
                sql.append(" AND r.prenotazione_online = ?");
                parametri.add(prenotazione);
            }

            // il filtro sulla distanza sta nell'HAVING (e la formula va ripetuta:
            // in SQL non posso riusare l'alias distanza_km del SELECT). Solo un
            // tetto massimo: il minimo e' sempre 0, cioe' la citta' stessa
            sql.append(" GROUP BY r.id HAVING ").append(SQL_DISTANZA).append(" <= ?");
            parametri.add(latRif);
            parametri.add(latRif);
            parametri.add(lonRif);
            parametri.add(kmMax);

            Double stelleMin = (Double) richiesta.get("stelleMin");
            if (stelleMin != null) {
                sql.append(" AND COALESCE(AVG(rec.stelle), 0) >= ?");
                parametri.add(stelleMin);
            }
            // l'ordinamento non si puo' legare con '?' (un PreparedStatement puo'
            // parametrizzare solo valori, non nomi di colonna): scelgo tra un
            // insieme fisso di ORDER BY scritti a mano, mai dal testo del client
            sql.append(" ORDER BY ").append(ordinePer((String) richiesta.get("ordinamento")));

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < parametri.size(); i++) {
                    ps.setObject(i + 1, parametri.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<Ristorante> risultati = new ArrayList<>();
                    while (rs.next()) {
                        Ristorante r = daResultSetConStatistiche(rs);
                        r.setDistanzaKm(rs.getDouble("distanza_km"));
                        risultati.add(r);
                    }
                    return Response.ok(risultati);
                }
            }
        }
    }

    /**
     * Traduce il criterio scelto dal client nella clausola ORDER BY.
     * A parita' di cucina, prezzo o valutazione, il piu' vicino resta
     * comunque primo: distanza_km e' sempre l'ultimo criterio.
     *
     * @param ordinamento {@code "cucina"}, {@code "prezzoAsc"}, {@code "prezzoDesc"},
     *                    {@code "valutazione"}, o altro/{@code null} per il predefinito
     * @return frammento SQL da usare dopo ORDER BY (mai testo del client: solo
     *         una di queste costanti scritte a mano, per evitare SQL injection)
     */
    private static String ordinePer(String ordinamento) {
        if (ordinamento == null) {
            return "distanza_km";
        }
        switch (ordinamento) {
            case "cucina":
                return "r.tipo_cucina, distanza_km";
            case "prezzoAsc":
                return "r.prezzo_medio, distanza_km";
            case "prezzoDesc":
                return "r.prezzo_medio DESC, distanza_km";
            case "valutazione":
                // media_stelle e' COALESCE(AVG(rec.stelle), 0): chi non ha
                // nessuna recensione vale sempre 0, quindi un DESC semplice
                // lo mette gia' in fondo da solo, dopo tutti i voti veri
                // (1-5) - non serve distinguere i due casi a parte. Se
                // nessun risultato ha recensioni sono tutti a 0, e quindi
                // l'ordine finale e' semplicemente per distanza
                return "media_stelle DESC, distanza_km";
            default:
                return "distanza_km";
        }
    }

    /**
     * Elenco alfabetico delle citta' presenti nel database: serve al
     * client per il menu a tendina, cosi' l'utente sceglie una citta'
     * che esiste davvero invece di scriverla a mano (e sbagliarla).
     *
     * @return risposta con la lista dei nomi delle citta'
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response elencoCitta() throws SQLException {
        String sql = "SELECT DISTINCT citta FROM RistorantiTheKnife ORDER BY citta";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> citta = new ArrayList<>();
            while (rs.next()) {
                citta.add(rs.getString("citta"));
            }
            return Response.ok(citta);
        }
    }

    /**
     * Restituisce il dettaglio di un singolo ristorante, con media stelle
     * e numero di recensioni aggiornati.
     *
     * @param idRistorante identificativo del ristorante
     * @return risposta con il {@link Ristorante}, o errore se non esiste
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response visualizzaRistorante(int idRistorante) throws SQLException {
        String sql = "SELECT r.*, COALESCE(AVG(rec.stelle), 0) AS media_stelle,"
                + " COUNT(rec.id) AS numero_recensioni"
                + " FROM RistorantiTheKnife r"
                + " LEFT JOIN Recensioni rec ON rec.id_ristorante = r.id"
                + " WHERE r.id = ?"
                + " GROUP BY r.id";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idRistorante);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Response.ok(daResultSetConStatistiche(rs));
                }
                return Response.errore("Ristorante non trovato");
            }
        }
    }

    /**
     * Inserisce un nuovo ristorante del gestore loggato.
     * <p>
     * Sulle coordinate ho messo tre difese, dopo aver visto coi miei
     * occhi cosa combina una longitudine sbagliata: (1) si possono
     * lasciare vuote, e se la citta' e' gia' nel database uso il suo
     * baricentro; (2) se indicate, devono stare nei range geografici
     * veri; (3) se la citta' e' conosciuta, non possono distare piu' di
     * {@value #SCARTO_MAX_KM} km dagli altri ristoranti — a quel punto
     * sono quasi sicuramente un refuso.
     *
     * @param idGestore identificativo del gestore (dalla sessione server)
     * @param richiesta richiesta con i dati del ristorante: {@code nome},
     *                  {@code nazione}, {@code citta}, {@code indirizzo},
     *                  {@code latitudine}, {@code longitudine} (facoltative),
     *                  {@code prezzoMedio}, {@code delivery},
     *                  {@code prenotazione}, {@code tipoCucina}
     * @return risposta di successo, o errore se i dati non sono validi
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response aggiungiRistorante(int idGestore, Request richiesta) throws SQLException {
        String nome = (String) richiesta.get("nome");
        String citta = (String) richiesta.get("citta");
        if (nome == null || nome.isBlank() || citta == null || citta.isBlank()) {
            return Response.errore("Nome e citta' del ristorante sono obbligatori");
        }
        Double prezzoMedio = (Double) richiesta.get("prezzoMedio");
        if (prezzoMedio == null || prezzoMedio < 0) {
            return Response.errore("Il prezzo medio deve essere un numero non negativo");
        }
        Double latitudine = (Double) richiesta.get("latitudine");
        Double longitudine = (Double) richiesta.get("longitudine");

        try (Connection conn = DBConnection.getIstanza().nuovaConnessione()) {
            double[] riferimento = baricentroCitta(conn, citta);

            if (latitudine == null || longitudine == null) {
                // coordinate lasciate vuote: se conosco la citta' uso il suo
                // baricentro, cosi' il gestore non deve cercarle su una mappa
                // (e soprattutto non puo' sbagliarle)
                if (riferimento == null) {
                    return Response.errore("\"" + citta + "\" non e' ancora su TheKnife: "
                            + "per una nuova citta' indica latitudine e longitudine");
                }
                latitudine = riferimento[0];
                longitudine = riferimento[1];
            } else {
                if (latitudine < -90 || latitudine > 90
                        || longitudine < -180 || longitudine > 180) {
                    return Response.errore("Coordinate non valide: latitudine tra -90 e 90, "
                            + "longitudine tra -180 e 180 (es. Legnano: 45.60, 8.92)");
                }
                // controllo di coerenza: se le coordinate cascano lontanissimo
                // dagli altri ristoranti della citta', quasi sicuramente sono
                // un refuso, e un refuso qui rompe la ricerca di tutta la citta'
                if (riferimento != null) {
                    double scarto = distanzaKm(latitudine, longitudine,
                            riferimento[0], riferimento[1]);
                    if (scarto > SCARTO_MAX_KM) {
                        return Response.errore(String.format(
                                "Le coordinate distano %.0f km dagli altri ristoranti di %s: "
                                + "controlla latitudine e longitudine, oppure lasciale vuote "
                                + "per usare il centro della citta'", scarto, citta));
                    }
                }
            }

            String sql = "INSERT INTO RistorantiTheKnife"
                    + " (nome, nazione, citta, indirizzo, latitudine, longitudine,"
                    + " prezzo_medio, delivery, prenotazione_online, tipo_cucina, id_gestore)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nome);
                ps.setString(2, (String) richiesta.get("nazione"));
                ps.setString(3, citta);
                ps.setString(4, (String) richiesta.get("indirizzo"));
                ps.setDouble(5, latitudine);
                ps.setDouble(6, longitudine);
                ps.setDouble(7, prezzoMedio);
                Boolean delivery = (Boolean) richiesta.get("delivery");
                Boolean prenotazione = (Boolean) richiesta.get("prenotazione");
                ps.setBoolean(8, delivery != null && delivery);
                ps.setBoolean(9, prenotazione != null && prenotazione);
                ps.setString(10, (String) richiesta.get("tipoCucina"));
                ps.setInt(11, idGestore);
                ps.executeUpdate();
                return Response.ok(null);
            }
        }
    }

    /**
     * Il "centro" di una citta' secondo il database: la media di
     * latitudine e longitudine dei suoi ristoranti. Lo usano sia la
     * ricerca sia i controlli sull'inserimento.
     *
     * @param conn  connessione gia' aperta da riutilizzare
     * @param citta nome della citta'
     * @return array {latitudine, longitudine}, o {@code null} se in
     *         quella citta' non c'e' ancora nessun ristorante
     * @throws SQLException in caso di errore di accesso al database
     */
    private double[] baricentroCitta(Connection conn, String citta) throws SQLException {
        String sql = "SELECT AVG(latitudine) AS lat, AVG(longitudine) AS lon "
                + "FROM RistorantiTheKnife "
                + "WHERE LOWER(citta) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, citta.trim());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                double lat = rs.getDouble("lat");
                // AVG su zero righe da' NULL, ma getDouble lo trasforma in 0.0:
                // per accorgermene devo chiedere a wasNull()
                if (rs.wasNull()) {
                    return null;
                }
                return new double[]{lat, rs.getDouble("lon")};
            }
        }
    }

    /**
     * Distanza in km tra due punti sulla Terra (formula di Haversine).
     * E' la copia in Java della formula che la ricerca usa in SQL:
     * devono restare identiche, altrimenti i controlli non tornano.
     *
     * @param lat1 latitudine del primo punto
     * @param lon1 longitudine del primo punto
     * @param lat2 latitudine del secondo punto
     * @param lon2 longitudine del secondo punto
     * @return distanza in chilometri
     */
    private static double distanzaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * 6371 * Math.asin(Math.sqrt(h));
    }

    /**
     * Riepilogo per il gestore: i suoi ristoranti con media stelle e
     * numero di recensioni di ciascuno.
     *
     * @param idGestore identificativo del gestore (dalla sessione server)
     * @return risposta con la lista dei {@link Ristorante} del gestore
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response visualizzaRiepilogo(int idGestore) throws SQLException {
        String sql = "SELECT r.*, COALESCE(AVG(rec.stelle), 0) AS media_stelle,"
                + " COUNT(rec.id) AS numero_recensioni"
                + " FROM RistorantiTheKnife r"
                + " LEFT JOIN Recensioni rec ON rec.id_ristorante = r.id"
                + " WHERE r.id_gestore = ?"
                + " GROUP BY r.id"
                + " ORDER BY r.nome";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idGestore);
            try (ResultSet rs = ps.executeQuery()) {
                List<Ristorante> ristoranti = new ArrayList<>();
                while (rs.next()) {
                    ristoranti.add(daResultSetConStatistiche(rs));
                }
                return Response.ok(ristoranti);
            }
        }
    }

    /**
     * Trasforma la riga corrente del ResultSet in un Ristorante; la riga
     * deve avere anche {@code media_stelle} e {@code numero_recensioni}.
     * Non e' private perche' la riusa anche {@link PreferitoDAO}, che
     * produce righe nello stesso formato.
     *
     * @param rs ResultSet posizionato su una riga del risultato
     * @return ristorante coi dati della riga
     * @throws SQLException in caso di errore di lettura
     */
    static Ristorante daResultSetConStatistiche(ResultSet rs) throws SQLException {
        Ristorante r = new Ristorante();
        r.setId(rs.getInt("id"));
        r.setNome(rs.getString("nome"));
        r.setNazione(rs.getString("nazione"));
        r.setCitta(rs.getString("citta"));
        r.setIndirizzo(rs.getString("indirizzo"));
        r.setLatitudine(rs.getDouble("latitudine"));
        r.setLongitudine(rs.getDouble("longitudine"));
        r.setPrezzoMedio(rs.getDouble("prezzo_medio"));
        r.setDelivery(rs.getBoolean("delivery"));
        r.setPrenotazioneOnline(rs.getBoolean("prenotazione_online"));
        r.setTipoCucina(rs.getString("tipo_cucina"));
        r.setIdGestore(rs.getInt("id_gestore"));
        r.setMediaStelle(rs.getDouble("media_stelle"));
        r.setNumeroRecensioni(rs.getInt("numero_recensioni"));
        return r;
    }
}
