/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

import theknife.common.Request;
import theknife.common.Response;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Gestisce l'unica connessione del client verso il server, secondo il
 * pattern Singleton. Il socket viene aperto alla prima richiesta; il
 * metodo {@link #invia(Request)} e' dichiarato {@code synchronized}
 * poiche' sullo stesso socket le coppie richiesta/risposta devono
 * susseguirsi in modo ordinato e non possono sovrapporsi.
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
     * Invia una richiesta al server e ne attende la risposta. In caso di
     * errore di comunicazione (server non attivo o rete non disponibile)
     * non viene propagata alcuna eccezione verso l'interfaccia grafica: il
     * metodo restituisce una normale risposta di errore, che i controller
     * gestiscono come qualsiasi altro esito negativo.
     *
     * @param richiesta richiesta da inviare
     * @return risposta del server, o una risposta di errore se la
     *         comunicazione fallisce
     */
    public synchronized Response invia(Request richiesta) {
        try {
            if (socket == null || socket.isClosed()) {
                connetti();
            }
            out.writeObject(richiesta);
            out.flush();
            out.reset();
            return (Response) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // La connessione va scartata: dopo un errore di comunicazione il
            // socket resta aperto ma inutilizzabile e, non risultando chiuso,
            // non verrebbe mai sostituito, impedendo ogni successivo tentativo
            // anche a server nuovamente disponibile. Azzerandolo qui, la
            // richiesta seguente ne apre uno nuovo e la connessione si ristabilisce.
            scarta();
            return Response.errore("Impossibile comunicare col server: " + e.getMessage());
        }
    }

    /**
     * Chiude e azzera il socket corrente, in modo che la richiesta successiva
     * ne apra uno nuovo. I riferimenti agli stream vengono rimossi insieme al
     * socket: se {@code close()} fallisse, un socket non ancora chiuso
     * verrebbe altrimenti riutilizzato con stream ormai inservibili.
     */
    private void scarta() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // La connessione viene comunque abbandonata: l'esito della
            // chiusura non modifica il comportamento successivo.
        } finally {
            socket = null;
            out = null;
            in = null;
        }
    }

    private void connetti() throws IOException {
        socket = new Socket(HOST, PORTA);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    /** Chiude la connessione verso il server, se aperta. */
    public synchronized void chiudi() {
        scarta();
    }
}
