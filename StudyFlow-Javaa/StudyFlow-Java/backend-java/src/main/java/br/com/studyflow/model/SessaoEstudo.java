package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.core.Validadores;
import br.com.studyflow.erros.NaoEncontradoException;
import br.com.studyflow.erros.ValidacaoException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sessão de estudo registrada por um estudante (módulo opcional "sessoes"). */
public final class SessaoEstudo {
    private SessaoEstudo() {
    }

    public static long registrar(Map<String, Object> dados) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        long idEstudante = Validadores.converterInteiro(dados.get("id_estudante"), "estudante");
        Map<String, Object> estudante = db.consultarUm(
                "SELECT id_instituicao FROM estudantes WHERE id_estudante = ?", idEstudante);
        if (estudante == null) {
            throw new NaoEncontradoException("Estudante não encontrado.");
        }
        long idInstituicao = Util.longValor(estudante.get("id_instituicao"));
        Instituicao.buscar(idInstituicao).exigirModulo("sessoes");

        String titulo = Validadores.exigirTexto(dados.get("titulo"), "título", 150, true);
        String descricao = Validadores.exigirTexto(dados.get("descricao"), "descrição", 2000, false);
        LocalDateTime inicio = Validadores.validarDataHora(dados.get("data_inicio"), "data de início");
        LocalDateTime fim = Validadores.validarDataHora(dados.get("data_fim"), "data de fim");
        if (fim.isBefore(inicio)) {
            throw new ValidacaoException("O fim da sessão não pode ser anterior ao início.");
        }
        if (Duration.between(inicio, fim).toHours() > 12) {
            throw new ValidacaoException("Uma sessão pode ter no máximo 12 horas.");
        }

        Long idDisciplina = Disciplina.idOpcional(dados.get("id_disciplina"), "disciplina");
        if (idDisciplina != null && Disciplina.buscar(idDisciplina).getIdInstituicao() != idInstituicao) {
            throw new ValidacaoException("A disciplina pertence a outra instituição.");
        }

        long id = db.executar(
                "INSERT INTO sessoes_estudo (id_estudante, titulo, descricao, data_inicio, data_fim) VALUES (?, ?, ?, ?, ?)",
                idEstudante, titulo, descricao, inicio, fim);
        if (idDisciplina != null) {
            db.executar("INSERT INTO sessao_disciplinas (id_sessao, id_disciplina) VALUES (?, ?)", id, idDisciplina);
        }
        return id;
    }

    public static void excluir(long idSessao) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        if (db.consultarUm("SELECT 1 AS ok FROM sessoes_estudo WHERE id_sessao = ?", idSessao) == null) {
            throw new NaoEncontradoException("Sessão não encontrada.");
        }
        db.executar("DELETE FROM sessoes_estudo WHERE id_sessao = ?", idSessao);
    }

    /** Sessões mais recentes da instituição, ou só de um estudante. */
    public static List<Map<String, Object>> listar(long idInstituicao, Long idEstudante, int limite) {
        String sql = """
                SELECT s.id_sessao AS id, s.titulo, s.descricao, s.data_inicio, s.data_fim,
                       e.id_estudante, e.nome AS estudante, d.nome AS disciplina
                  FROM sessoes_estudo s
                  JOIN estudantes e ON e.id_estudante = s.id_estudante
                  LEFT JOIN sessao_disciplinas sd ON sd.id_sessao = s.id_sessao
                  LEFT JOIN disciplinas d ON d.id_disciplina = sd.id_disciplina
                 WHERE e.id_instituicao = ?""";
        List<Map<String, Object>> linhas = idEstudante == null
                ? DatabaseConnection.getInstance().consultar(sql + " ORDER BY s.data_inicio DESC LIMIT ?", idInstituicao, limite)
                : DatabaseConnection.getInstance().consultar(
                        sql + " AND s.id_estudante = ? ORDER BY s.data_inicio DESC LIMIT ?", idInstituicao, idEstudante, limite);
        return linhas.stream().map(l -> {
            Map<String, Object> item = new LinkedHashMap<>(Util.serializar(l));
            item.put("minutos", minutos(l));
            return item;
        }).toList();
    }

    /** Resumo dos últimos 7 dias: minutos estudados, quantidade de sessões e disciplina de maior foco. */
    public static Map<String, Object> resumoSemanal(long idInstituicao, Long idEstudante) {
        LocalDateTime limite = LocalDateTime.now().minusDays(7);
        long minutos = 0;
        int sessoes = 0;
        Map<String, Long> porDisciplina = new LinkedHashMap<>();
        for (Map<String, Object> l : listar(idInstituicao, idEstudante, 500)) {
            if (Validadores.validarDataHora(l.get("data_inicio"), "data").isBefore(limite)) {
                continue;
            }
            long m = (Long) l.get("minutos");
            minutos += m;
            sessoes++;
            if (l.get("disciplina") != null) {
                porDisciplina.merge((String) l.get("disciplina"), m, Long::sum);
            }
        }
        String foco = porDisciplina.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("minutos", minutos);
        resumo.put("sessoes", sessoes);
        resumo.put("maior_foco", foco);
        return resumo;
    }

    private static long minutos(Map<String, Object> linha) {
        if (linha.get("data_fim") == null) {
            return 0;
        }
        return Math.max(0, Duration.between(
                Validadores.validarDataHora(linha.get("data_inicio"), "início"),
                Validadores.validarDataHora(linha.get("data_fim"), "fim")).toMinutes());
    }
}
