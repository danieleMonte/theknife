/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client.controller;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import theknife.common.Recensione;

import java.util.Optional;

/**
 * Dialogo riutilizzabile per l'inserimento e la modifica di una
 * recensione, composto da uno Spinner per il numero di stelle e da
 * un'area di testo. E' impiegato in due contesti distinti, l'inserimento
 * di una nuova recensione dal dettaglio del ristorante e la modifica
 * dalla schermata "Le mie recensioni", che differiscono unicamente per
 * titolo e valori iniziali.
 *
 * @author Daniele Montefiore
 */
public final class DialogoRecensione {

    private DialogoRecensione() { }

    /**
     * Mostra il dialogo e attende la conferma dell'utente.
     *
     * @param titolo        titolo del dialogo
     * @param stelleIniziali valore iniziale delle stelle (1-5)
     * @param testoIniziale  testo iniziale della recensione (puo' essere vuoto)
     * @return recensione con stelle e testo compilati, o {@link Optional#empty()}
     *         se l'utente annulla
     */
    public static Optional<Recensione> mostra(String titolo, int stelleIniziali, String testoIniziale) {
        Dialog<Recensione> dialogo = new Dialog<>();
        dialogo.setTitle(titolo);
        dialogo.setHeaderText(null);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Spinner<Integer> spinnerStelle = new Spinner<>(1, 5, stelleIniziali);
        TextArea areaTesto = new TextArea(testoIniziale != null ? testoIniziale : "");
        areaTesto.setPromptText("Scrivi qui la tua recensione...");
        areaTesto.setPrefRowCount(5);
        areaTesto.setWrapText(true);

        VBox contenuto = new VBox(8,
                new Label("Stelle (da 1 a 5):"), spinnerStelle,
                new Label("Testo della recensione:"), areaTesto);
        contenuto.setPadding(new Insets(10));
        dialogo.getDialogPane().setContent(contenuto);

        dialogo.setResultConverter(bottone -> {
            if (bottone == ButtonType.OK) {
                Recensione recensione = new Recensione();
                recensione.setStelle(spinnerStelle.getValue());
                recensione.setTesto(areaTesto.getText().trim());
                return recensione;
            }
            return null;
        });
        return dialogo.showAndWait();
    }
}
