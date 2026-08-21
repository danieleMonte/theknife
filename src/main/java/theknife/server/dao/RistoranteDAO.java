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
 * Data Access Object per la tabella {@code RistorantiTheKnife}: ricerca
 * geografica, dettaglio, inserimento e riepilogo dei ristoranti.
 * <p>
 * La ricerca per distanza si basa sul baricentro (media di latitudine e
 * longitudine) dei ristoranti gia' noti in una citta', usato come punto
 * di riferimento per il calcolo della distanza ortodromica (formula di
 * Haversine) direttamente in SQL, combinabile con i filtri opzionali
 * della richiesta.
 * <p>
 * Media delle stelle e numero di recensioni non sono colonne della
 * tabella ma vengono ricalcolati a ogni interrogazione tramite funzioni
 * di aggregazione, garantendo la coerenza con i dati effettivi delle
 * recensioni senza richiedere aggiornamenti espliciti.
 *
 * @author Daniele Montefiore
 */
public class RistoranteDAO {

    /** Distanza massima (km) quando l'utente non ne sceglie una. */
    public static final double DISTANZA_MAX_PREDEFINITA = 30;

    /**
     * Distanza massima (km) ammessa tra le coordinate di un nuovo
     * ristorante e il baricentro della sua citta'. Oltre questa soglia
     * le coordinate sono considerate errate e l'inserimento viene
     * rifiutato: una coordinata errata sposterebbe il baricentro e
     * comprometterebbe la ricerca per l'intera citta'.
     */
    public static final double SCARTO_MAX_KM = 60;

    /** Espressione SQL della distanza di Haversine (km) dal punto identificato dai tre parametri {@code ?}. */
    private static final String SQL_DISTANZA =
            "2 * 6371 * ASIN(SQRT("
            + " POWER(SIN(RADIANS(r.latitudine - ?) / 2), 2)"
            + " + COS(RADIANS(?)) * COS(RADIANS(r.latitudine))"
            + " * POWER(SIN(RADIANS(r.longitudine - ?) / 2), 2)"
            + " ))";

    /**
     * Ricerca dei ristoranti in base a criteri geografici e a filtri opzionali.
     * <p>
     * La citta' e' obbligatoria e costituisce il punto di partenza della
     * ricerca: come riferimento geografico viene utilizzato il baricentro
     * (media di latitudine e longitudine) dei ristoranti gia' presenti in
     * quella citta'. Vengono restituiti i ristoranti la cui distanza dal
     * riferimento, calcolata con la formula di Haversine, non supera il
     * raggio massimo indicato ({@code distanzaMax}, in km; valore
     * predefinito {@value #DISTANZA_MAX_PREDEFINITA}). I risultati sono
     * ordinati per distanza crescente, salvo diversa indicazione tramite
     * il parametro {@code ordinamento}.
     * <p>
     * Lo stesso metodo serve sia la schermata dei ristoranti vicini
     * all'utente sia la ricerca con filtri combinati.
     * <p>
     * Parametri riconosciuti: {@code citta} (String, obbligatorio),
     * {@code distanzaMax} (Double, km),
     * {@code tipoCucina} (String), {@code prezzoMin}/{@code prezzoMax} (Double),
     * {@code delivery}/{@code prenotazione} (Boolean), {@code stelleMin} (Double),
     * {@code ordinamento} (String: {@code "cucina"}, {@code "prezzoAsc"},
     * {@code "prezzoDesc"}, {@code "valutazione"}, o assente per l'ordinamento
     * per distanza).
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

            // 1) punto di riferimento: baricentro dei ristoranti della citta'
            double[] riferimento = baricentroCitta(conn, citta);
            if (riferimento == null) {
                return Response.errore("Nessun ristorante conosciuto vicino a \"" + citta
                        + "\": scegli una citta' dall'elenco");
            }
            double latRif = riferimento[0];
            double lonRif = riferimento[1];

            // 2) la query viene costruita incrementalmente: ogni filtro presente
            //    nella richiesta aggiunge una clausola AND con il relativo
            //    parametro '?'. I valori non vengono mai concatenati come
            //    testo, a difesa da SQL injection.
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

            // Il filtro sulla distanza e' collocato nella clausola HAVING; la
            // formula va ripetuta perche' l'alias distanza_km, definito nel
            // SELECT, non e' referenziabile in HAVING. E' previsto un solo
            // limite massimo: la distanza minima e' implicitamente 0, poiche'
            // il riferimento coincide con la citta' stessa.
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
            // Il criterio di ordinamento non puo' essere legato con '?', poiche'
            // un PreparedStatement parametrizza solo valori, non identificatori
            // di colonna: viene quindi selezionata una tra un insieme fisso di
            // clausole ORDER BY predefinite nel codice, mai derivate dal client.
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
     * Traduce il criterio di ordinamento richiesto dal client nella
     * corrispondente clausola ORDER BY. A parita' di cucina, prezzo o
     * valutazione, la distanza costituisce sempre il criterio finale di
     * ordinamento.
     *
     * @param ordinamento {@code "cucina"}, {@code "prezzoAsc"}, {@code "prezzoDesc"},
     *                    {@code "valutazione"}, oppure altro valore o {@code null}
     *                    per l'ordinamento predefinito
     * @return frammento SQL da utilizzare dopo ORDER BY; corrisponde sempre a una
     *         delle costanti predefinite nel codice, mai a testo proveniente dal
     *         client, a garanzia contro la SQL injection
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
                // media_stelle corrisponde a COALESCE(AVG(rec.stelle), 0): un
                // ristorante privo di recensioni assume sempre valore 0, quindi
                // l'ordinamento decrescente colloca automaticamente in coda i
                // ristoranti senza recensioni, senza necessita' di un criterio
                // distinto. Se nessun risultato possiede recensioni, tutti i
                // valori sono 0 e l'ordinamento finale coincide con la distanza.
                return "media_stelle DESC, distanza_km";
            default:
                return "distanza_km";
        }
    }

    /**
     * Elenco alfabetico delle citta' distinte presenti nel database,
     * utilizzato dal client per popolare il menu a tendina della ricerca
     * ed evitare l'inserimento manuale di nomi di citta' non validi.
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
     * Restituisce il dettaglio di un singolo ristorante, con la media
     * delle stelle e il numero di recensioni aggiornati alla situazione
     * corrente.
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
     * Inserisce un nuovo ristorante associato al gestore autenticato.
     * <p>
     * La validazione delle coordinate geografiche prevede tre controlli:
     * (1) latitudine e longitudine sono facoltative; se omesse e la
     * citta' e' gia' presente nel database, viene utilizzato il relativo
     * baricentro; (2) se indicate, devono rientrare nei range geografici
     * validi; (3) se la citta' e' gia' nota, non possono distare piu' di
     * {@value #SCARTO_MAX_KM} km dagli altri ristoranti della stessa
     * citta', poiche' un valore fuori soglia e' con elevata probabilita'
     * un errore di inserimento.
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
                // Coordinate non fornite: se la citta' e' gia' nota, si utilizza
                // il relativo baricentro, evitando che il gestore debba
                // reperire manualmente le coordinate (con il conseguente
                // rischio di errore).
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
                // Controllo di coerenza: coordinate significativamente distanti
                // dagli altri ristoranti della citta' sono con elevata
                // probabilita' errate, e un valore errato comprometterebbe la
                // ricerca per l'intera citta'.
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
     * Assegna al gestore indicato un ristorante attualmente privo di
     * gestore, tipicamente uno di quelli importati dal dataset iniziale.
     * <p>
     * La condizione {@code id_gestore IS NULL} nella clausola WHERE rende
     * l'operazione atomica: l'aggiornamento ha effetto solo se il
     * ristorante non e' gia' stato assegnato. In caso di richieste
     * concorrenti da parte di piu' gestori, il DBMS serializza le
     * istruzioni e una sola di esse modifica la riga, mentre le altre
     * riscontrano zero righe aggiornate e vengono respinte.
     *
     * @param idGestore    identificativo del gestore (dalla sessione server)
     * @param idRistorante identificativo del ristorante da prendere in carico
     * @return risposta di successo, oppure errore se il ristorante non
     *         esiste o risulta gia' assegnato a un gestore
     * @throws SQLException in caso di errore di accesso al database
     */
    public Response rivendicaRistorante(int idGestore, int idRistorante) throws SQLException {
        String sql = "UPDATE RistorantiTheKnife SET id_gestore = ? "
                + "WHERE id = ? AND id_gestore IS NULL";
        try (Connection conn = DBConnection.getIstanza().nuovaConnessione();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idGestore);
            ps.setInt(2, idRistorante);
            if (ps.executeUpdate() == 0) {
                return Response.errore("Il ristorante non esiste oppure e' gia' "
                        + "gestito da un altro utente");
            }
            return Response.ok(null);
        }
    }

    /**
     * Calcola il baricentro geografico di una citta', inteso come la
     * media di latitudine e longitudine dei ristoranti in essa presenti.
     * Il valore e' utilizzato sia dalla ricerca per distanza sia dai
     * controlli di validazione in fase di inserimento.
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
                // AVG su un insieme vuoto restituisce NULL, ma getDouble lo
                // converte silenziosamente in 0.0: la distinzione e' possibile
                // solo interrogando wasNull() subito dopo la lettura.
                if (rs.wasNull()) {
                    return null;
                }
                return new double[]{lat, rs.getDouble("lon")};
            }
        }
    }

    /**
     * Calcola la distanza in km tra due punti geografici identificati dalle loro coordinate,
     * mediante la formula di Haversine. Costituisce l'equivalente in Java
     * della medesima formula utilizzata in SQL dalla ricerca: le due
     * implementazioni devono rimanere identiche affinche' i controlli di
     * coerenza sulle coordinate siano validi.
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
     * Restituisce, per il gestore autenticato, l'elenco dei propri
     * ristoranti con la relativa media delle stelle e il numero di
     * recensioni ricevute.
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
     * Costruisce un oggetto {@link Ristorante} a partire dalla riga
     * corrente del ResultSet, che deve includere anche le colonne
     * calcolate {@code media_stelle} e {@code numero_recensioni}.
     * Visibilita' di package anziche' privata poiche' il metodo e'
     * riutilizzato anche da {@link PreferitoDAO}, che produce risultati
     * nello stesso formato.
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
