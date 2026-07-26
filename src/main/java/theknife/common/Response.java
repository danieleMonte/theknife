/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

import java.io.Serializable;

/**
 * La risposta del server: dice se l'operazione e' andata bene, porta
 * l'eventuale messaggio di errore e i dati richiesti (per esempio la
 * lista dei ristoranti trovati). Il costruttore e' privato apposta:
 * una Response si crea solo con ok() o errore(), cosi' non possono
 * esistere risposte "a meta'".
 *
 * @author Daniele Montefiore
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean successo;
    private final String messaggio;
    private final Object dati;

    private Response(boolean successo, String messaggio, Object dati) {
        this.successo = successo;
        this.messaggio = messaggio;
        this.dati = dati;
    }

    /**
     * Operazione riuscita.
     *
     * @param dati dati da restituire al client (anche {@code null})
     * @return risposta di successo
     */
    public static Response ok(Object dati) {
        return new Response(true, null, dati);
    }

    /**
     * Operazione fallita.
     *
     * @param messaggio spiegazione dell'errore, da far vedere all'utente
     * @return risposta di errore
     */
    public static Response errore(String messaggio) {
        return new Response(false, messaggio, null);
    }

    /** @return {@code true} se l'operazione e' andata a buon fine */
    public boolean isSuccesso() { return successo; }

    /** @return messaggio d'errore, {@code null} in caso di successo */
    public String getMessaggio() { return messaggio; }

    /** @return dati restituiti dal server */
    public Object getDati() { return dati; }
}
