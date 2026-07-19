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
import java.util.Optional;

/**
 * Dialogo riutilizzabile per l'inserimento o la modifica di una recensione:
 * un numero di stelle da 1 a 5 e un testo, come da specifiche.
 * Usato sia dal dettaglio ristorante (nuova recensione) sia dalla
 * schermata "le mie recensioni" (modifica).
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
    /* da creare la finestra di dialogo per inserire o modificare una recensione 
    */
}
