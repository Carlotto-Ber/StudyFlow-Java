package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;

import java.util.List;
import java.util.Map;

/** Registro gravado pelos observers de canal. */
public final class Notificacao {
    private Notificacao() {
    }

    public static long registrar(long idEstudante, String canal, String tipo, long idReferencia,
                                 String evento, String mensagem) {
        return DatabaseConnection.getInstance().executar("""
                INSERT INTO notificacoes (id_estudante, canal, tipo, id_referencia, evento, mensagem)
                VALUES (?, ?, ?, ?, ?, ?)""", idEstudante, canal, tipo, idReferencia, evento, mensagem);
    }

    /** Últimas notificações da instituição, ou só as de um estudante. */
    public static List<Map<String, Object>> listarRecentes(long idInstituicao, String tipo, int limite, Long idEstudante) {
        String sql = """
                SELECT n.id_notificacao AS id, e.nome AS estudante, n.canal, n.evento,
                       n.mensagem, n.criada_em
                  FROM notificacoes n
                  JOIN estudantes e ON e.id_estudante = n.id_estudante
                 WHERE e.id_instituicao = ? AND n.tipo = ?""";
        List<Map<String, Object>> linhas = idEstudante == null
                ? DatabaseConnection.getInstance().consultar(sql + " ORDER BY n.id_notificacao DESC LIMIT ?",
                        idInstituicao, tipo, limite)
                : DatabaseConnection.getInstance().consultar(sql + " AND n.id_estudante = ? ORDER BY n.id_notificacao DESC LIMIT ?",
                        idInstituicao, tipo, idEstudante, limite);
        return linhas.stream().map(Util::serializar).toList();
    }
}
