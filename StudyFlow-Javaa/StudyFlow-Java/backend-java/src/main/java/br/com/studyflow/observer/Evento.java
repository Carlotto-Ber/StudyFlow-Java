package br.com.studyflow.observer;

/** Eventos que uma publicação anuncia aos seus observers. */
public enum Evento {
    CRIADO("criado"),
    ALTERADO("alterado");

    private final String valor;

    Evento(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }
}
