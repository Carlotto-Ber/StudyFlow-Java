package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.core.Validadores;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class Calendario {
    private Calendario() {
    }

    public static List<Map<String, Object>> doMes(long idInstituicao, Long idEstudante, YearMonth mes) {
        return intervalo(idInstituicao, idEstudante, mes.atDay(1), mes.plusMonths(1).atDay(1));
    }

    public static List<Map<String, Object>> proximos(long idInstituicao, Long idEstudante, LocalDate aPartirDe, int limite) {
        return intervalo(idInstituicao, idEstudante, aPartirDe, aPartirDe.plusDays(120)).stream().limit(limite).toList();
    }

    private static Map<String, Object> item(Object data, Object titulo, String tipo, Object disciplina) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("data", Util.paraTexto(data));
        m.put("titulo", titulo);
        m.put("tipo", tipo);
        m.put("disciplina", disciplina);
        return m;
    }

    private static List<Map<String, Object>> intervalo(long idInstituicao, Long idEstudante, LocalDate inicio, LocalDate fim) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Instituicao instituicao = Instituicao.buscar(idInstituicao);
        List<Map<String, Object>> itens = new ArrayList<>();

        if (idEstudante != null) {
            for (Map<String, Object> l : db.consultar("""
                    SELECT data_evento, titulo, tipo FROM eventos_calendario
                     WHERE id_estudante = ? AND data_evento >= ? AND data_evento < ?""", idEstudante, inicio, fim)) {
                itens.add(item(l.get("data_evento"), l.get("titulo"), (String) l.get("tipo"), null));
            }
            if (instituicao.possuiModulo("sessoes")) {
                for (Map<String, Object> l : db.consultar(
                        "SELECT titulo, data_inicio FROM sessoes_estudo WHERE id_estudante = ?", idEstudante)) {
                    LocalDate dia = Validadores.validarDataHora(l.get("data_inicio"), "data").toLocalDate();
                    if (!dia.isBefore(inicio) && dia.isBefore(fim)) {
                        itens.add(item(dia, "Estudo: " + l.get("titulo"), "sessao", null));
                    }
                }
            }
        } else {
            for (Map<String, Object> l : db.consultar("""
                    SELECT t.data_entrega AS data, t.titulo, d.nome AS disciplina
                      FROM tarefas t JOIN disciplinas d ON d.id_disciplina = t.id_disciplina
                     WHERE d.id_instituicao = ? AND t.data_entrega >= ? AND t.data_entrega < ?""",
                    idInstituicao, inicio, fim)) {
                itens.add(item(l.get("data"), "Entrega: " + l.get("titulo"), "tarefa", l.get("disciplina")));
            }
            if (instituicao.possuiModulo("provas")) {
                for (Map<String, Object> l : db.consultar("""
                        SELECT p.data_prova AS data, p.titulo, d.nome AS disciplina
                          FROM provas p JOIN disciplinas d ON d.id_disciplina = p.id_disciplina
                         WHERE d.id_instituicao = ? AND p.data_prova >= ? AND p.data_prova < ?""",
                        idInstituicao, inicio, fim)) {
                    itens.add(item(l.get("data"), "Prova: " + l.get("titulo"), "prova", l.get("disciplina")));
                }
            }
        }
        itens.sort(Comparator.comparing((Map<String, Object> m) -> (String) m.get("data"))
                .thenComparing(m -> (String) m.get("titulo")));
        return itens;
    }
}
