package br.com.studyflow.observer;

import java.util.Map;
import java.util.logging.Logger;

public class NotificadorEmail extends Notificador {
    private static final Logger LOG = Logger.getLogger("studyflow.notificacoes");

    @Override
    public String canal() {
        return "email";
    }

    @Override
    protected String formatar(String texto) {
        return texto + ". Acesse o StudyFlow para ver os detalhes.";
    }

    @Override
    protected void enviar(Map<String, Object> estudante, String mensagem) {
        LOG.info("[email] para " + estudante.get("email") + ": " + mensagem);
    }
}
