package model;


import java.util.Date;

/**
 * Created by dn on 21/03/17.
 */
public class Cliente extends Persona {

    private int telefono;
    private String codiceCliente;

    public Cliente(String codiceCliente, String nome, String cognome, Date dataDiNascita, int telefono) {
        super(nome, cognome, dataDiNascita);
        this.codiceCliente = codiceCliente;
        this.telefono = telefono;
    }

    public String getCodiceCliente() {
        return codiceCliente;
    }

    public Cliente setCodiceCliente(String codiceCliente) {
        this.codiceCliente = codiceCliente;
        return this;
    }

    public int getTelefono() {
        return telefono;
    }

    public Cliente setTelefono(int telefono) {
        this.telefono = telefono;
        return this;
    }

}
