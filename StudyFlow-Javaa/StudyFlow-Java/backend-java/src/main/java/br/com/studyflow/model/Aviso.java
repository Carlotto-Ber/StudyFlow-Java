package br.com.studyflow.model;

import br.com.studyflow.observer.Evento;

import java.util.List;
import java.util.Map;

public class Aviso extends Publicacao {
    public static final String TABELA = "avisos";
    public static final String PK = "id_aviso";

    public Aviso(long idDisciplina, String titulo, String descricao, Long id) {
        super(idDisciplina, titulo, descricao, id);
    }

    @Override
    public String tabela() {
        return TABELA;
    }

    @Override
    public String pk() {
        return PK;
    }

    @Override
    public String tipo() {
        return "aviso";
    }

    @Override
    protected boolean descricaoObrigatoria() {
        return true;
    }

    @Override
    protected Map<String, Object> camposExtras() {
        return Map.of(); // data_publicacao é preenchida pelo próprio banco
    }

    @Override
    protected void aplicarExtras(Map<String, Object> dados) {
        // sem campos próprios
    }

    @Override
    public String mensagem(Evento evento) {
        if (evento == Evento.CRIADO) {
            return "Novo aviso: " + titulo;
        }
        return "Aviso atualizado: " + titulo;
    }

    public static Map<String, Object> buscarRegistro(long id) {
        return buscarRegistro(TABELA, PK, "Aviso não encontrado.", id);
    }

    public static List<Map<String, Object>> listar(long idInstituicao, Long idEstudante) {
        return listarPorInstituicao(TABELA, PK, "p.data_publicacao DESC, p.id_aviso DESC", idInstituicao, idEstudante);
    }
}
