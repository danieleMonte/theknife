/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

import theknife.common.Utente;

/**
 * Mantiene lo stato della sessione lato client: l'utente autenticato
 * oppure il luogo indicato in caso di accesso come guest. E' implementata
 * come Singleton affinche' tutte le schermate condividano la medesima
 * sessione.
 * <p>
 * Le informazioni qui memorizzate hanno finalita' esclusivamente di
 * presentazione: la verifica effettiva dei permessi e' comunque
 * ripetuta dal server a ogni richiesta.
 *
 * @author Daniele Montefiore
 */
public final class Sessione {

    private static final Sessione ISTANZA = new Sessione();

    private Utente utente;     // null in caso di accesso come guest
    private String luogoGuest; // luogo indicato dal guest; null se autenticato

    private Sessione() { }

    /**
     * Restituisce l'istanza unica della sessione.
     *
     * @return istanza del singleton
     */
    public static Sessione getIstanza() {
        return ISTANZA;
    }

    /**
     * Inizia una sessione autenticata.
     *
     * @param utente utente autenticato dal server
     */
    public void accedi(Utente utente) {
        this.utente = utente;
        this.luogoGuest = null;
    }

    /**
     * Inizia una sessione guest.
     *
     * @param luogo luogo indicato dal guest
     */
    public void accediComeGuest(String luogo) {
        this.utente = null;
        this.luogoGuest = luogo;
    }

    /** Termina la sessione corrente. */
    public void esci() {
        this.utente = null;
        this.luogoGuest = null;
    }

    /** @return utente autenticato, {@code null} in caso di accesso come guest */
    public Utente getUtente() { return utente; }

    /** @return {@code true} se e' presente un utente autenticato */
    public boolean isLoggato() { return utente != null; }

    /**
     * Restituisce il luogo di riferimento per la ricerca dei ristoranti
     * vicini: il domicilio dell'utente autenticato oppure, in assenza di
     * autenticazione, il luogo indicato dal guest.
     *
     * @return luogo di riferimento della sessione
     */
    public String getLuogo() {
        return utente != null ? utente.getDomicilio() : luogoGuest;
    }
}
