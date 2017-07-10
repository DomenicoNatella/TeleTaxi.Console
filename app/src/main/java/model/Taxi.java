package model;

import java.util.Arrays;

/**
 * Created by dn on 21/03/17.
 */
public class Taxi {
    private int codice;
    private String stato, destinazione, serviziSpeciali[], posizioneCorrente;
    private String progressivoPrenotazione;

    public Taxi(int codice, String stato, String posizioneCorrente, String destinazione, String[] serviziSpeciali, String progressivoPrenotazione) {
        this.codice = codice;
        this.stato = stato;
        this.posizioneCorrente = posizioneCorrente;
        this.destinazione = destinazione;
        this.serviziSpeciali = serviziSpeciali;
        this.progressivoPrenotazione = progressivoPrenotazione;
    }

    public String getPosizioneCorrente() {
        return posizioneCorrente;
    }

    public Taxi setPosizioneCorrente(String posizioneCorrente) {
        this.posizioneCorrente = posizioneCorrente;
        return this;
    }

    public int getCodice() {
        return codice;
    }

    public Taxi setCodice(int codice) {
        this.codice = codice;
        return this;
    }

    public String getStato() {
        return stato;
    }

    public Taxi impostaStato(String stato) {
        this.stato = stato;
        return this;
    }

    public String getDestinazione() {
        return destinazione;
    }

    public Taxi setDestinazione(String destinazione) {
        this.destinazione = destinazione;
        return this;
    }

    public String[] getServiziSpeciali() {
        return serviziSpeciali;
    }

    public Taxi setServiziSpeciali(String[] serviziSpeciali) {
        this.serviziSpeciali = serviziSpeciali;
        return this;
    }

    public String getPrenotazione() {
        return progressivoPrenotazione;
    }

    public Taxi setPrenotazione(String progressivoPrenotazione) {
        this.progressivoPrenotazione = progressivoPrenotazione;
        return this;
    }

    @Override
    public String toString() {
        return "Taxi: " + "codice: " + codice + ", stato: '" + stato + '\'' + ", serviziSpeciali: " + Arrays.toString(serviziSpeciali);
    }
}
