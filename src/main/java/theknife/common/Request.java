/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Messaggio di richiesta inviato dal client al server. Specifica
 * l'operazione da eseguire e trasporta i parametri necessari alla sua
 * esecuzione (criteri di ricerca, dati di una recensione, ecc.).
 *
 * @author Daniele Montefiore
 */
public class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Operazione operazione;
    private final Map<String, Object> parametri = new HashMap<>();

    /**
     * Crea una richiesta per l'operazione indicata.
     *
     * @param operazione operazione richiesta al server
     */
    public Request(Operazione operazione) {
        this.operazione = operazione;
    }

    /** @return operazione richiesta */
    public Operazione getOperazione() { return operazione; }

    /**
     * Registra la coppia (chiave, valore) tra i parametri della richiesta.
     * Restituisce l'istanza corrente per consentire la concatenazione di
     * piu' chiamate in fase di costruzione, ad esempio
     * {@code new Request(LOGIN).con("username", u).con("password", p)}.
     *
     * @param chiave nome del parametro, con cui il server lo rilegge tramite
     *               {@link #get(String)}
     * @param valore valore del parametro (deve essere serializzabile)
     * @return questa stessa richiesta, per la concatenazione
     */
    public Request con(String chiave, Object valore) {
        parametri.put(chiave, valore);
        return this;
    }

    /**
     * Restituisce il valore associato a un parametro. Il tipo effettivo
     * e' noto al chiamante, che effettua il cast appropriato.
     *
     * @param chiave nome del parametro
     * @return valore del parametro, o {@code null} se non presente
     */
    public Object get(String chiave) {
        return parametri.get(chiave);
    }
}
