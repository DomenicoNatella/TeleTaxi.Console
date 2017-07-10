package model;

import java.util.Date;

/**
 * Created by dn on 26/03/17.
 */
public class Prenotazione {

    private Cliente cliente;
    private OperatoreTelefonico operatoreTelefonico;
    private Taxi taxi;
    private String serviziSpeciali[], progressivo, posizioneCliente, destinazione;
    private double tempoAttesa;
    private Date data;
    private boolean assegnata;

    public Prenotazione(String progressivo, Cliente cliente, OperatoreTelefonico operatoreTelefonico, Taxi taxi, String destinazione, String[] serviziSpeciali,
                        String posizioneCliente, double tempoAttesa, Date data, boolean assegnata) {
        this.cliente = cliente;
        this.operatoreTelefonico = operatoreTelefonico;
        this.taxi = taxi;
        this.serviziSpeciali = serviziSpeciali;
        this.destinazione = destinazione;
        this.progressivo = progressivo;
        this.tempoAttesa = tempoAttesa;
        this.posizioneCliente = posizioneCliente;
        this.data = data;
        this.assegnata = assegnata;
    }

    public boolean isAssegnata() {
        return assegnata;
    }

    public Prenotazione setAssegnata(boolean assegnata) {
        this.assegnata = assegnata;
        return this;
    }

    public String getDestinazione() {
        return destinazione;
    }

    public Prenotazione setDestinazione(String destinazione) {
        this.destinazione = destinazione;
        return this;
    }

    public String getPosizioneCliente() {
        return posizioneCliente;
    }

    public Prenotazione setPosizioneCliente(String posizioneCliente) {
        this.posizioneCliente = posizioneCliente;
        return this;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Prenotazione setCliente(Cliente cliente) {
        this.cliente = cliente;
        return this;
    }

    public OperatoreTelefonico getOperatoreTelefonico() {
        return operatoreTelefonico;
    }

    public Prenotazione setOperatoreTelefonico(OperatoreTelefonico operatoreTelefonico) {
        this.operatoreTelefonico = operatoreTelefonico;
        return this;
    }

    public Taxi getTaxi() {
        return taxi;
    }

    public Prenotazione setTaxi(Taxi taxi) {
        this.taxi = taxi;
        return this;
    }

    public String[] getServiziSpeciali() {
        return serviziSpeciali;
    }

    public Prenotazione setServiziSpeciali(String[] serviziSpeciali) {
        this.serviziSpeciali = serviziSpeciali;
        return this;
    }

    public String getProgressivo() {
        return progressivo;
    }

    public Prenotazione setProgressivo(String progressivo) {
        this.progressivo = progressivo;
        return this;
    }

    public double getTempoAttesa() {
        return tempoAttesa;
    }

    public Prenotazione setTempoAttesa(double tempoAttesa) {
        this.tempoAttesa = tempoAttesa;
        return this;
    }

    public Date getData() {
        return data;
    }

    public Prenotazione setData(Date data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return "Progressivo: "+getProgressivo()+", cliente: "+getCliente().getNome()+" "+getCliente().getCognome();
    }
}
