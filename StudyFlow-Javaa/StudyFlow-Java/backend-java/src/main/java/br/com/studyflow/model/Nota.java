package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.core.Validadores;
import br.com.studyflow.erros.NaoEncontradoException;
import br.com.studyflow.erros.ValidacaoException;
import br.com.studyflow.strategy.CalculoMedia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nota de um estudante em uma prova. O rótulo (número ou conceito) segue a regra da instituição. */
public final class Nota {
    private Nota() {
    }

    /** Notas de um estudante, uma linha por prova já corrigida. */
    public static List<Map<String, Object>> doEstudante(long idEstudante) {
        Map<String, Object> estudante = DatabaseConnection.getInstance()
                .consultarUm("SELECT id_instituicao FROM estudantes WHERE id_estudante = ?", idEstudante);
        if (estudante == null) {
            throw new NaoEncontradoException("Estudante não encontrado.");
        }
        CalculoMedia calculo = Instituicao.buscar(Util.longValor(estudante.get("id_instituicao"))).calculoMedia();
        List<Map<String, Object>> saida = new ArrayList<>();
        for (Map<String, Object> l : DatabaseConnection.getInstance()
                .consultar("SELECT id_prova, nota FROM notas WHERE id_estudante = ?", idEstudante)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id_prova", l.get("id_prova"));
            item.put("nota", Util.doubleValor(l.get("nota")));
            item.put("rotulo", calculo.rotulo(Util.doubleValor(l.get("nota"))));
            saida.add(item);
        }
        return saida;
    }

    /** Estudantes matriculados na disciplina da prova, com a nota lançada (ou nulo). */
    public static List<Map<String, Object>> daProva(long idProva) {
        Map<String, Object> prova = Prova.buscarRegistro(idProva);
        long idInstituicao = Disciplina.buscar(Util.longValor(prova.get("id_disciplina"))).getIdInstituicao();
        CalculoMedia calculo = Instituicao.buscar(idInstituicao).calculoMedia();
        List<Map<String, Object>> saida = new ArrayList<>();
        for (Map<String, Object> l : DatabaseConnection.getInstance().consultar("""
                SELECT e.id_estudante, e.nome, n.nota
                  FROM provas p
                  JOIN matriculas m ON m.id_disciplina = p.id_disciplina
                  JOIN estudantes e ON e.id_estudante = m.id_estudante
                  LEFT JOIN notas n ON n.id_prova = p.id_prova AND n.id_estudante = e.id_estudante
                 WHERE p.id_prova = ?
                 ORDER BY e.nome""", idProva)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id_estudante", l.get("id_estudante"));
            item.put("nome", l.get("nome"));
            Object nota = l.get("nota");
            item.put("nota", nota == null ? null : Util.doubleValor(nota));
            item.put("rotulo", nota == null ? null : calculo.rotulo(Util.doubleValor(nota)));
            saida.add(item);
        }
        return saida;
    }

    /** Grava, altera ou (com nota vazia) remove a nota de um estudante. */
    public static void lancar(long idProva, long idEstudante, Object nota) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Map<String, Object> prova = Prova.buscarRegistro(idProva);
        Map<String, Object> matriculado = db.consultarUm(
                "SELECT 1 AS ok FROM matriculas WHERE id_estudante = ? AND id_disciplina = ?",
                idEstudante, prova.get("id_disciplina"));
        if (matriculado == null) {
            throw new ValidacaoException("O estudante não está matriculado nesta disciplina.");
        }
        if (nota == null || nota.toString().isBlank()) {
            db.executar("DELETE FROM notas WHERE id_prova = ? AND id_estudante = ?", idProva, idEstudante);
            return;
        }
        double valor = Validadores.validarNumero(nota, "nota", 0, 10);
        if (db.consultarUm("SELECT 1 AS ok FROM notas WHERE id_prova = ? AND id_estudante = ?", idProva, idEstudante) != null) {
            db.executar("UPDATE notas SET nota = ? WHERE id_prova = ? AND id_estudante = ?", valor, idProva, idEstudante);
        } else {
            db.executar("INSERT INTO notas (id_prova, id_estudante, nota) VALUES (?, ?, ?)", idProva, idEstudante, valor);
        }
    }
}
