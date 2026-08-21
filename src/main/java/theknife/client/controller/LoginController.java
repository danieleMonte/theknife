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
import theknife.client.ClientConnection;
import theknife.client.Navigazione;
import theknife.client.Sessione;
import theknife.common.Operazione;
import theknife.common.Request;
import theknife.common.Response;
import theknife.common.Utente;

/**
 * Controller della schermata iniziale, che consente l'autenticazione,
 * l'accesso alla registrazione oppure la prosecuzione come utente guest
 * previa indicazione di un luogo di riferimento.
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

    /** Invia le credenziali al server e, in caso di esito positivo, apre la schermata principale. */
    @FXML
    private void onLogin(ActionEvent evento) {
        Request richiesta = new Request(Operazione.LOGIN)
                .con("username", campoUsername.getText().trim())
                .con("password", campoPassword.getText());
        Response risposta = ClientConnection.getIstanza().invia(richiesta);
        if (risposta.isSuccesso()) {
            Sessione.getIstanza().accedi((Utente) risposta.getDati());
            Navigazione.vaiA(finestra(evento), "main.fxml", "TheKnife");
        } else {
            etichettaErrore.setText(risposta.getMessaggio());
        }
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

    /** Ricava dall'evento la finestra di provenienza, necessaria per il cambio di scena. */
    private javafx.stage.Window finestra(ActionEvent evento) {
        return ((Node) evento.getSource()).getScene().getWindow();
    }
}
