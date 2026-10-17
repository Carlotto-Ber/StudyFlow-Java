package br.com.studyflow.observer;

import java.util.Map;
import java.util.function.Supplier;

/** Relaciona o nome do canal (como está no banco) ao observer correspondente. */
public final class Notificadores {
    private static final Map<String, Supplier<Notificador>> CANAIS = Map.of(
            "sistema", NotificadorSistema::new,
            "email", NotificadorEmail::new,
            "app", NotificadorApp::new,
            "sms", NotificadorSMS::new);

    private Notificadores() {
    }

    public static Notificador paraCanal(String canal) {
        Supplier<Notificador> criador = CANAIS.get(canal);
        if (criador == null) {
            throw new IllegalStateException("Canal desconhecido: " + canal);
        }
        return criador.get();
    }
}
