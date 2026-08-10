/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import theknife.client.AutoCompletamento;
import theknife.client.Navigazione;


/**
 * Controller della schermata di registrazione di un nuovo utente
 * (cliente o gestore), con validazione dei campi obbligatori.
 *
 * @author Daniele Montefiore
 */
public class RegistrazioneController {

    @FXML private TextField campoNome;
    @FXML private TextField campoCognome;
    @FXML private TextField campoUsername;
    @FXML private PasswordField campoPassword;
    @FXML private PasswordField campoConferma;
    @FXML private DatePicker campoDataNascita;
    @FXML private ComboBox<String> campoDomicilio;
    @FXML private ChoiceBox<String> sceltaRuolo; // [PROVVISORIO: era ChoiceBox<Ruolo>, manca theknife.common]
    @FXML private Label etichettaErrore;

    /** Popola la scelta del ruolo e l'autocompletamento del domicilio. */
    @FXML
    private void initialize() {
        /* gestione della scelta del ruolo e dell'autocompletamento del domicilio
        */
        sceltaRuolo.getItems().setAll("CLIENTE", "GESTORE"); // [PROVVISORIO]
        sceltaRuolo.setValue("CLIENTE"); // [PROVVISORIO]
        AutoCompletamento.applicaCitta(campoDomicilio);
    }

    /** Valida i campi, invia la registrazione al server e torna al login. */
    @FXML
    private void onConferma(ActionEvent evento) {
        String nome = campoNome.getText().trim();
        String cognome = campoCognome.getText().trim();
        String username = campoUsername.getText().trim();
        String password = campoPassword.getText();
        String domicilio = AutoCompletamento.testo(campoDomicilio);

        if (nome.isEmpty() || cognome.isEmpty() || username.isEmpty()
                || password.isEmpty() || domicilio.isEmpty()) {
            etichettaErrore.setText("Compila tutti i campi obbligatori (*)");
            return;
        }
        if (!password.equals(campoConferma.getText())) {
            etichettaErrore.setText("Le password non coincidono");
            return;
        }

        /* creare la richiesta di registrazione al server e gestire la risposta
        */
        etichettaErrore.setText("Registrazione non disponibile: manca il package theknife.common");
    }

    /** Annulla la registrazione e torna al login. */
    @FXML
    private void onAnnulla(ActionEvent evento) {
        tornaAlLogin(evento);
    }

    private void tornaAlLogin(ActionEvent evento) {
        Navigazione.vaiA(((Node) evento.getSource()).getScene().getWindow(),
                "login.fxml", "TheKnife");
    }
}
