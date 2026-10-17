package br.com.studyflow.model;

import br.com.studyflow.core.Validadores;
import br.com.studyflow.observer.Evento;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Provas, o nome varia por instituição na tela). */
public class Prova extends Publicacao {
    public static final String TABELA = "provas";
    public static final String PK = "id_prova";

    private Object dataProva;
    private Object peso;

    public Prova(long idDisciplina, String titulo, String descricao, Object dataProva, Object peso, Long id) {
        super(idDisciplina, titulo, descricao, id);
        this.dataProva = dataProva;
        this.peso = peso;
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
        return "prova";
    }

    @Override
    public void validar() {
        super.validar();
        dataProva = Validadores.validarData(dataProva, "data da prova");
        peso = Validadores.validarNumero(peso, "peso", 0.01, 100);
    }

    public LocalDate getDataProva() {
        return Validadores.validarData(dataProva, "data da prova");
    }

    public double getPeso() {
        return Validadores.validarNumero(peso, "peso", 0.01, 100);
    }

    @Override
    protected Map<String, Object> camposExtras() {
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("data_prova", getDataProva());
        extras.put("peso", getPeso());
        return extras;
    }

    @Override
    protected void aplicarExtras(Map<String, Object> dados) {
        if (dados.containsKey("data_prova")) {
            dataProva = dados.get("data_prova");
        }
        if (dados.containsKey("peso")) {
            peso = dados.get("peso");
        }
    }

    @Override
    public LocalDate dataDoEvento() {
        return getDataProva();
    }

    @Override
    public String tituloNoCalendario() {
        return "Prova: " + titulo;
    }

    @Override
    public String mensagem(Evento evento) {
        String data = Validadores.formatarDataBr(dataProva);
        if (evento == Evento.CRIADO) {
            return "Nova prova: " + titulo + " (em " + data + ")";
        }
        return "Prova alterada: " + titulo + " (em " + data + ")";
    }

    public static Map<String, Object> buscarRegistro(long id) {
        return buscarRegistro(TABELA, PK, "Prova não encontrada.", id);
    }

    public static List<Map<String, Object>> listar(long idInstituicao, Long idEstudante) {
        return listarPorInstituicao(TABELA, PK, "p.data_prova ASC, p.id_prova ASC", idInstituicao, idEstudante);
    }
}
