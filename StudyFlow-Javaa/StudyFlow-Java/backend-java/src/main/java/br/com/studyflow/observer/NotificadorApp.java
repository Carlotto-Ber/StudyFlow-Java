package br.com.studyflow.observer;

public class NotificadorApp extends Notificador {
    @Override
    public String canal() {
        return "app";
    }

    @Override
    protected String formatar(String texto) {
        return cortar(texto, 120);
    }
}
