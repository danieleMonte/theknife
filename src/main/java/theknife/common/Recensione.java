/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

import java.io.Serializable;

/**
 * Una recensione: stelle da 1 a 5, testo e l'eventuale risposta del
 * gestore (le specifiche ne ammettono al massimo una per recensione,
 * quindi basta un campo e non serve una tabella a parte).
 *
 * @author Daniele Montefiore
 */
public class Recensione implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int idUtente;
    private int idRistorante;
    private int stelle;
    private String testo;
    private String risposta; // null se il gestore non ha ancora risposto

    // nome del ristorante recensito: lo riempie il server con una JOIN
    // per la schermata "le mie recensioni", altrimenti resta null
    private String nomeRistorante;

    /** Costruttore vuoto. */
    public Recensione() { }

    /**
     * Costruttore completo.
     *
     * @param id           identificativo della recensione
     * @param idUtente     identificativo dell'autore
     * @param idRistorante identificativo del ristorante recensito
     * @param stelle       numero di stelle (da 1 a 5)
     * @param testo        testo della recensione
     * @param risposta     risposta del gestore ({@code null} se assente)
     */
    public Recensione(int id, int idUtente, int idRistorante, int stelle, String testo, String risposta) {
        this.id = id;
        this.idUtente = idUtente;
        this.idRistorante = idRistorante;
        this.stelle = stelle;
        this.testo = testo;
        this.risposta = risposta;
    }

    /** @return identificativo della recensione */
    public int getId() { return id; }

    /** @param id identificativo della recensione */
    public void setId(int id) { this.id = id; }

    /** @return identificativo dell'autore */
    public int getIdUtente() { return idUtente; }

    /** @param idUtente identificativo dell'autore */
    public void setIdUtente(int idUtente) { this.idUtente = idUtente; }

    /** @return identificativo del ristorante recensito */
    public int getIdRistorante() { return idRistorante; }

    /** @param idRistorante identificativo del ristorante recensito */
    public void setIdRistorante(int idRistorante) { this.idRistorante = idRistorante; }

    /** @return numero di stelle (da 1 a 5) */
    public int getStelle() { return stelle; }

    /** @param stelle numero di stelle (da 1 a 5) */
    public void setStelle(int stelle) { this.stelle = stelle; }

    /** @return testo della recensione */
    public String getTesto() { return testo; }

    /** @param testo testo della recensione */
    public void setTesto(String testo) { this.testo = testo; }

    /** @return risposta del gestore, {@code null} se assente */
    public String getRisposta() { return risposta; }

    /** @param risposta risposta del gestore */
    public void setRisposta(String risposta) { this.risposta = risposta; }

    /** @return nome del ristorante recensito, {@code null} se non richiesto */
    public String getNomeRistorante() { return nomeRistorante; }

    /** @param nomeRistorante nome del ristorante recensito */
    public void setNomeRistorante(String nomeRistorante) { this.nomeRistorante = nomeRistorante; }

    @Override
    public String toString() {
        return "★".repeat(Math.max(1, stelle)) + " - " + testo;
    }
}
