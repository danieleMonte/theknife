/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import theknife.client.ClientConnection;
import theknife.client.Navigazione;
import theknife.client.Sessione;


import java.util.List;
import java.util.Optional;

/**
 * Controller del dettaglio di un ristorante: caratteristiche complete,
 * valutazione media con numero di recensioni, elenco delle recensioni
 * in forma anonima e, per i clienti loggati, gestione dei preferiti
 * e inserimento di una recensione.
 *
 * @author Daniele Montefiore
 */
public class DettaglioController {

    @FXML private Label etichettaNome;
    @FXML private Label etichettaLuogo;
    @FXML private Label etichettaCoordinate;
    @FXML private Label etichettaCaratteristiche;
    @FXML private Label etichettaServizi;
    @FXML private Label etichettaValutazione;
    @FXML private Label etichettaNota;
    @FXML private ListView<Object> listaRecensioni; // [PROVVISORIO: era ListView<Recensione>, manca theknife.common]
    @FXML private HBox barraAzioniCliente;

    

    /** Nasconde le azioni da cliente se la sessione non lo consente. */
    @FXML
    private void initialize() {
        /* Controllo della sessione */
        boolean cliente = false; // [PROVVISORIO: senza theknife.common si naviga solo come guest]
        barraAzioniCliente.setVisible(cliente);
        barraAzioniCliente.setManaged(cliente);
        if (!cliente) {
            etichettaNota.setText("Accedi come cliente per salvare i preferiti e recensire");
        }

        /*
        modifica la cella della lista delle recensioni per mostrare le stelle e il testo
        */
    }

    /**
     * Imposta il ristorante da mostrare e carica le sue recensioni.
     * Va chiamato subito dopo l'apertura della schermata.
     *
     * @param ristorante ristorante selezionato nella lista dei risultati
     */
    /* mostrazione del ristorante e delle recension
    */

    /* richiesta al server per aggiornare la valutazione media e le recensioni del ristorante 
    */

    /** Aggiunge il ristorante alla lista dei preferiti del cliente. */
    @FXML
    private void onAggiungiPreferito() {
        /* risposta dal server per aggiungere il ristorante ai preferiti del cliente
        */
        Navigazione.mostraInfo("Funzione non disponibile: manca il package theknife.common");
    }

    /** Rimuove il ristorante dalla lista dei preferiti del cliente. */
    @FXML
    private void onRimuoviPreferito() {
        /* risposta dal server per rimuovere il ristorante dai preferiti del cliente
        */
        Navigazione.mostraInfo("Funzione non disponibile: manca il package theknife.common");
    }

    /** Apre il dialogo per scrivere una nuova recensione. */
    @FXML
    private void onScriviRecensione() {
        /* risposta dal server per aprire il dialogo di scrittura della recensione
        */
        Navigazione.mostraInfo("Funzione non disponibile: manca il package theknife.common");
    }
}
