/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * E' il messaggio che il client manda al server: dice quale operazione
 * vuole fare e si porta dietro i parametri che servono (i filtri di
 * ricerca, i dati di una recensione, ecc.). Ho usato una mappa generica
 * cosi' non devo creare una classe diversa per ogni operazione.
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
     * Aggiunge un parametro e restituisce la richiesta stessa, cosi'
     * posso concatenare piu' .con() uno dietro l'altro quando la creo.
     *
     * @param chiave nome del parametro
     * @param valore valore del parametro (deve essere serializzabile)
     * @return questa stessa richiesta
     */
    public Request con(String chiave, Object valore) {
        parametri.put(chiave, valore);
        return this;
    }

    /**
     * Legge un parametro; chi chiama sa gia' di che tipo e' e fa il cast.
     *
     * @param chiave nome del parametro
     * @return valore del parametro, o {@code null} se non c'e'
     */
    public Object get(String chiave) {
        return parametri.get(chiave);
    }
}
