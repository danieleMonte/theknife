/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import java.util.ArrayList;
import java.util.List;

/**
 * Autocompletamento delle citta' sui menu a tendina del client:
 * l'elenco delle citta' viene richiesto al server una sola volta e
 * riutilizzato da tutte le schermate; digitando nel campo l'elenco
 * si restringe alle citta' che contengono il testo inserito.
 *
 * @author Daniele Montefiore
 */
public final class AutoCompletamento {

    /** Numero massimo di suggerimenti mostrati nel menu a tendina. */
    private static final int MAX_SUGGERIMENTI = 50;

    /** Cache dell'elenco citta' (caricato dal server alla prima richiesta). */
    private static List<String> citta;

    private AutoCompletamento() { }

    /**
     * Elenco delle citta' del database, richiesto al server la prima volta.
     * Se il server non e' raggiungibile restituisce una lista vuota:
     * il menu a tendina si comporta allora come un normale campo di testo.
     *
     * @return elenco (eventualmente vuoto) dei nomi delle citta'
     */
    public static synchronized List<String> elencoCitta() {
        if (citta == null) {
            /* richiesta al server per ottenere l'elenco delle citta'
            */
            citta = new ArrayList<>(); // [PROVVISORIO: senza server nessun suggerimento]
        }
        return citta;
    }

    /**
     * Rende il ComboBox un campo citta' con autocompletamento:
     * editabile, popolato con le citta' del database e filtrato
     * man mano che l'utente digita.
     *
     * @param combo menu a tendina da configurare
     */
    public static void applicaCitta(ComboBox<String> combo) {
        List<String> tutte = elencoCitta();
        combo.setEditable(true);
        combo.setItems(FXCollections.observableArrayList(filtra(tutte, "")));

        // Flag per distinguere le modifiche fatte da questo listener da quelle
        // dell'utente: senza, il ripristino del testo rilancerebbe il listener.
        // (array di un elemento perche' una variabile locale usata in una
        // lambda deve essere effettivamente final)
        final boolean[] aggiornamentoInterno = {false};

        combo.getEditor().textProperty().addListener((oss, vecchio, testo) -> {
            if (aggiornamentoInterno[0] || testo == null) {
                return;
            }
            // testo appena impostato scegliendo dall'elenco: non rifiltrare
            String selezionata = combo.getSelectionModel().getSelectedItem();
            if (testo.equals(selezionata)) {
                return;
            }
            List<String> filtrate = filtra(tutte, testo.trim().toLowerCase());

            aggiornamentoInterno[0] = true;
            // Azzera la selezione PRIMA di sostituire gli elementi: se restasse
            // attiva, il cambio di elementi riscriverebbe nell'editor il testo
            // della vecchia selezione, impedendo di modificare il campo.
            combo.getSelectionModel().clearSelection();
            combo.setItems(FXCollections.observableArrayList(filtrate));
            // Ripristina cio' che l'utente aveva digitato e il cursore in fondo
            combo.getEditor().setText(testo);
            combo.getEditor().positionCaret(testo.length());
            aggiornamentoInterno[0] = false;

            if (!filtrate.isEmpty() && combo.isFocused()) {
                combo.show();
            } else if (filtrate.isEmpty()) {
                combo.hide();
            }
        });
    }

    /**
     * Restituisce le citta' che contengono il testo cercato (ignorando
     * maiuscole/minuscole), al massimo {@value #MAX_SUGGERIMENTI}.
     * Con filtro vuoto restituisce le prime citta' dell'elenco.
     *
     * @param tutte  elenco completo delle citta'
     * @param filtro testo cercato, gia' in minuscolo
     * @return citta' filtrate
     */
    private static List<String> filtra(List<String> tutte, String filtro) {
        List<String> risultato = new ArrayList<>();
        for (String nome : tutte) {
            if (filtro.isEmpty() || nome.toLowerCase().contains(filtro)) {
                risultato.add(nome);
                if (risultato.size() >= MAX_SUGGERIMENTI) {
                    break;
                }
            }
        }
        return risultato;
    }

    /**
     * Testo attualmente presente nel campo (digitato o scelto dall'elenco).
     *
     * @param combo menu a tendina configurato con {@link #applicaCitta}
     * @return testo del campo, senza spazi iniziali e finali
     */
    public static String testo(ComboBox<String> combo) {
        String testo = combo.getEditor().getText();
        return testo == null ? "" : testo.trim();
    }
}
