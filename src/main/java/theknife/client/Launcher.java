/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

/**
 * Punto d'ingresso del jar eseguibile del client.
 * Esiste come classe separata (che non estende {@code Application})
 * per permettere l'avvio di JavaFX da un jar "shaded" senza module-path.
 *
 * @author Daniele Montefiore
 */
public final class Launcher {

    private Launcher() { }

    /**
     * Avvia l'applicazione JavaFX del client.
     *
     * @param args argomenti passati a {@link ClientTK}
     */
    public static void main(String[] args) {
        ClientTK.main(args);
    }
}
