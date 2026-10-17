package br.com.studyflow.factory;

import br.com.studyflow.core.Util;
import br.com.studyflow.model.Instituicao;
import br.com.studyflow.model.Prova;
import br.com.studyflow.model.Publicacao;
import br.com.studyflow.observer.CalendarioObserver;

import java.util.List;
import java.util.Map;

public class ProvaFactory extends PublicacaoFactory {
    @Override
    protected Prova criarPublicacao(Map<String, Object> dados, Long id) {
        return new Prova(Util.longValor(dados.get("id_disciplina")), Util.texto(dados.get("titulo")),
                Util.texto(dados.get("descricao")), dados.get("data_prova"), dados.get("peso"), id);
    }

    @Override
    protected String moduloExigido() {
        return "provas";
    }

    @Override
    protected void registrarObservers(Publicacao publicacao, Instituicao instituicao) {
        super.registrarObservers(publicacao, instituicao);
        publicacao.anexar(new CalendarioObserver()); // provas também aparecem no calendário
    }

    @Override
    public List<Map<String, Object>> listar(long idInstituicao, Long idEstudante) {
        return Prova.listar(idInstituicao, idEstudante);
    }

    @Override
    protected Map<String, Object> buscarRegistro(long id) {
        return Prova.buscarRegistro(id);
    }
}
