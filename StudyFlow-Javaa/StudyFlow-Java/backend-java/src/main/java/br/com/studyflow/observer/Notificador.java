package br.com.studyflow.observer;

import br.com.studyflow.core.Util;
import br.com.studyflow.model.Disciplina;
import br.com.studyflow.model.Notificacao;
import br.com.studyflow.model.Publicacao;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Padrão OBSERVER #1
 * Cada canal de lembrete é um observer. A instituição define quais canais existem
 * (tabela instituicao_canais): publicar um aviso no Horizonte gera notificações de
 * sistema e e-mail, e no Atlas também por SMS.
 */
public abstract class Notificador implements Observer {
    private static final Logger LOG = Logger.getLogger("studyflow.notificacoes");

    public abstract String canal();

    /** Adapta o texto às regras do canal. */
    protected abstract String formatar(String texto);

    @Override
    public void atualizar(Publicacao publicacao, Evento evento) {
        Disciplina disciplina = Disciplina.buscar(publicacao.getIdDisciplina());
        String mensagem = formatar(publicacao.mensagem(evento));
        for (Map<String, Object> estudante : disciplina.estudantesMatriculados()) {
            enviar(estudante, mensagem);
            Notificacao.registrar(Util.longValor(estudante.get("id_estudante")), canal(), publicacao.tipo(),
                    publicacao.getId(), evento.valor(), mensagem);
        }
    }

    /** Envio simulado: um sistema real chamaria SMTP, gateway de SMS, push etc. */
    protected void enviar(Map<String, Object> estudante, String mensagem) {
        LOG.info("[" + canal() + "] " + estudante.get("nome") + ": " + mensagem);
    }

    protected static String cortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
