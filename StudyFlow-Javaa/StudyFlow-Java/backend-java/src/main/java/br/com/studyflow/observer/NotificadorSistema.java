package br.com.studyflow.observer;

public class NotificadorSistema extends Notificador {
    @Override
    public String canal() {
        return "sistema";
    }

    @Override
    protected String formatar(String texto) {
        return texto;
    }
}
