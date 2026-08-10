/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.server;

import theknife.common.Request;
import theknife.common.Response;
import theknife.common.Ruolo;
import theknife.common.Utente;
import theknife.server.dao.PreferitoDAO;
import theknife.server.dao.RecensioneDAO;
import theknife.server.dao.RistoranteDAO;
import theknife.server.dao.UtenteDAO;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Serve un singolo client, su un thread tutto suo: legge una
 * {@link Request} alla volta, la smista con lo switch e rimanda
 * indietro la {@link Response}.
 * <p>
 * L'utente loggato sta qui dentro (un handler = una sessione). E' una
 * scelta di sicurezza: per le operazioni riservate uso l'identita' che
 * il server ha verificato col login, mai un id mandato dal client —
 * altrimenti basterebbe un client modificato per spacciarsi per un altro.
 *
 * @author Daniele Montefiore
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final UtenteDAO utenteDAO = new UtenteDAO();
    private final RistoranteDAO ristoranteDAO = new RistoranteDAO();
    private final RecensioneDAO recensioneDAO = new RecensioneDAO();
    private final PreferitoDAO preferitoDAO = new PreferitoDAO();

    /** Chi ha fatto il login in questa sessione; null finche' si e' guest. */
    private Utente utenteLoggato;

    /**
     * Crea l'handler per il client connesso sul socket indicato.
     *
     * @param socket socket del client accettato dal server
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /** Cicla sulle richieste del client finche' la connessione resta aperta. */
    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (true) {
                Request richiesta = (Request) in.readObject();
                Response risposta = gestisci(richiesta);
                out.writeObject(risposta);
                out.flush();
                // senza reset() lo stream ricorda gli oggetti gia' inviati e
                // alla seconda risposta uguale manderebbe quella vecchia
                out.reset();
            }
        } catch (EOFException e) {
            System.out.println("Client disconnesso: " + socket.getInetAddress());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Errore nella sessione client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // la chiusura fallita a fine sessione non richiede altre azioni
            }
        }
    }

    /**
     * Lo smistamento vero e proprio. Le operazioni riservate controllano
     * il ruolo come prima cosa. Il try/catch esterno trasforma qualunque
     * eccezione in una Response di errore: il client deve ricevere sempre
     * una risposta, altrimenti resterebbe bloccato in attesa.
     *
     * @param richiesta richiesta ricevuta dal client
     * @return risposta da inviare al client
     */
    private Response gestisci(Request richiesta) {
        try {
            switch (richiesta.getOperazione()) {

                // --- Operazioni che non richiedono il login ---
                case LOGIN:
                    return login(richiesta);
                case REGISTRAZIONE:
                    return utenteDAO.registrazione(richiesta);
                case LOGOUT:
                    utenteLoggato = null;
                    return Response.ok(null);
                case CERCA_RISTORANTE:
                    return ristoranteDAO.cercaRistorante(richiesta);
                case ELENCO_CITTA:
                    return ristoranteDAO.elencoCitta();
                case VISUALIZZA_RISTORANTE:
                    return ristoranteDAO.visualizzaRistorante((Integer) richiesta.get("idRistorante"));
                case VISUALIZZA_RECENSIONI:
                    return recensioneDAO.visualizzaRecensioni((Integer) richiesta.get("idRistorante"));

                // --- Operazioni riservate ai clienti ---
                case AGGIUNGI_PREFERITO:
                    if (!isCliente()) {
                        return Response.errore("Operazione riservata ai clienti registrati");
                    }
                    return preferitoDAO.aggiungiPreferito(utenteLoggato.getId(),
                            (Integer) richiesta.get("idRistorante"));
                case RIMUOVI_PREFERITO:
                    if (!isCliente()) {
                        return Response.errore("Operazione riservata ai clienti registrati");
                    }
                    return preferitoDAO.rimuoviPreferito(utenteLoggato.getId(),
                            (Integer) richiesta.get("idRistorante"));
                case VISUALIZZA_PREFERITI:
                    if (!isCliente()) {
                        return Response.errore("Operazione riservata ai clienti registrati");
                    }
                    return preferitoDAO.visualizzaPreferiti(utenteLoggato.getId());
                case AGGIUNGI_RECENSIONE:
                    if (!isCliente()) {
                        return Response.errore("Operazione riservata ai clienti registrati");
                    }
                    return recensioneDAO.aggiungiRecensione(utenteLoggato.getId(), richiesta);
                case MODIFICA_RECENSIONE:
                    if (!isCliente()) {
                        return Response.errore("Operazione riservata ai clienti registrati");
                    }
                    return recensioneDAO.modificaRecensione(utenteLoggato.getId(), richiesta);
                case ELIMINA_RECENSIONE:
                    if (!isCliente()) {
                        return Response.errore("Operazione riservata ai clienti registrati");
                    }
                    return recensioneDAO.eliminaRecensione(utenteLoggato.getId(),
                            (Integer) richiesta.get("idRecensione"));
                case VISUALIZZA_MIE_RECENSIONI:
                    if (!isCliente()) {
                        return Response.errore("Operazione riservata ai clienti registrati");
                    }
                    return recensioneDAO.mieRecensioni(utenteLoggato.getId());

                // --- Operazioni riservate ai gestori ---
                case AGGIUNGI_RISTORANTE:
                    if (!isGestore()) {
                        return Response.errore("Operazione riservata ai gestori registrati");
                    }
                    return ristoranteDAO.aggiungiRistorante(utenteLoggato.getId(), richiesta);
                case VISUALIZZA_RIEPILOGO:
                    if (!isGestore()) {
                        return Response.errore("Operazione riservata ai gestori registrati");
                    }
                    return ristoranteDAO.visualizzaRiepilogo(utenteLoggato.getId());
                case RISPOSTA_RECENSIONE:
                    if (!isGestore()) {
                        return Response.errore("Operazione riservata ai gestori registrati");
                    }
                    return recensioneDAO.rispostaRecensione(utenteLoggato.getId(), richiesta);

                default:
                    return Response.errore("Operazione non riconosciuta: "
                            + richiesta.getOperazione());
            }
        } catch (Exception e) {
            return Response.errore("Errore del server: " + e.getMessage());
        }
    }

    /**
     * Il login passa da qui e non va dritto al DAO perche', se va a buon
     * fine, devo ricordarmi chi si e' autenticato in questa sessione.
     *
     * @param richiesta richiesta con username e password
     * @return risposta del login
     * @throws Exception in caso di errore di accesso al database
     */
    private Response login(Request richiesta) throws Exception {
        Response risposta = utenteDAO.login(
                (String) richiesta.get("username"),
                (String) richiesta.get("password"));
        if (risposta.isSuccesso()) {
            utenteLoggato = (Utente) risposta.getDati();
        }
        return risposta;
    }

    /** @return {@code true} se in questa sessione e' loggato un cliente */
    private boolean isCliente() {
        return utenteLoggato != null && utenteLoggato.getRuolo() == Ruolo.CLIENTE;
    }

    /** @return {@code true} se in questa sessione e' loggato un gestore */
    private boolean isGestore() {
        return utenteLoggato != null && utenteLoggato.getRuolo() == Ruolo.GESTORE;
    }
}
