package br.com.studyflow.factory;

import br.com.studyflow.core.Util;
import br.com.studyflow.model.Aviso;

import java.util.List;
import java.util.Map;

public class AvisoFactory extends PublicacaoFactory {
    @Override
    protected Aviso criarPublicacao(Map<String, Object> dados, Long id) {
        return new Aviso(Util.longValor(dados.get("id_disciplina")), Util.texto(dados.get("titulo")),
                Util.texto(dados.get("descricao")), id);
    }

    @Override
    protected String moduloExigido() {
        return "avisos";
    }

    @Override
    public List<Map<String, Object>> listar(long idInstituicao, Long idEstudante) {
        return Aviso.listar(idInstituicao, idEstudante);
    }

    @Override
    protected Map<String, Object> buscarRegistro(long id) {
        return Aviso.buscarRegistro(id);
    }
}
