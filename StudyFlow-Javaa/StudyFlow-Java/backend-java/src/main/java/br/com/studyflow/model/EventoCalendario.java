package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Evento do calendário do estudante, mantido pelo CalendarioObserver. */
public final class EventoCalendario {
    private EventoCalendario() {
    }

    public static void sincronizar(long idEstudante, String tipo, long idOrigem, String titulo, LocalDate dataEvento) {
        String coluna = "prova".equals(tipo) ? "id_prova" : "id_tarefa";
        DatabaseConnection db = DatabaseConnection.getInstance();
        Map<String, Object> existente = db.consultarUm(
                "SELECT id_evento FROM eventos_calendario WHERE id_estudante = ? AND " + coluna + " = ?",
                idEstudante, idOrigem);
        if (existente != null) {
            db.executar("UPDATE eventos_calendario SET titulo = ?, data_evento = ? WHERE id_evento = ?",
                    titulo, dataEvento, existente.get("id_evento"));
        } else {
            db.executar("INSERT INTO eventos_calendario (id_estudante, tipo, " + coluna
                    + ", titulo, data_evento) VALUES (?, ?, ?, ?, ?)", idEstudante, tipo, idOrigem, titulo, dataEvento);
        }
    }

    /** Eventos da instituição; tipo pode ser nulo (todos), "tarefa" ou "prova". */
    public static List<Map<String, Object>> listar(long idInstituicao, String tipo, int limite, Long idEstudante) {
        StringBuilder sql = new StringBuilder("""
                SELECT ev.id_evento AS id, e.nome AS estudante, ev.tipo, ev.titulo, ev.data_evento
                  FROM eventos_calendario ev
                  JOIN estudantes e ON e.id_estudante = ev.id_estudante
                 WHERE e.id_instituicao = ?""");
        List<Object> params = new java.util.ArrayList<>();
        params.add(idInstituicao);
        if (tipo != null) {
            sql.append(" AND ev.tipo = ?");
            params.add(tipo);
        }
        if (idEstudante != null) {
            sql.append(" AND ev.id_estudante = ?");
            params.add(idEstudante);
        }
        sql.append(" ORDER BY ev.data_evento, e.nome LIMIT ?");
        params.add(limite);
        return DatabaseConnection.getInstance().consultar(sql.toString(), params.toArray())
                .stream().map(Util::serializar).toList();
    }
}
