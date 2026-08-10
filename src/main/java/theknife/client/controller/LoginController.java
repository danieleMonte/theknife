/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import theknife.client.AutoCompletamento;
import theknife.client.Navigazione;
import theknife.client.Sessione;


/**
 * Controller della schermata iniziale: login, registrazione
 * o accesso come guest indicando solo un luogo.
 *
 * @author Daniele Montefiore
 */
public class LoginController {

    @FXML private TextField campoUsername;
    @FXML private PasswordField campoPassword;
    @FXML private ComboBox<String> campoLuogo;
    @FXML private Label etichettaErrore;

    /** Configura l'autocompletamento delle citta' sul campo luogo. */
    @FXML
    private void initialize() {
        AutoCompletamento.applicaCitta(campoLuogo);
    }

    /** Autentica l'utente presso il server e apre la schermata principale. */
    @FXML
    private void onLogin(ActionEvent evento) {
        /* creare la richiesta di login al server e gestire la risposta
        */
        etichettaErrore.setText("Login non disponibile: manca il package theknife.common");
    }

    /** Apre la schermata di registrazione di un nuovo utente. */
    @FXML
    private void onRegistrazione(ActionEvent evento) {
        Navigazione.vaiA(finestra(evento), "registrazione.fxml", "TheKnife - Registrazione");
    }

    /** Prosegue come guest usando il luogo indicato. */
    @FXML
    private void onGuest(ActionEvent evento) {
        String luogo = AutoCompletamento.testo(campoLuogo);
        if (luogo.isEmpty()) {
            etichettaErrore.setText("Indica un luogo per continuare come guest");
            return;
        }
        Sessione.getIstanza().accediComeGuest(luogo);
        Navigazione.vaiA(finestra(evento), "main.fxml", "TheKnife");
    }

    /** Finestra da cui proviene l'evento (per il cambio di scena). */
    private javafx.stage.Window finestra(ActionEvent evento) {
        return ((Node) evento.getSource()).getScene().getWindow();
    }
}
