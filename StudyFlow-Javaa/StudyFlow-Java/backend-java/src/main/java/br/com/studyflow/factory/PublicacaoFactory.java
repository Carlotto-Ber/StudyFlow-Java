package br.com.studyflow.factory;

import br.com.studyflow.core.Validadores;
import br.com.studyflow.model.Disciplina;
import br.com.studyflow.model.Instituicao;
import br.com.studyflow.model.Publicacao;
import br.com.studyflow.observer.Notificadores;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Padrão FACTORY METHOD #2
 * PublicacaoFactory cria tarefas e avisos. A factory
 * aplica a VARIABILIDADE da instituição:
 *  - recusa criar a publicação se o módulo não foi contratado
 *  - anexa ao objeto os observers dos canais ativos da instituição.
 */
public abstract class PublicacaoFactory {

    /** FACTORY METHOD: devolve a publicação concreta. */
    protected abstract Publicacao criarPublicacao(Map<String, Object> dados, Long id);

    /** Módulo que a instituição precisa ter contratado; null = recurso comum a todas. */
    protected String moduloExigido() {
        return null;
    }

    public abstract List<Map<String, Object>> listar(long idInstituicao, Long idEstudante);

    protected abstract Map<String, Object> buscarRegistro(long id);

    public Publicacao criar(Map<String, Object> dados, Long id) {
        long idDisciplina = Validadores.converterInteiro(dados.get("id_disciplina"), "disciplina");
        Disciplina disciplina = Disciplina.buscar(idDisciplina);
        Instituicao instituicao = Instituicao.buscar(disciplina.getIdInstituicao());

        String modulo = moduloExigido();
        if (modulo != null) {
            instituicao.exigirModulo(modulo);
        }

        Map<String, Object> normalizados = new HashMap<>(dados);
        normalizados.put("id_disciplina", idDisciplina);
        Publicacao publicacao = criarPublicacao(normalizados, id);
        registrarObservers(publicacao, instituicao);
        return publicacao;
    }

    protected void registrarObservers(Publicacao publicacao, Instituicao instituicao) {
        for (String canal : instituicao.canaisAtivos()) {
            publicacao.anexar(Notificadores.paraCanal(canal));
        }
    }

    /** Reconstrói uma publicação já gravada, com seus observers, para editar ou excluir. */
    public Publicacao carregar(long idPublicacao) {
        return criar(buscarRegistro(idPublicacao), idPublicacao);
    }
}
