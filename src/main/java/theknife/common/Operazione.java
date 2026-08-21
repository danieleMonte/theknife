/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

/**
 * Enumerazione delle operazioni che il client puo' richiedere al server:
 * a ciascuna costante corrisponde una funzionalita' prevista dalle
 * specifiche di progetto. Il ruolo eventualmente richiesto per
 * l'esecuzione e' indicato tra parentesi.
 *
 * @author Daniele Montefiore
 */
public enum Operazione {
    /** Autenticazione di un utente registrato. */
    LOGIN,
    /** Registrazione di un nuovo utente (cliente o gestore). */
    REGISTRAZIONE,
    /** Ricerca dei ristoranti per criteri combinati, ordinati per distanza. */
    CERCA_RISTORANTE,
    /** Elenco delle citta' presenti nel database (per i menu a tendina). */
    ELENCO_CITTA,
    /** Dettaglio di un singolo ristorante. */
    VISUALIZZA_RISTORANTE,
    /** Elenco delle recensioni di un ristorante. */
    VISUALIZZA_RECENSIONI,
    /** Aggiunta di un ristorante alla lista dei preferiti (cliente). */
    AGGIUNGI_PREFERITO,
    /** Rimozione di un ristorante dalla lista dei preferiti (cliente). */
    RIMUOVI_PREFERITO,
    /** Elenco dei ristoranti preferiti (cliente). */
    VISUALIZZA_PREFERITI,
    /** Inserimento di una nuova recensione (cliente). */
    AGGIUNGI_RECENSIONE,
    /** Modifica di una recensione esistente (cliente). */
    MODIFICA_RECENSIONE,
    /** Cancellazione di una recensione esistente (cliente). */
    ELIMINA_RECENSIONE,
    /** Elenco delle recensioni scritte dall'utente loggato (cliente). */
    VISUALIZZA_MIE_RECENSIONI,
    /** Inserimento di un nuovo ristorante (gestore). */
    AGGIUNGI_RISTORANTE,
    /** Presa in carico di un ristorante privo di gestore (gestore). */
    RIVENDICA_RISTORANTE,
    /** Riepilogo recensioni dei propri ristoranti (gestore). */
    VISUALIZZA_RIEPILOGO,
    /** Risposta a una recensione (gestore). */
    RISPOSTA_RECENSIONE,
    /** Chiusura della sessione dell'utente loggato. */
    LOGOUT
}
