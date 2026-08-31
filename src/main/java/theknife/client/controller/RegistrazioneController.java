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
import theknife.client.ClientConnection;
import theknife.client.Navigazione;
import theknife.common.Operazione;
import theknife.common.Request;
import theknife.common.Response;
import theknife.common.Ruolo;
import theknife.common.Utente;

/**
 * Controller della schermata di registrazione di un nuovo utente,
 * cliente o gestore. Le verifiche effettuate a questo livello (campi
 * obbligatori, corrispondenza tra password e conferma) hanno finalita'
 * di usabilita': i controlli di validita' effettivi, come l'unicita'
 * dello username, sono eseguiti dal server.
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
    @FXML private ChoiceBox<Ruolo> sceltaRuolo;
    @FXML private Label etichettaErrore;

    /** Popola la scelta del ruolo e l'autocompletamento del domicilio. */
    @FXML
    private void initialize() {
        sceltaRuolo.getItems().setAll(Ruolo.values());
        sceltaRuolo.setValue(Ruolo.CLIENTE);
        AutoCompletamento.applicaCitta(campoDomicilio);
    }

    /** Valida i campi, invia la richiesta di registrazione al server e ritorna alla schermata di login. */
    @FXML
    private void onConferma(ActionEvent evento) {
        String nome = campoNome.getText().trim();
        String cognome = campoCognome.getText().trim();
        String email = campoUsername.getText().trim();
        String password = campoPassword.getText();
        String domicilio = AutoCompletamento.testo(campoDomicilio);

        if (nome.isEmpty() || cognome.isEmpty() || email.isEmpty()
                || password.isEmpty() || domicilio.isEmpty()) {
            etichettaErrore.setText("Compila tutti i campi obbligatori (*)");
            return;
        }
        if (!Utente.emailValida(email)) {
            etichettaErrore.setText("Indirizzo e-mail non valido (es. nome@dominio.it)");
            return;
        }
        if (!Utente.passwordValida(password)) {
            etichettaErrore.setText("La password deve contenere almeno "
                    + Utente.LUNGHEZZA_MIN_PASSWORD + " caratteri, con almeno una lettera e una cifra");
            return;
        }
        if (!password.equals(campoConferma.getText())) {
            etichettaErrore.setText("Le password non coincidono");
            return;
        }

        Request richiesta = new Request(Operazione.REGISTRAZIONE)
                .con("nome", nome)
                .con("cognome", cognome)
                .con("username", email)
                .con("password", password)
                .con("dataNascita", campoDataNascita.getValue()) // puo' essere null
                .con("domicilio", domicilio)
                .con("ruolo", sceltaRuolo.getValue());
        Response risposta = ClientConnection.getIstanza().invia(richiesta);
        if (risposta.isSuccesso()) {
            Navigazione.mostraInfo("Registrazione completata: ora puoi accedere");
            tornaAlLogin(evento);
        } else {
            etichettaErrore.setText(risposta.getMessaggio());
        }
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
