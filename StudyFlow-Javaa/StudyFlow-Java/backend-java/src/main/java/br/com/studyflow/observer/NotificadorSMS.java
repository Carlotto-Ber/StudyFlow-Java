package br.com.studyflow.observer;

public class NotificadorSMS extends Notificador {
    @Override
    public String canal() {
        return "sms";
    }

    @Override
    protected String formatar(String texto) {
        return cortar(texto, 80); // SMS tem limite curto
    }
}
