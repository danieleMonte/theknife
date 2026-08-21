/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

import java.io.Serializable;

/**
 * Messaggio di risposta inviato dal server al client. Comunica l'esito
 * dell'operazione, l'eventuale messaggio di errore e i dati richiesti.
 * <p>
 * Il costruttore e' privato e l'istanziazione avviene esclusivamente
 * tramite i metodi factory {@link #ok(Object)} e {@link #errore(String)},
 * in modo da impedire la creazione di risposte in stato incoerente.
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
     * Crea una risposta di esito positivo.
     *
     * @param dati dati da restituire al client (eventualmente {@code null})
     * @return risposta di successo
     */
    public static Response ok(Object dati) {
        return new Response(true, null, dati);
    }

    /**
     * Crea una risposta di esito negativo.
     *
     * @param messaggio descrizione dell'errore, destinata alla
     *                  visualizzazione all'utente
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
