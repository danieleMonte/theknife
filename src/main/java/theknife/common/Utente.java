/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Un utente registrato a TheKnife. Sul campo password va fatta
 * attenzione: nel database c'e' solo l'hash BCrypt, e prima di spedire
 * l'oggetto al client il campo viene svuotato del tutto — la password,
 * nemmeno cifrata, non deve mai viaggiare verso il client.
 *
 * @author Daniele Montefiore
 */
public class Utente implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String nome;
    private String cognome;
    private String username;
    private String password;      // hash BCrypt (solo lato server)
    private LocalDate dataNascita; // facoltativa, puo' essere null
    private String domicilio;
    private Ruolo ruolo;

    /** Costruttore vuoto. */
    public Utente() { }

    /**
     * Costruttore completo.
     *
     * @param id          identificativo dell'utente
     * @param nome        nome proprio
     * @param cognome     cognome
     * @param username    username o e-mail (univoco)
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
