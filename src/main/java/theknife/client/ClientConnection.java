/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

// [DISATTIVATO: manca theknife.common]
// import theknife.common.Request;
// import theknife.common.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Connessione del client verso il server TheKnife (pattern Singleton).
 * Apre il socket alla prima richiesta e serializza l'invio delle
 * richieste, cosi' da poter essere usata da piu' punti della GUI.
 *
 * @author Daniele Montefiore
 */
public final class ClientConnection {

    /** Host del server a cui connettersi. */
    public static final String HOST = "localhost";
    /** Porta del server a cui connettersi. */
    public static final int PORTA = 4444;

    private static ClientConnection istanza;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private ClientConnection() { }

    /**
     * Restituisce l'istanza unica della connessione.
     *
     * @return istanza del singleton
     */
    public static synchronized ClientConnection getIstanza() {
        if (istanza == null) {
            istanza = new ClientConnection();
        }
        return istanza;
    }

    /**
     * Invia una richiesta al server e ne attende la risposta.
     * La connessione viene aperta alla prima chiamata.
     *
     * @param richiesta richiesta da inviare
     * @return risposta del server, o una risposta di errore se la
     *         comunicazione fallisce
     */
    /* da creare l'invio della richiesta al server e la ricezione della risposta 
    */

    private void connetti() throws IOException {
        socket = new Socket(HOST, PORTA);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    /** Chiude la connessione col server, se aperta. */
    public synchronized void chiudi() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // la chiusura fallita non e' un errore per l'utente
        }
    }
}
