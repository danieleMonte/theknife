/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import theknife.client.ClientConnection;
import theknife.client.Navigazione;
import theknife.client.Sessione;
import theknife.common.Operazione;
import theknife.common.Recensione;
import theknife.common.Request;
import theknife.common.Response;
import theknife.common.Ristorante;
import theknife.common.Ruolo;

import java.util.List;
import java.util.Optional;

/**
 * Controller della finestra di dettaglio di un ristorante: ne visualizza
 * le caratteristiche complete, la valutazione media e l'elenco delle
 * recensioni in forma anonima.
 * <p>
 * I comandi relativi ai preferiti e all'inserimento di una recensione
 * sono disponibili unicamente per i clienti autenticati; per gli utenti
 * guest e per i gestori la relativa barra e' nascosta.
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
    @FXML private ListView<Recensione> listaRecensioni;
    @FXML private HBox barraAzioniCliente;
    @FXML private HBox barraAzioniGestore;

    private Ristorante ristorante;

    /** Mostra le azioni pertinenti al ruolo della sessione, nascondendo le altre. */
    @FXML
    private void initialize() {
        Sessione sessione = Sessione.getIstanza();
        boolean cliente = sessione.isLoggato()
                && sessione.getUtente().getRuolo() == Ruolo.CLIENTE;
        barraAzioniCliente.setVisible(cliente);
        barraAzioniCliente.setManaged(cliente);
        // L'invito ad accedere riguarda i soli utenti guest: proporlo a un
        // gestore sarebbe fuorviante, dato che e' gia' autenticato e dispone
        // di azioni proprie. La nota destinata ai gestori viene impostata da
        // aggiornaAzioniGestore(), che conosce lo stato di titolarita' del
        // ristorante e viene invocato solo dopo imposta().
        if (!sessione.isLoggato()) {
            etichettaNota.setText("Accedi come cliente per salvare i preferiti e recensire");
        }
        // La barra del gestore resta nascosta finche' imposta() non verifica che
        // il ristorante visualizzato sia effettivamente privo di gestore.
        barraAzioniGestore.setVisible(false);
        barraAzioniGestore.setManaged(false);

        listaRecensioni.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Recensione recensione, boolean vuota) {
                super.updateItem(recensione, vuota);
                if (vuota || recensione == null) {
                    setText(null);
                    return;
                }
                String testo = "★".repeat(recensione.getStelle()) + " " + recensione.getTesto();
                if (recensione.getRisposta() != null) {
                    testo += "\n   ↳ Risposta del gestore: " + recensione.getRisposta();
                }
                setText(testo);
            }
        });
    }

    /**
     * Imposta il ristorante da visualizzare e ne carica le recensioni. Il
     * metodo e' invocato dal MainController subito dopo l'apertura della
     * finestra, poiche' il caricamento di un file FXML non consente il
     * passaggio diretto di parametri al controller.
     *
     * @param ristorante ristorante selezionato nella lista dei risultati
     */
    public void imposta(Ristorante ristorante) {
        this.ristorante = ristorante;
        etichettaNome.setText(ristorante.getNome());
        etichettaLuogo.setText(ristorante.getIndirizzo() + ", "
                + ristorante.getCitta() + " (" + ristorante.getNazione() + ")");
        etichettaCoordinate.setText(String.format("Coordinate: %.5f, %.5f",
                ristorante.getLatitudine(), ristorante.getLongitudine()));
        etichettaCaratteristiche.setText("Cucina: " + ristorante.getTipoCucina()
                + " | Prezzo medio: " + String.format("%.0f€", ristorante.getPrezzoMedio()));
        etichettaServizi.setText("Delivery: " + (ristorante.isDelivery() ? "si'" : "no")
                + " | Prenotazione online: " + (ristorante.isPrenotazioneOnline() ? "si'" : "no"));
        aggiornaAzioniGestore();
        aggiornaValutazioneERecensioni();
    }

    /**
     * Rende disponibile la presa in gestione del ristorante quando la
     * sessione appartiene a un gestore e il ristorante risulta privo di
     * titolare. L'assenza di gestore e' individuata da un identificativo
     * pari a zero: la colonna {@code id_gestore} vale NULL per i
     * ristoranti importati dal dataset e {@code ResultSet.getInt} converte
     * il valore NULL in zero, che non puo' corrispondere ad alcun utente
     * reale poiche' gli identificativi partono da uno.
     */
    private void aggiornaAzioniGestore() {
        Sessione sessione = Sessione.getIstanza();
        boolean gestore = sessione.isLoggato()
                && sessione.getUtente().getRuolo() == Ruolo.GESTORE;
        boolean rivendicabile = gestore && ristorante.getIdGestore() == 0;
        barraAzioniGestore.setVisible(rivendicabile);
        barraAzioniGestore.setManaged(rivendicabile);
        if (rivendicabile) {
            etichettaNota.setText("Questo ristorante non ha ancora un gestore: "
                    + "puoi prenderlo in carico");
        } else if (gestore) {
            etichettaNota.setText(ristorante.getIdGestore() == sessione.getUtente().getId()
                    ? "Questo ristorante e' gia' tra quelli che gestisci"
                    : "Questo ristorante e' gia' gestito da un altro utente");
        }
    }

    /** Ricarica dal server la valutazione media e l'elenco aggiornato delle recensioni. */
    private void aggiornaValutazioneERecensioni() {
        Response dettaglio = ClientConnection.getIstanza().invia(
                new Request(Operazione.VISUALIZZA_RISTORANTE).con("idRistorante", ristorante.getId()));
        if (dettaglio.isSuccesso()) {
            Ristorante aggiornato = (Ristorante) dettaglio.getDati();
            etichettaValutazione.setText(aggiornato.getNumeroRecensioni() == 0
                    ? "Nessuna recensione presente"
                    : String.format("Valutazione media: %.1f ★ su %d recensioni",
                            aggiornato.getMediaStelle(), aggiornato.getNumeroRecensioni()));
        }

        Response recensioni = ClientConnection.getIstanza().invia(
                new Request(Operazione.VISUALIZZA_RECENSIONI).con("idRistorante", ristorante.getId()));
        if (recensioni.isSuccesso()) {
            @SuppressWarnings("unchecked")
            List<Recensione> elenco = (List<Recensione>) recensioni.getDati();
            listaRecensioni.getItems().setAll(elenco);
        } else {
            Navigazione.mostraErrore(recensioni.getMessaggio());
        }
    }

    /**
     * Assegna il ristorante, previa conferma, al gestore autenticato. Al
     * termine dell'operazione il ristorante compare nella scheda
     * "I miei ristoranti" e la barra di presa in gestione viene nascosta.
     */
    @FXML
    private void onRivendica() {
        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION,
                "Vuoi prendere in gestione \"" + ristorante.getNome() + "\"?\n"
                + "Da questo momento potrai rispondere alle sue recensioni e "
                + "consultarne il riepilogo delle valutazioni.");
        conferma.setHeaderText(null);
        Optional<ButtonType> scelta = conferma.showAndWait();
        if (!scelta.isPresent() || scelta.get() != ButtonType.OK) {
            return;
        }
        Response risposta = ClientConnection.getIstanza().invia(
                new Request(Operazione.RIVENDICA_RISTORANTE).con("idRistorante", ristorante.getId()));
        if (risposta.isSuccesso()) {
            ristorante.setIdGestore(Sessione.getIstanza().getUtente().getId());
            aggiornaAzioniGestore();
            etichettaNota.setText("Ristorante preso in gestione: lo trovi nella scheda "
                    + "\"I miei ristoranti\"");
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    /** Aggiunge il ristorante alla lista dei preferiti del cliente. */
    @FXML
    private void onAggiungiPreferito() {
        Response risposta = ClientConnection.getIstanza().invia(
                new Request(Operazione.AGGIUNGI_PREFERITO).con("idRistorante", ristorante.getId()));
        if (risposta.isSuccesso()) {
            etichettaNota.setText("Aggiunto ai preferiti");
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    /** Rimuove il ristorante dalla lista dei preferiti del cliente. */
    @FXML
    private void onRimuoviPreferito() {
        Response risposta = ClientConnection.getIstanza().invia(
                new Request(Operazione.RIMUOVI_PREFERITO).con("idRistorante", ristorante.getId()));
        if (risposta.isSuccesso()) {
            etichettaNota.setText("Rimosso dai preferiti");
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    /** Apre il dialogo per scrivere una nuova recensione. */
    @FXML
    private void onScriviRecensione() {
        Optional<Recensione> nuova = DialogoRecensione.mostra(
                "Recensione - " + ristorante.getNome(), 5, "");
        if (!nuova.isPresent()) {
            return; // l'utente ha annullato
        }
        Recensione recensione = nuova.get();
        Response risposta = ClientConnection.getIstanza().invia(
                new Request(Operazione.AGGIUNGI_RECENSIONE)
                        .con("idRistorante", ristorante.getId())
                        .con("stelle", recensione.getStelle())
                        .con("testo", recensione.getTesto()));
        if (risposta.isSuccesso()) {
            aggiornaValutazioneERecensioni();
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }
}
