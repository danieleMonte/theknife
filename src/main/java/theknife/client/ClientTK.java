/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Modulo client della piattaforma TheKnife: applicazione JavaFX
 * che si connette al server e mostra l'interfaccia grafica.
 *
 * @author Daniele Montefiore
 */
public class ClientTK extends Application {

    /**
     * Inizializza la finestra principale caricando la schermata iniziale
     * (login / registrazione / accesso come guest).
     *
     * @param stage finestra principale fornita da JavaFX
     * @throws IOException se il file FXML non puo' essere caricato
     */
    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/theknife/client/login.fxml"));
        stage.setTitle("TheKnife");
        stage.setScene(new Scene(root, 640, 480));
        stage.show();
    }

    /** Chiude la connessione col server quando l'applicazione termina. */
    @Override
    public void stop() {
       ClientConnection.getIstanza().chiudi();
    }

    /**
     * Punto d'ingresso dell'applicazione client.
     *
     * @param args non utilizzati
     */
    public static void main(String[] args) {
        launch(args);
    }
}
