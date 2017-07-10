package model;

import java.util.Date;

/**
 * Created by dn on 21/03/17.
 */
public abstract class Persona {
    private String nome, cognome;
    private Date dataDiNascita;

    public Persona() {
    }

    public Persona(String nome, String cognome, Date dataDiNascita) {
        super();
        this.nome = nome;
        this.cognome = cognome;
        this.dataDiNascita = dataDiNascita;
    }

    public String getNome() {
        return nome;
    }

    public Persona setNome(String nome) {
        this.nome = nome;
        return this;
    }

    public String getCognome() {
        return cognome;
    }

    public Persona setCognome(String cognome) {
        this.cognome = cognome;
        return this;
    }

    public Date getDataDiNascita() {
        return dataDiNascita;
    }

    public Persona setDataDiNascita(Date dataDiNascita) {
        this.dataDiNascita = dataDiNascita;
        return this;
    }

    @Override
    public String toString() {
        return "Persona: nome: '" + nome + '\'' + ", cognome: '" + cognome + '\'' + ", data di nascita: " + dataDiNascita;
    }
}
