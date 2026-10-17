package br.com.studyflow.factory;

import br.com.studyflow.core.Util;
import br.com.studyflow.model.Instituicao;
import br.com.studyflow.model.Publicacao;
import br.com.studyflow.model.Tarefa;
import br.com.studyflow.observer.CalendarioObserver;

import java.util.List;
import java.util.Map;

public class TarefaFactory extends PublicacaoFactory {
    @Override
    protected Tarefa criarPublicacao(Map<String, Object> dados, Long id) {
        return new Tarefa(Util.longValor(dados.get("id_disciplina")), Util.texto(dados.get("titulo")),
                Util.texto(dados.get("descricao")), dados.get("data_entrega"), id);
    }

    @Override
    protected void registrarObservers(Publicacao publicacao, Instituicao instituicao) {
        super.registrarObservers(publicacao, instituicao);
        publicacao.anexar(new CalendarioObserver()); // tarefas também atualizam o calendário
    }

    @Override
    public List<Map<String, Object>> listar(long idInstituicao, Long idEstudante) {
        return Tarefa.listar(idInstituicao, idEstudante);
    }

    @Override
    protected Map<String, Object> buscarRegistro(long id) {
        return Tarefa.buscarRegistro(id);
    }
}
