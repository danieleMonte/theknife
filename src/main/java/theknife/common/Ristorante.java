/*
 * Daniele Montefiore, Matricola: 736906, Sede: VA
 */
package theknife.common;

import java.io.Serializable;

/**
 * Ristorante della piattaforma: dati anagrafici, posizione geografica,
 * fascia di prezzo, servizi disponibili e tipo di cucina.
 * <p>
 * Media delle stelle, numero di recensioni e distanza non corrispondono a
 * colonne della tabella: sono calcolati dal server a ogni interrogazione
 * e valorizzati in questi campi al solo scopo di essere trasmessi al
 * client insieme ai restanti dati.
 *
 * @author Daniele Montefiore
 */
public class Ristorante implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String nome;
    private String nazione;
    private String citta;
    private String indirizzo;
    private double latitudine;
    private double longitudine;
    private double prezzoMedio;
    private boolean delivery;
    private boolean prenotazioneOnline;
    private String tipoCucina;
    private int idGestore;

    // Statistiche calcolate dal server tramite funzioni di aggregazione
    private double mediaStelle;
    private int numeroRecensioni;

    // Distanza dal punto di riferimento della ricerca; tipo Double anziche'
    // double per poter assumere valore null quando non e' stata calcolata
    private Double distanzaKm;

    /** Costruttore vuoto. */
    public Ristorante() { }

    /** @return identificativo del ristorante */
    public int getId() { return id; }

    /** @param id identificativo del ristorante */
    public void setId(int id) { this.id = id; }

    /** @return nome del ristorante */
    public String getNome() { return nome; }

    /** @param nome nome del ristorante */
    public void setNome(String nome) { this.nome = nome; }

    /** @return nazione in cui si trova il ristorante */
    public String getNazione() { return nazione; }

    /** @param nazione nazione in cui si trova il ristorante */
    public void setNazione(String nazione) { this.nazione = nazione; }

    /** @return citta' in cui si trova il ristorante */
    public String getCitta() { return citta; }

    /** @param citta citta' in cui si trova il ristorante */
    public void setCitta(String citta) { this.citta = citta; }

    /** @return indirizzo del ristorante */
    public String getIndirizzo() { return indirizzo; }

    /** @param indirizzo indirizzo del ristorante */
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }

    /** @return latitudine */
    public double getLatitudine() { return latitudine; }

    /** @param latitudine latitudine */
    public void setLatitudine(double latitudine) { this.latitudine = latitudine; }

    /** @return longitudine */
    public double getLongitudine() { return longitudine; }

    /** @param longitudine longitudine */
    public void setLongitudine(double longitudine) { this.longitudine = longitudine; }

    /** @return prezzo medio in euro */
    public double getPrezzoMedio() { return prezzoMedio; }

    /** @param prezzoMedio prezzo medio in euro */
    public void setPrezzoMedio(double prezzoMedio) { this.prezzoMedio = prezzoMedio; }

    /** @return {@code true} se il servizio di delivery e' disponibile */
    public boolean isDelivery() { return delivery; }

    /** @param delivery disponibilita' del servizio di delivery */
    public void setDelivery(boolean delivery) { this.delivery = delivery; }

    /** @return {@code true} se la prenotazione online e' disponibile */
    public boolean isPrenotazioneOnline() { return prenotazioneOnline; }

    /** @param prenotazioneOnline disponibilita' della prenotazione online */
    public void setPrenotazioneOnline(boolean prenotazioneOnline) { this.prenotazioneOnline = prenotazioneOnline; }

    /** @return tipo di cucina */
    public String getTipoCucina() { return tipoCucina; }

    /** @param tipoCucina tipo di cucina */
    public void setTipoCucina(String tipoCucina) { this.tipoCucina = tipoCucina; }

    /** @return identificativo del gestore proprietario */
    public int getIdGestore() { return idGestore; }

    /** @param idGestore identificativo del gestore proprietario */
    public void setIdGestore(int idGestore) { this.idGestore = idGestore; }

    /** @return media delle stelle delle recensioni ricevute */
    public double getMediaStelle() { return mediaStelle; }

    /** @param mediaStelle media delle stelle delle recensioni ricevute */
    public void setMediaStelle(double mediaStelle) { this.mediaStelle = mediaStelle; }

    /** @return numero di recensioni ricevute */
    public int getNumeroRecensioni() { return numeroRecensioni; }

    /** @param numeroRecensioni numero di recensioni ricevute */
    public void setNumeroRecensioni(int numeroRecensioni) { this.numeroRecensioni = numeroRecensioni; }

    /** @return distanza in km dal luogo di riferimento, {@code null} se non calcolata */
    public Double getDistanzaKm() { return distanzaKm; }

    /** @param distanzaKm distanza in km dal luogo di riferimento */
    public void setDistanzaKm(Double distanzaKm) { this.distanzaKm = distanzaKm; }

    @Override
    public String toString() {
        return nome + " - " + citta + " (" + tipoCucina + ", " + prezzoMedio + "€)";
    }
}
