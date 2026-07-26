/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

/**
 * Tutte le operazioni che il client puo' chiedere al server: una
 * costante per ogni funzionalita' delle specifiche. Dove serve un
 * ruolo particolare l'ho segnato tra parentesi.
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
    /** Riepilogo recensioni dei propri ristoranti (gestore). */
    VISUALIZZA_RIEPILOGO,
    /** Risposta a una recensione (gestore). */
    RISPOSTA_RECENSIONE,
    /** Chiusura della sessione dell'utente loggato. */
    LOGOUT
}
