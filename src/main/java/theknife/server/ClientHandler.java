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
 * Gestisce la sessione di un singolo client su un thread dedicato: legge
 * una {@link Request} alla volta, la smista all'operazione corrispondente
 * e restituisce la {@link Response} risultante.
 * <p>
 * L'utente autenticato e' mantenuto nell'handler stesso, che coincide
 * quindi con la sessione del client. La scelta risponde a un requisito di
 * sicurezza: le operazioni riservate utilizzano l'identita' verificata dal
 * server in fase di login e mai un identificativo trasmesso dal client, che
 * altrimenti potrebbe essere alterato per impersonare un altro utente.
 *
 * @author Daniele Montefiore
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final UtenteDAO utenteDAO = new UtenteDAO();
    private final RistoranteDAO ristoranteDAO = new RistoranteDAO();
    private final RecensioneDAO recensioneDAO = new RecensioneDAO();
    private final PreferitoDAO preferitoDAO = new PreferitoDAO();

    /** Utente autenticato nella sessione corrente; {@code null} per gli utenti guest. */
    private Utente utenteLoggato;

    /**
     * Crea l'handler per il client connesso sul socket indicato.
     *
     * @param socket socket del client accettato dal server
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /** Elabora le richieste del client finche' la connessione rimane aperta. */
    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (true) {
                Request richiesta = (Request) in.readObject();
                Response risposta = gestisci(richiesta);
                out.writeObject(risposta);
                out.flush();
                // Senza reset() lo stream mantiene in cache gli oggetti gia'
                // serializzati e, a fronte di un successivo invio, trasmetterebbe
                // un riferimento alla versione precedente anziche' i dati aggiornati.
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
                // Il fallimento della chiusura a fine sessione non richiede
                // ulteriori azioni correttive.
            }
        }
    }

    /**
     * Smista la richiesta all'operazione corrispondente. Le operazioni
     * riservate verificano preliminarmente il ruolo dell'utente autenticato.
     * Il blocco try/catch esterno converte qualsiasi eccezione in una
     * risposta di errore, garantendo che il client riceva sempre un esito e
     * non rimanga in attesa indefinita.
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
                case RIVENDICA_RISTORANTE:
                    if (!isGestore()) {
                        return Response.errore("Operazione riservata ai gestori registrati");
                    }
                    return ristoranteDAO.rivendicaRistorante(utenteLoggato.getId(),
                            (Integer) richiesta.get("idRistorante"));
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
     * Esegue l'autenticazione delegando al DAO e, in caso di esito
     * positivo, registra l'identita' dell'utente nella sessione corrente.
     * Per questa ragione l'operazione non e' inoltrata direttamente al DAO
     * come le altre.
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

    /** @return {@code true} se nella sessione corrente e' autenticato un cliente */
    private boolean isCliente() {
        return utenteLoggato != null && utenteLoggato.getRuolo() == Ruolo.CLIENTE;
    }

    /** @return {@code true} se nella sessione corrente e' autenticato un gestore */
    private boolean isGestore() {
        return utenteLoggato != null && utenteLoggato.getRuolo() == Ruolo.GESTORE;
    }
}
