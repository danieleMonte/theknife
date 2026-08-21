/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import theknife.client.AutoCompletamento;
import theknife.client.ClientConnection;
import theknife.client.Navigazione;
import theknife.client.Sessione;
import theknife.common.Operazione;
import theknife.common.Recensione;
import theknife.common.Request;
import theknife.common.Response;
import theknife.common.Ristorante;
import theknife.common.Ruolo;
import theknife.common.Utente;

import java.util.List;
import java.util.Optional;

/**
 * Controller della schermata principale, organizzata a schede.
 * <p>
 * L'impostazione adottata prevede un'unica schermata per tutti i ruoli:
 * il metodo {@code initialize()} rimuove le schede non pertinenti al
 * ruolo corrente. All'utente guest resta disponibile la sola ricerca; al
 * cliente si aggiungono i preferiti e le proprie recensioni; al gestore
 * il riepilogo dei ristoranti inseriti. All'apertura viene eseguita
 * automaticamente una ricerca dei ristoranti vicini al domicilio
 * dell'utente o al luogo indicato dal guest.
 *
 * @author Daniele Montefiore
 */
public class MainController {

    private static final String STELLE_INDIFFERENTE = "Indifferente";

    // Voci del menu "Ordina per": le etichette visualizzate non coincidono con
    // i valori trasmessi al server, definiti come identificatori sintetici
    // (si veda la conversione in onCerca).
    private static final String ORDINE_DISTANZA = "Distanza (piu' vicino)";
    private static final String ORDINE_CUCINA = "Tipo di cucina (A-Z)";
    private static final String ORDINE_PREZZO_ASC = "Prezzo: dal piu' economico";
    private static final String ORDINE_PREZZO_DESC = "Prezzo: dal piu' costoso";
    private static final String ORDINE_VALUTAZIONE = "Valutazione (i piu' apprezzati prima)";

    @FXML private Label etichettaUtente;
    @FXML private TabPane pannelloTab;
    @FXML private Tab tabRistoranti;
    @FXML private Tab tabPreferiti;
    @FXML private Tab tabMieRecensioni;
    @FXML private Tab tabMieiRistoranti;

    // Tab ristoranti
    @FXML private ComboBox<String> campoCitta;
    @FXML private TextField campoDistanzaMax;
    @FXML private TextField campoCucina;
    @FXML private TextField campoPrezzoMin;
    @FXML private TextField campoPrezzoMax;
    @FXML private CheckBox filtroDelivery;
    @FXML private CheckBox filtroPrenotazione;
    @FXML private ChoiceBox<String> sceltaStelleMin;
    @FXML private ChoiceBox<String> sceltaOrdinamento;
    @FXML private Label etichettaRisultati;
    @FXML private ListView<Ristorante> listaRistoranti;

    // Tab cliente
    @FXML private ListView<Ristorante> listaPreferiti;
    @FXML private ListView<Recensione> listaMieRecensioni;

    // Tab gestore
    @FXML private ListView<Ristorante> listaMieiRistoranti;
    @FXML private ListView<Recensione> listaRecensioniGestore;

    /** Configura le schede in base al ruolo corrente e carica l'elenco dei ristoranti vicini. */
    @FXML
    private void initialize() {
        Sessione sessione = Sessione.getIstanza();
        Utente utente = sessione.getUtente();

        if (utente == null) {
            etichettaUtente.setText("Ospite - " + sessione.getLuogo());
            pannelloTab.getTabs().removeAll(tabPreferiti, tabMieRecensioni, tabMieiRistoranti);
        } else if (utente.getRuolo() == Ruolo.CLIENTE) {
            etichettaUtente.setText(utente.getNome() + " " + utente.getCognome() + " (cliente)");
            pannelloTab.getTabs().remove(tabMieiRistoranti);
        } else {
            etichettaUtente.setText(utente.getNome() + " " + utente.getCognome() + " (gestore)");
            pannelloTab.getTabs().removeAll(tabPreferiti, tabMieRecensioni);
        }

        sceltaStelleMin.getItems().add(STELLE_INDIFFERENTE);
        for (int stelle = 1; stelle <= 5; stelle++) {
            sceltaStelleMin.getItems().add(String.valueOf(stelle));
        }
        sceltaStelleMin.setValue(STELLE_INDIFFERENTE);

        sceltaOrdinamento.getItems().addAll(ORDINE_DISTANZA, ORDINE_CUCINA,
                ORDINE_PREZZO_ASC, ORDINE_PREZZO_DESC, ORDINE_VALUTAZIONE);
        sceltaOrdinamento.setValue(ORDINE_DISTANZA);

        listaRistoranti.setCellFactory(lista -> cellaRistorante());
        listaPreferiti.setCellFactory(lista -> cellaRistorante());
        listaMieiRistoranti.setCellFactory(lista -> cellaRistorante());
        listaMieRecensioni.setCellFactory(lista -> cellaRecensione(true));
        listaRecensioniGestore.setCellFactory(lista -> cellaRecensione(false));

        // Il doppio click su un ristorante apre la finestra di dettaglio con le recensioni.
        listaRistoranti.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) apriDettaglio(listaRistoranti.getSelectionModel().getSelectedItem());
        });
        listaPreferiti.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) apriDettaglio(listaPreferiti.getSelectionModel().getSelectedItem());
        });

        // Il contenuto di ciascuna scheda viene ricaricato alla selezione, in modo
        // da presentare dati aggiornati senza dover aggiornare l'intera schermata
        // a ogni operazione.
        tabPreferiti.setOnSelectionChanged(e -> {
            if (tabPreferiti.isSelected()) aggiornaPreferiti();
        });
        tabMieRecensioni.setOnSelectionChanged(e -> {
            if (tabMieRecensioni.isSelected()) aggiornaMieRecensioni();
        });
        tabMieiRistoranti.setOnSelectionChanged(e -> {
            if (tabMieiRistoranti.isSelected()) aggiornaRiepilogoGestore();
        });

        // La selezione di un proprio ristorante determina il caricamento delle
        // relative recensioni.
        listaMieiRistoranti.getSelectionModel().selectedItemProperty().addListener(
                (oss, vecchio, selezionato) -> aggiornaRecensioniGestore(selezionato));

        // Elenco iniziale: il campo citta' viene precompilato con il luogo della
        // sessione e viene riutilizzata la stessa ricerca associata al pulsante
        // Cerca, con il raggio predefinito.
        AutoCompletamento.applicaCitta(campoCitta);
        campoCitta.getEditor().setText(sessione.getLuogo());
        onCerca();
    }

    // ------------------------------------------------------------------
    // Tab "Ristoranti": ricerca
    // ------------------------------------------------------------------

    /** Raccoglie i criteri di ricerca compilati, li inserisce nella Request e la invia al server. */
    @FXML
    private void onCerca() {
        String citta = AutoCompletamento.testo(campoCitta);
        if (citta.isEmpty()) {
            etichettaRisultati.setText("La citta' e' obbligatoria per la ricerca");
            return;
        }
        Request richiesta = new Request(Operazione.CERCA_RISTORANTE).con("citta", citta);

        String cucina = campoCucina.getText().trim();
        if (!cucina.isEmpty()) {
            richiesta.con("tipoCucina", cucina);
        }
        try {
            if (!campoDistanzaMax.getText().isBlank()) {
                richiesta.con("distanzaMax", Double.parseDouble(campoDistanzaMax.getText().trim()));
            }
            if (!campoPrezzoMin.getText().isBlank()) {
                richiesta.con("prezzoMin", Double.parseDouble(campoPrezzoMin.getText().trim()));
            }
            if (!campoPrezzoMax.getText().isBlank()) {
                richiesta.con("prezzoMax", Double.parseDouble(campoPrezzoMax.getText().trim()));
            }
        } catch (NumberFormatException e) {
            etichettaRisultati.setText("Distanze e prezzi devono essere numeri (es. 30 o 45.50)");
            return;
        }
        if (filtroDelivery.isSelected()) {
            richiesta.con("delivery", Boolean.TRUE);
        }
        if (filtroPrenotazione.isSelected()) {
            richiesta.con("prenotazione", Boolean.TRUE);
        }
        if (!STELLE_INDIFFERENTE.equals(sceltaStelleMin.getValue())) {
            richiesta.con("stelleMin", Double.parseDouble(sceltaStelleMin.getValue()));
        }
        String ordinamento = sceltaOrdinamento.getValue();
        if (ORDINE_CUCINA.equals(ordinamento)) {
            richiesta.con("ordinamento", "cucina");
        } else if (ORDINE_PREZZO_ASC.equals(ordinamento)) {
            richiesta.con("ordinamento", "prezzoAsc");
        } else if (ORDINE_PREZZO_DESC.equals(ordinamento)) {
            richiesta.con("ordinamento", "prezzoDesc");
        } else if (ORDINE_VALUTAZIONE.equals(ordinamento)) {
            richiesta.con("ordinamento", "valutazione");
        }
        // ORDINE_DISTANZA non richiede alcun parametro: in sua assenza il server
        // applica l'ordinamento per distanza, che costituisce il comportamento predefinito.

        Response risposta = ClientConnection.getIstanza().invia(richiesta);
        if (risposta.isSuccesso()) {
            @SuppressWarnings("unchecked")
            List<Ristorante> risultati = (List<Ristorante>) risposta.getDati();
            listaRistoranti.getItems().setAll(risultati);
            etichettaRisultati.setText(risultati.size() + " ristoranti vicini a " + citta
                    + " nella fascia di distanza scelta (ordinati dal piu' vicino)");
        } else {
            // In caso di esito negativo la lista viene svuotata: mantenere i
            // risultati precedenti al di sotto di un messaggio di errore
            // risulterebbe fuorviante per l'utente.
            listaRistoranti.getItems().clear();
            etichettaRisultati.setText(risposta.getMessaggio());
        }
    }

    // ------------------------------------------------------------------
    // Tab "Preferiti" (cliente)
    // ------------------------------------------------------------------

    private void aggiornaPreferiti() {
        Response risposta = ClientConnection.getIstanza()
                .invia(new Request(Operazione.VISUALIZZA_PREFERITI));
        if (risposta.isSuccesso()) {
            @SuppressWarnings("unchecked")
            List<Ristorante> preferiti = (List<Ristorante>) risposta.getDati();
            listaPreferiti.getItems().setAll(preferiti);
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    /** Rimuove il ristorante selezionato dalla lista dei preferiti. */
    @FXML
    private void onRimuoviPreferito() {
        Ristorante selezionato = listaPreferiti.getSelectionModel().getSelectedItem();
        if (selezionato == null) {
            Navigazione.mostraInfo("Seleziona prima un ristorante dalla lista");
            return;
        }
        Response risposta = ClientConnection.getIstanza().invia(
                new Request(Operazione.RIMUOVI_PREFERITO).con("idRistorante", selezionato.getId()));
        if (risposta.isSuccesso()) {
            aggiornaPreferiti();
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    // ------------------------------------------------------------------
    // Tab "Le mie recensioni" (cliente)
    // ------------------------------------------------------------------

    private void aggiornaMieRecensioni() {
        Response risposta = ClientConnection.getIstanza()
                .invia(new Request(Operazione.VISUALIZZA_MIE_RECENSIONI));
        if (risposta.isSuccesso()) {
            @SuppressWarnings("unchecked")
            List<Recensione> recensioni = (List<Recensione>) risposta.getDati();
            listaMieRecensioni.getItems().setAll(recensioni);
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    /** Modifica la recensione selezionata (stelle e testo). */
    @FXML
    private void onModificaRecensione() {
        Recensione selezionata = listaMieRecensioni.getSelectionModel().getSelectedItem();
        if (selezionata == null) {
            Navigazione.mostraInfo("Seleziona prima una recensione dalla lista");
            return;
        }
        Optional<Recensione> modificata = DialogoRecensione.mostra(
                "Modifica recensione - " + selezionata.getNomeRistorante(),
                selezionata.getStelle(), selezionata.getTesto());
        if (!modificata.isPresent()) {
            return; // l'utente ha annullato
        }
        Recensione recensione = modificata.get();
        Response risposta = ClientConnection.getIstanza().invia(
                new Request(Operazione.MODIFICA_RECENSIONE)
                        .con("idRecensione", selezionata.getId())
                        .con("stelle", recensione.getStelle())
                        .con("testo", recensione.getTesto()));
        if (risposta.isSuccesso()) {
            aggiornaMieRecensioni();
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    /** Elimina la recensione selezionata, dopo conferma. */
    @FXML
    private void onEliminaRecensione() {
        Recensione selezionata = listaMieRecensioni.getSelectionModel().getSelectedItem();
        if (selezionata == null) {
            Navigazione.mostraInfo("Seleziona prima una recensione dalla lista");
            return;
        }
        Alert conferma = new Alert(Alert.AlertType.CONFIRMATION,
                "Eliminare la recensione di \"" + selezionata.getNomeRistorante() + "\"?");
        conferma.setHeaderText(null);
        Optional<ButtonType> scelta = conferma.showAndWait();
        if (!scelta.isPresent() || scelta.get() != ButtonType.OK) {
            return; // l'utente ha annullato
        }
        Response risposta = ClientConnection.getIstanza().invia(
                new Request(Operazione.ELIMINA_RECENSIONE).con("idRecensione", selezionata.getId()));
        if (risposta.isSuccesso()) {
            aggiornaMieRecensioni();
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    // ------------------------------------------------------------------
    // Tab "I miei ristoranti" (gestore)
    // ------------------------------------------------------------------

    private void aggiornaRiepilogoGestore() {
        Response risposta = ClientConnection.getIstanza()
                .invia(new Request(Operazione.VISUALIZZA_RIEPILOGO));
        if (risposta.isSuccesso()) {
            @SuppressWarnings("unchecked")
            List<Ristorante> ristoranti = (List<Ristorante>) risposta.getDati();
            listaMieiRistoranti.getItems().setAll(ristoranti);
            listaRecensioniGestore.getItems().clear();
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    private void aggiornaRecensioniGestore(Ristorante ristorante) {
        if (ristorante == null) {
            listaRecensioniGestore.getItems().clear();
            return;
        }
        Response risposta = ClientConnection.getIstanza().invia(
                new Request(Operazione.VISUALIZZA_RECENSIONI).con("idRistorante", ristorante.getId()));
        if (risposta.isSuccesso()) {
            @SuppressWarnings("unchecked")
            List<Recensione> recensioni = (List<Recensione>) risposta.getDati();
            listaRecensioniGestore.getItems().setAll(recensioni);
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    /** Apre il modulo di inserimento di un nuovo ristorante, realizzato con un Dialog e un GridPane. */
    @FXML
    private void onAggiungiRistorante() {
        Dialog<Request> dialogo = new Dialog<>();
        dialogo.setTitle("Aggiungi ristorante");
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nome = new TextField();
        TextField nazione = new TextField();
        TextField citta = new TextField();
        TextField indirizzo = new TextField();
        TextField latitudine = new TextField();
        TextField longitudine = new TextField();
        TextField prezzoMedio = new TextField();
        TextField tipoCucina = new TextField();
        CheckBox delivery = new CheckBox("Servizio di delivery disponibile");
        CheckBox prenotazione = new CheckBox("Prenotazione online disponibile");

        GridPane griglia = new GridPane();
        griglia.setHgap(8);
        griglia.setVgap(8);
        griglia.setPadding(new Insets(10));
        String[] etichette = {"Nome *", "Nazione *", "Citta' *", "Indirizzo *",
                "Latitudine", "Longitudine", "Prezzo medio (€) *", "Tipo di cucina *"};
        TextField[] campi = {nome, nazione, citta, indirizzo, latitudine, longitudine, prezzoMedio, tipoCucina};
        for (int riga = 0; riga < campi.length; riga++) {
            griglia.add(new Label(etichette[riga]), 0, riga);
            griglia.add(campi[riga], 1, riga);
        }
        griglia.add(delivery, 1, campi.length);
        griglia.add(prenotazione, 1, campi.length + 1);
        Label suggerimento = new Label("Coordinate: lasciale vuote per usare il centro della\n"
                + "citta' (se gia' presente su TheKnife), oppure prendile da\n"
                + "Google Maps (click destro sul luogo), es. Legnano: 45.60 e 8.92");
        suggerimento.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        griglia.add(suggerimento, 0, campi.length + 2, 2, 1);
        dialogo.getDialogPane().setContent(griglia);

        dialogo.setResultConverter(bottone -> {
            if (bottone != ButtonType.OK) {
                return null;
            }
            try {
                Request nuova = new Request(Operazione.AGGIUNGI_RISTORANTE)
                        .con("nome", nome.getText().trim())
                        .con("nazione", nazione.getText().trim())
                        .con("citta", citta.getText().trim())
                        .con("indirizzo", indirizzo.getText().trim())
                        .con("prezzoMedio", Double.parseDouble(prezzoMedio.getText().trim()))
                        .con("delivery", delivery.isSelected())
                        .con("prenotazione", prenotazione.isSelected())
                        .con("tipoCucina", tipoCucina.getText().trim());
                // Le coordinate sono incluse nella richiesta solo se valorizzate:
                // in loro assenza il server utilizza il baricentro della citta'.
                if (!latitudine.getText().trim().isEmpty()) {
                    nuova.con("latitudine", Double.parseDouble(latitudine.getText().trim()));
                }
                if (!longitudine.getText().trim().isEmpty()) {
                    nuova.con("longitudine", Double.parseDouble(longitudine.getText().trim()));
                }
                return nuova;
            } catch (NumberFormatException e) {
                return null; // segnalato sotto come dati non validi
            }
        });

        Optional<Request> richiesta = dialogo.showAndWait();
        if (!richiesta.isPresent()) {
            return; // annullato o dati numerici non validi
        }
        Response risposta = ClientConnection.getIstanza().invia(richiesta.get());
        if (risposta.isSuccesso()) {
            aggiornaRiepilogoGestore();
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    /** Registra la risposta del gestore alla recensione selezionata; ne e' ammessa una sola, verificata dal server. */
    @FXML
    private void onRispondiRecensione() {
        Recensione selezionata = listaRecensioniGestore.getSelectionModel().getSelectedItem();
        if (selezionata == null) {
            Navigazione.mostraInfo("Seleziona prima una recensione dalla lista");
            return;
        }
        if (selezionata.getRisposta() != null) {
            Navigazione.mostraInfo("Hai gia' risposto a questa recensione");
            return;
        }
        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Rispondi alla recensione");
        dialogo.setHeaderText(null);
        dialogo.setContentText("Testo della risposta:");
        Optional<String> testo = dialogo.showAndWait();
        if (!testo.isPresent() || testo.get().trim().isEmpty()) {
            return; // annullato o risposta vuota
        }
        Response risposta = ClientConnection.getIstanza().invia(
                new Request(Operazione.RISPOSTA_RECENSIONE)
                        .con("idRecensione", selezionata.getId())
                        .con("risposta", testo.get().trim()));
        if (risposta.isSuccesso()) {
            aggiornaRecensioniGestore(listaMieiRistoranti.getSelectionModel().getSelectedItem());
        } else {
            Navigazione.mostraErrore(risposta.getMessaggio());
        }
    }

    // ------------------------------------------------------------------
    // Uscita e utilita'
    // ------------------------------------------------------------------

    /** Termina la sessione, anche lato server, e ritorna alla schermata di login. */
    @FXML
    private void onEsci(ActionEvent evento) {
        if (Sessione.getIstanza().isLoggato()) {
            ClientConnection.getIstanza().invia(new Request(Operazione.LOGOUT));
        }
        Sessione.getIstanza().esci();
        Navigazione.vaiA(((Node) evento.getSource()).getScene().getWindow(),
                "login.fxml", "TheKnife");
    }

    private void apriDettaglio(Ristorante ristorante) {
        if (ristorante == null) {
            return;
        }
        DettaglioController controller = (DettaglioController) Navigazione.apriDialogo(
                pannelloTab.getScene().getWindow(), "dettaglio.fxml", ristorante.getNome());
        if (controller != null) {
            controller.imposta(ristorante);
        }
    }

    /** Definisce il formato di visualizzazione di una riga della lista ristoranti: nome, cucina, prezzo, valutazione e distanza. */
    private ListCell<Ristorante> cellaRistorante() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Ristorante ristorante, boolean vuota) {
                super.updateItem(ristorante, vuota);
                if (vuota || ristorante == null) {
                    setText(null);
                    return;
                }
                String valutazione = ristorante.getNumeroRecensioni() == 0
                        ? "nessuna recensione"
                        : String.format("%.1f ★ (%d recensioni)",
                                ristorante.getMediaStelle(), ristorante.getNumeroRecensioni());
                String distanza = ristorante.getDistanzaKm() == null ? ""
                        : String.format(" | %.1f km", ristorante.getDistanzaKm());
                setText(ristorante.getNome() + " - " + ristorante.getCitta()
                        + " | " + ristorante.getTipoCucina()
                        + " | " + String.format("%.0f€", ristorante.getPrezzoMedio())
                        + " | " + valutazione + distanza);
            }
        };
    }

    /** Definisce il formato di visualizzazione di una riga della lista recensioni; il nome del ristorante e' incluso solo dove necessario. */
    private ListCell<Recensione> cellaRecensione(boolean conNomeRistorante) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Recensione recensione, boolean vuota) {
                super.updateItem(recensione, vuota);
                if (vuota || recensione == null) {
                    setText(null);
                    return;
                }
                StringBuilder testo = new StringBuilder();
                if (conNomeRistorante && recensione.getNomeRistorante() != null) {
                    testo.append(recensione.getNomeRistorante()).append(" - ");
                }
                testo.append("★".repeat(recensione.getStelle()))
                        .append(" ").append(recensione.getTesto());
                if (recensione.getRisposta() != null) {
                    testo.append("\n   ↳ Risposta del gestore: ").append(recensione.getRisposta());
                }
                setText(testo.toString());
            }
        };
    }
}
