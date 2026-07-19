/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

/**
 * Stato della sessione lato client (pattern Singleton): l'utente loggato,
 * oppure il luogo indicato in caso di accesso come guest.
 *
 * @author Daniele Montefiore
 */
public final class Sessione {

    private static final Sessione ISTANZA = new Sessione();

    private String luogoGuest; // luogo indicato dal guest, null se loggato

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
    /* Da creare la classe Utente 
    */

    /**
     * Inizia una sessione guest.
     *
     * @param luogo luogo indicato dal guest
     */
    public void accediComeGuest(String luogo) {
        // this.utente = null; // manca la classe utente
        this.luogoGuest = luogo;
    }

    /** Termina la sessione corrente. */
    public void esci() {
        // this.utente = null; // manca la classe utente
        this.luogoGuest = null;
    }

   

    /** @return {@code true} se c'e' un utente loggato */
    public boolean isLoggato() { return false; } // [PROVVISORIO: "utente != null"]

    /**
     * Luogo di riferimento per l'elenco dei ristoranti "vicini":
     * il domicilio dell'utente loggato, o il luogo indicato dal guest.
     *
     * @return luogo di riferimento della sessione
     */
    public String getLuogo() {
        // [PROVVISORIO: "utente != null ? utente.getDomicilio() : luogoGuest"]
        return luogoGuest;
    }
}
