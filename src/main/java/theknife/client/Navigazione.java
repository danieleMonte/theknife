/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Classe di utilita' per la navigazione tra le schermate del client. La
 * sequenza di caricamento di un file FXML, creazione della Scene e
 * assegnazione allo Stage e' identica per tutte le schermate: viene
 * pertanto centralizzata in questa classe, insieme alla visualizzazione
 * dei messaggi informativi e di errore.
 *
 * @author Daniele Montefiore
 */
public final class Navigazione {

    private Navigazione() { }

    /**
     * Sostituisce la scena della finestra indicata con la schermata FXML.
     *
     * @param finestra finestra di cui cambiare la scena
     * @param fxml     nome del file FXML (es. {@code main.fxml})
     * @param titolo   titolo da assegnare alla finestra
     */
    public static void vaiA(Window finestra, String fxml, String titolo) {
        try {
            Parent root = FXMLLoader.load(Navigazione.class.getResource("/theknife/client/" + fxml));
            Stage stage = (Stage) finestra;
            stage.setTitle(titolo);
            stage.setScene(new Scene(root, 900, 620));
            stage.centerOnScreen();
        } catch (IOException e) {
            mostraErrore("Impossibile caricare la schermata: " + e.getMessage());
        }
    }

    /**
     * Apre una schermata FXML come finestra modale e ne restituisce il
     * controller, consentendo al chiamante di trasmettergli i dati
     * necessari (ad esempio il ristorante da visualizzare) subito dopo
     * l'apertura. Il tipo di ritorno e' {@code Object}: il cast al
     * controller specifico e' a carico del chiamante.
     *
     * @param proprietario finestra proprietaria del dialogo
     * @param fxml        nome del file FXML (es. {@code dettaglio.fxml})
     * @param titolo      titolo del dialogo
     * @return controller della schermata caricata, o {@code null} in caso di errore
     */
    public static Object apriDialogo(Window proprietario, String fxml, String titolo) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigazione.class.getResource("/theknife/client/" + fxml));
            Parent root = loader.load();
            Stage dialogo = new Stage();
            dialogo.initModality(Modality.WINDOW_MODAL);
            dialogo.initOwner(proprietario);
            dialogo.setTitle(titolo);
            dialogo.setScene(new Scene(root));
            dialogo.show();
            return loader.getController();
        } catch (IOException e) {
            mostraErrore("Impossibile aprire il dialogo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Mostra un messaggio di errore in un Alert.
     *
     * @param messaggio testo da mostrare all'utente
     */
    public static void mostraErrore(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR, messaggio);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Mostra un messaggio informativo in un Alert.
     *
     * @param messaggio testo da mostrare all'utente
     */
    public static void mostraInfo(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, messaggio);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
