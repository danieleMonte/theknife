/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.client;

/**
 * Punto di ingresso del jar eseguibile del client. La classe si limita a
 * delegare a {@link ClientTK}, ma la sua presenza e' necessaria: se la
 * main class del jar estendesse direttamente {@code Application}, il
 * launcher di Java richiederebbe la presenza dei moduli JavaFX sul
 * module-path, interrompendo l'avvio con l'errore "JavaFX runtime
 * components are missing". L'interposizione di una classe ordinaria
 * elude tale controllo e consente l'esecuzione del jar con il comando
 * {@code java -jar}.
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
