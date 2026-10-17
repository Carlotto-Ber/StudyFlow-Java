package br.com.studyflow.observer;

import br.com.studyflow.core.Util;
import br.com.studyflow.model.Disciplina;
import br.com.studyflow.model.EventoCalendario;
import br.com.studyflow.model.Publicacao;

import java.time.LocalDate;
import java.util.Map;

/**
 * Padrão OBSERVER #2
 * Mantém o calendário dos estudantes sincronizado com as publicações que têm data
 * (tarefas e provas): quando são criadas ou a data muda, o evento correspondente
 * é criado ou atualizado.
 */
public class CalendarioObserver implements Observer {
    @Override
    public void atualizar(Publicacao publicacao, Evento evento) {
        LocalDate data = publicacao.dataDoEvento();
        if (data == null) {
            return; // avisos não têm data de evento
        }
        Disciplina disciplina = Disciplina.buscar(publicacao.getIdDisciplina());
        for (Map<String, Object> estudante : disciplina.estudantesMatriculados()) {
            EventoCalendario.sincronizar(Util.longValor(estudante.get("id_estudante")), publicacao.tipo(),
                    publicacao.getId(), publicacao.tituloNoCalendario(), data);
        }
    }
}
