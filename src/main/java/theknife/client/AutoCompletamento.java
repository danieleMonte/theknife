/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import theknife.common.Operazione;
import theknife.common.Request;
import theknife.common.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementa l'autocompletamento delle citta' nei menu a tendina del
 * client. L'elenco completo e' richiesto al server una sola volta e
 * mantenuto in cache per tutte le schermate; durante la digitazione le
 * voci proposte sono filtrate in base al testo inserito.
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
     * Restituisce l'elenco delle citta' presenti nel database,
     * richiedendolo al server alla prima invocazione. In caso di server
     * non raggiungibile viene restituita una lista vuota anziche' un
     * errore: il campo resta utilizzabile, pur senza suggerimenti.
     *
     * @return elenco (eventualmente vuoto) dei nomi delle citta'
     */
    @SuppressWarnings("unchecked")
    public static synchronized List<String> elencoCitta() {
        if (citta == null) {
            Response risposta = ClientConnection.getIstanza().invia(new Request(Operazione.ELENCO_CITTA));
            if (risposta.isSuccesso()) {
                citta = (List<String>) risposta.getDati();
            } else {
                citta = new ArrayList<>();
            }
        }
        return citta;
    }

    /**
     * Configura il ComboBox come campo citta' con autocompletamento:
     * lo rende editabile, lo popola con le citta' presenti nel database e
     * ne filtra le voci durante la digitazione.
     *
     * @param combo menu a tendina da configurare
     */
    public static void applicaCitta(ComboBox<String> combo) {
        List<String> tutte = elencoCitta();
        combo.setEditable(true);
        combo.setItems(FXCollections.observableArrayList(filtra(tutte, "")));

        // Flag che distingue le modifiche al testo effettuate dall'utente da
        // quelle operate dal listener stesso: in sua assenza il ripristino del
        // testo eseguito piu' avanti provocherebbe una riattivazione ricorsiva
        // del listener. E' dichiarato come array di un elemento poiche' una
        // variabile locale utilizzata all'interno di una lambda deve essere
        // effettivamente final, mentre il contenuto dell'array resta modificabile.
        final boolean[] aggiornamentoInterno = {false};

        combo.getEditor().textProperty().addListener((oss, vecchio, testo) -> {
            if (aggiornamentoInterno[0] || testo == null) {
                return;
            }
            // Se il testo coincide con la voce appena selezionata dall'elenco,
            // non e' necessario applicare nuovamente il filtro.
            String selezionata = combo.getSelectionModel().getSelectedItem();
            if (testo.equals(selezionata)) {
                return;
            }
            List<String> filtrate = filtra(tutte, testo.trim().toLowerCase());

            aggiornamentoInterno[0] = true;
            // La selezione va azzerata prima della sostituzione degli elementi:
            // se una voce restasse selezionata, il ComboBox riscriverebbe
            // nell'editor il testo della selezione precedente, rendendo il campo
            // apparentemente non modificabile.
            combo.getSelectionModel().clearSelection();
            combo.setItems(FXCollections.observableArrayList(filtrate));
            // Ripristino del testo digitato dall'utente, con cursore in coda.
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
     * Restituisce le citta' che contengono il testo cercato, senza
     * distinzione tra maiuscole e minuscole, in numero non superiore a
     * {@value #MAX_SUGGERIMENTI}: data la dimensione del dataset, un
     * elenco non limitato risulterebbe inutilizzabile. Con filtro vuoto
     * vengono restituite le prime voci dell'elenco.
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
     * Restituisce il testo attualmente presente nel campo, sia esso
     * digitato dall'utente o selezionato dall'elenco. I controller
     * leggono il valore tramite questo metodo e non dalla proprieta'
     * {@code value} del ComboBox.
     *
     * @param combo menu a tendina configurato con {@link #applicaCitta}
     * @return testo del campo, senza spazi iniziali e finali
     */
    public static String testo(ComboBox<String> combo) {
        String testo = combo.getEditor().getText();
        return testo == null ? "" : testo.trim();
    }
}
