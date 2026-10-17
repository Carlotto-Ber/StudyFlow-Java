package br.com.studyflow.model;

import br.com.studyflow.core.Validadores;
import br.com.studyflow.observer.Evento;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Tarefa extends Publicacao {
    public static final String TABELA = "tarefas";
    public static final String PK = "id_tarefa";

    private Object dataEntrega; // texto/data bruta até validar()

    public Tarefa(long idDisciplina, String titulo, String descricao, Object dataEntrega, Long id) {
        super(idDisciplina, titulo, descricao, id);
        this.dataEntrega = dataEntrega;
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
        return "tarefa";
    }

    @Override
    public void validar() {
        super.validar();
        dataEntrega = Validadores.validarData(dataEntrega, "data de entrega");
    }

    public LocalDate getDataEntrega() {
        return Validadores.validarData(dataEntrega, "data de entrega");
    }

    @Override
    protected Map<String, Object> camposExtras() {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("data_entrega", getDataEntrega());
        return extras;
    }

    @Override
    protected void aplicarExtras(Map<String, Object> dados) {
        if (dados.containsKey("data_entrega")) {
            dataEntrega = dados.get("data_entrega");
        }
    }

    @Override
    public LocalDate dataDoEvento() {
        return getDataEntrega();
    }

    @Override
    public String tituloNoCalendario() {
        return "Entrega: " + titulo;
    }

    @Override
    public String mensagem(Evento evento) {
        String prazo = Validadores.formatarDataBr(dataEntrega);
        if (evento == Evento.CRIADO) {
            return "Nova tarefa: " + titulo + " (entrega em " + prazo + ")";
        }
        return "Tarefa alterada: " + titulo + " (entrega em " + prazo + ")";
    }

    public static Map<String, Object> buscarRegistro(long id) {
        return buscarRegistro(TABELA, PK, "Tarefa não encontrada.", id);
    }

    public static List<Map<String, Object>> listar(long idInstituicao, Long idEstudante) {
        return listarPorInstituicao(TABELA, PK, "p.data_entrega ASC, p.id_tarefa ASC", idInstituicao, idEstudante);
    }
}
