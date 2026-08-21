/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Utente registrato alla piattaforma TheKnife.
 * <p>
 * Il campo password contiene esclusivamente l'hash BCrypt memorizzato nel
 * database e viene azzerato prima dell'invio dell'oggetto al client: la
 * password, in nessuna forma, transita verso il client.
 *
 * @author Daniele Montefiore
 */
public class Utente implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String nome;
    private String cognome;
    private String username;
    private String password;       // hash BCrypt, valorizzato solo lato server
    private LocalDate dataNascita; // facoltativa: puo' essere null
    private String domicilio;
    private Ruolo ruolo;

    /**
     * Verifica il formato di un indirizzo e-mail. Il controllo, volutamente
     * permissivo, richiede la presenza di una parte locale, del carattere
     * {@code @}, di un dominio e di un suffisso di almeno due lettere: e'
     * sufficiente a intercettare gli errori di digitazione senza rifiutare
     * indirizzi validi ma inusuali.
     * <p>
     * Il metodo risiede in questa classe, condivisa da client e server,
     * affinche' la regola sia definita una sola volta e applicata in modo
     * identico dalla validazione dell'interfaccia grafica e da quella
     * effettuata lato server.
     *
     * @param email indirizzo da verificare
     * @return {@code true} se l'indirizzo ha un formato accettabile
     */
    public static boolean emailValida(String email) {
        return email != null && email.matches("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)*\\.[A-Za-z]{2,}$");
    }

    /** Costruttore vuoto. */
    public Utente() { }

    /**
     * Costruttore completo.
     *
     * @param id          identificativo dell'utente
     * @param nome        nome proprio
     * @param cognome     cognome
     * @param username    username / e-mail (univoco)
     * @param password    hash BCrypt della password
     * @param dataNascita data di nascita (puo' essere {@code null})
     * @param domicilio   luogo del domicilio
     * @param ruolo       ruolo dell'utente (cliente o gestore)
     */
    public Utente(int id, String nome, String cognome, String username, String password,
                  LocalDate dataNascita, String domicilio, Ruolo ruolo) {
        this.id = id;
        this.nome = nome;
        this.cognome = cognome;
        this.username = username;
        this.password = password;
        this.dataNascita = dataNascita;
        this.domicilio = domicilio;
        this.ruolo = ruolo;
    }

    /** @return identificativo dell'utente */
    public int getId() { return id; }

    /** @param id identificativo dell'utente */
    public void setId(int id) { this.id = id; }

    /** @return nome proprio */
    public String getNome() { return nome; }

    /** @param nome nome proprio */
    public void setNome(String nome) { this.nome = nome; }

    /** @return cognome */
    public String getCognome() { return cognome; }

    /** @param cognome cognome */
    public void setCognome(String cognome) { this.cognome = cognome; }

    /** @return username o e-mail */
    public String getUsername() { return username; }

    /** @param username username o e-mail */
    public void setUsername(String username) { this.username = username; }

    /** @return hash BCrypt della password (o {@code null} lato client) */
    public String getPassword() { return password; }

    /** @param password hash BCrypt della password */
    public void setPassword(String password) { this.password = password; }

    /** @return data di nascita, {@code null} se non indicata */
    public LocalDate getDataNascita() { return dataNascita; }

    /** @param dataNascita data di nascita */
    public void setDataNascita(LocalDate dataNascita) { this.dataNascita = dataNascita; }

    /** @return luogo del domicilio */
    public String getDomicilio() { return domicilio; }

    /** @param domicilio luogo del domicilio */
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    /** @return ruolo dell'utente */
    public Ruolo getRuolo() { return ruolo; }

    /** @param ruolo ruolo dell'utente */
    public void setRuolo(Ruolo ruolo) { this.ruolo = ruolo; }

    @Override
    public String toString() {
        return nome + " " + cognome + " (" + username + ", " + ruolo + ")";
    }
}
