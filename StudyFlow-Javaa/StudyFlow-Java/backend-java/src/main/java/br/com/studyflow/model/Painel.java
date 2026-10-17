package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Números do painel inicial. Módulos que a instituição não tem aparecem como nulo. */
public final class Painel {
    private Painel() {
    }

    private static long contar(String sql, Object... parametros) {
        return Util.longValor(DatabaseConnection.getInstance().consultarUm(sql, parametros).get("c"));
    }

    public static Map<String, Object> resumo(Instituicao instituicao, Long idEstudante) {
        long id = instituicao.getId();
        Map<String, Object> r = new LinkedHashMap<>();
        if (idEstudante != null) {
            r.put("disciplinas", contar("""
                    SELECT COUNT(*) AS c FROM matriculas m JOIN disciplinas d ON d.id_disciplina = m.id_disciplina
                     WHERE m.id_estudante = ? AND d.id_instituicao = ?""", idEstudante, id));
            r.put("tarefas", contar("""
                    SELECT COUNT(*) AS c FROM tarefas t JOIN matriculas m ON m.id_disciplina = t.id_disciplina
                     WHERE m.id_estudante = ?""", idEstudante));
            r.put("provas", instituicao.possuiModulo("provas") ? contar("""
                    SELECT COUNT(*) AS c FROM provas p JOIN matriculas m ON m.id_disciplina = p.id_disciplina
                     WHERE m.id_estudante = ?""", idEstudante) : null);
            r.put("avisos", instituicao.possuiModulo("avisos") ? contar("""
                    SELECT COUNT(*) AS c FROM avisos a JOIN matriculas m ON m.id_disciplina = a.id_disciplina
                     WHERE m.id_estudante = ?""", idEstudante) : null);
        } else {
            r.put("disciplinas", contar("SELECT COUNT(*) AS c FROM disciplinas WHERE id_instituicao = ?", id));
            r.put("tarefas", contar("""
                    SELECT COUNT(*) AS c FROM tarefas t JOIN disciplinas d ON d.id_disciplina = t.id_disciplina
                     WHERE d.id_instituicao = ?""", id));
            r.put("provas", instituicao.possuiModulo("provas") ? contar("""
                    SELECT COUNT(*) AS c FROM provas p JOIN disciplinas d ON d.id_disciplina = p.id_disciplina
                     WHERE d.id_instituicao = ?""", id) : null);
            r.put("avisos", instituicao.possuiModulo("avisos") ? contar("""
                    SELECT COUNT(*) AS c FROM avisos a JOIN disciplinas d ON d.id_disciplina = a.id_disciplina
                     WHERE d.id_instituicao = ?""", id) : null);
        }
        r.put("estudantes", contar("SELECT COUNT(*) AS c FROM estudantes WHERE id_instituicao = ?", id));
        r.put("professores", contar("SELECT COUNT(*) AS c FROM professores WHERE id_instituicao = ?", id));
        r.put("proximos", Calendario.proximos(id, idEstudante, LocalDate.now(), 5));
        return r;
    }
}
