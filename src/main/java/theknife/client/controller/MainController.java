/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import theknife.client.AutoCompletamento;
import theknife.client.Navigazione;
import theknife.client.Sessione;


/**
 * Controller della schermata principale. Mostra i tab in base al ruolo
 * della sessione: ricerca ristoranti (tutti), preferiti e recensioni
 * personali (clienti), riepilogo dei propri ristoranti con risposta
 * alle recensioni (gestori).
 * All'apertura viene mostrato l'elenco dei ristoranti vicini al luogo
 * indicato dal guest o al domicilio dell'utente loggato.
 *
 * @author Daniele Montefiore
 */
public class MainController {

    private static final String STELLE_INDIFFERENTE = "Indifferente";

    @FXML private Label etichettaUtente;
    @FXML private TabPane pannelloTab;
    @FXML private Tab tabRistoranti;
    @FXML private Tab tabPreferiti;
    @FXML private Tab tabMieRecensioni;
    @FXML private Tab tabMieiRistoranti;

    // Tab ristoranti
    @FXML private ComboBox<String> campoCitta;
    @FXML private TextField campoDistanzaMin;
    @FXML private TextField campoDistanzaMax;
    @FXML private TextField campoCucina;
    @FXML private TextField campoPrezzoMin;
    @FXML private TextField campoPrezzoMax;
    @FXML private CheckBox filtroDelivery;
    @FXML private CheckBox filtroPrenotazione;
    @FXML private ChoiceBox<String> sceltaStelleMin;
    @FXML private Label etichettaRisultati;
    // [PROVVISORIO: le ListView tipizzate Ristorante/Recensione
    @FXML private ListView<Object> listaRistoranti;

    // Tab cliente
    @FXML private ListView<Object> listaPreferiti;
    @FXML private ListView<Object> listaMieRecensioni;

    // Tab gestore
    @FXML private ListView<Object> listaMieiRistoranti;
    @FXML private ListView<Object> listaRecensioniGestore;

    /** Configura i tab per il ruolo corrente e carica i ristoranti vicini. */
    @FXML
    private void initialize() {
        Sessione sessione = Sessione.getIstanza();
        /* da creare la gestione dei tab in base al ruolo della sessione
        */
        etichettaUtente.setText("Ospite - " + sessione.getLuogo()); // [PROVVISORIO]
        pannelloTab.getTabs().removeAll(tabPreferiti, tabMieRecensioni, tabMieiRistoranti); // [PROVVISORIO]

        sceltaStelleMin.getItems().add(STELLE_INDIFFERENTE);
        for (int stelle = 1; stelle <= 5; stelle++) {
            sceltaStelleMin.getItems().add(String.valueOf(stelle));
        }
        sceltaStelleMin.setValue(STELLE_INDIFFERENTE);

        /* da creare la gestione delle celle delle liste in base al ruolo della sessione
        */

        // Elenco iniziale: ristoranti vicini al luogo del guest o al domicilio
        // (stessa ricerca del pulsante Cerca, con la fascia di distanza predefinita)
        AutoCompletamento.applicaCitta(campoCitta);
        campoCitta.getEditor().setText(sessione.getLuogo());
        onCerca();
    }

    // ------------------------------------------------------------------
    // Tab "Ristoranti": ricerca
    // ------------------------------------------------------------------

    /** Esegue la ricerca dei ristoranti con i criteri compilati. */
    @FXML
    private void onCerca() {
        String citta = AutoCompletamento.testo(campoCitta);
        if (citta.isEmpty()) {
            etichettaRisultati.setText("La citta' e' obbligatoria per la ricerca");
            return;
        }
        /* risposta dal server con l'elenco dei ristoranti che soddisfano i criteri di ricerca
        */
        listaRistoranti.getItems().clear();
        etichettaRisultati.setText("Ricerca non disponibile: manca il package theknife.common (e il server)");
    }

    // ------------------------------------------------------------------
    // Tab "Preferiti" (cliente)
    // ------------------------------------------------------------------

    /* da creare la richiesta al server per aggiornare la lista dei preferiti del cliente
    */

    /** Rimuove il ristorante selezionato dalla lista dei preferiti. */
    @FXML
    private void onRimuoviPreferito() {
        /* risposta dal server per rimuovere il ristorante dai preferiti del cliente
        */
        Navigazione.mostraInfo("Funzione non disponibile: manca il package theknife.common");
    }

    // ------------------------------------------------------------------
    // Tab "Le mie recensioni" (cliente)
    // ------------------------------------------------------------------

    /* da creare la richiesta al server per aggiornare la lista delle recensioni del cliente
    */

    /** Modifica la recensione selezionata (stelle e testo). */
    @FXML
    private void onModificaRecensione() {
        /* risposta dal server per modificare la recensione del cliente
        */
        Navigazione.mostraInfo("Funzione non disponibile: manca il package theknife.common");
    }

    /** Elimina la recensione selezionata, dopo conferma. */
    @FXML
    private void onEliminaRecensione() {
        /* risposta dal server per eliminare la recensione del cliente
        */
        Navigazione.mostraInfo("Funzione non disponibile: manca il package theknife.common");
    }

    // ------------------------------------------------------------------
    // Tab "I miei ristoranti" (gestore)
    // ------------------------------------------------------------------

    /* da creare la richiesta al server per aggiornare la lista dei ristoranti del gestore
    */

    /** Apre il modulo di inserimento di un nuovo ristorante. */
    @FXML
    private void onAggiungiRistorante() {
        /* risposta dal server per aprire il dialogo di inserimento del ristorante
        */
        Navigazione.mostraInfo("Funzione non disponibile: manca il package theknife.common");
    }

    /** Risponde alla recensione selezionata (al massimo una risposta). */
    @FXML
    private void onRispondiRecensione() {
        /* risposta dal server per aprire il dialogo di risposta alla recensione
        */
        Navigazione.mostraInfo("Funzione non disponibile: manca il package theknife.common");
    }

    // ------------------------------------------------------------------
    // Uscita e utilita'
    // ------------------------------------------------------------------

    /** Chiude la sessione (anche lato server) e torna al login. */
    @FXML
    private void onEsci(ActionEvent evento) {
        /* risposta dal server per chiudere la sessione e tornare al login
        */
        Sessione.getIstanza().esci();
        Navigazione.vaiA(((Node) evento.getSource()).getScene().getWindow(),
                "login.fxml", "TheKnife");
    }

    /* da creare la gestione della selezione di un ristorante per aprire il dettaglio
    */
}
