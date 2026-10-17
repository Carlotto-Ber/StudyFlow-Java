package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.core.Validadores;
import br.com.studyflow.erros.NaoEncontradoException;
import br.com.studyflow.erros.ValidacaoException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Disciplina {
    private Long id;
    private final long idInstituicao;
    private String nome;
    private String descricao;
    private Long idProfessor;
    private int creditos;

    public Disciplina(long idInstituicao, String nome, String descricao, Long idProfessor, int creditos, Long id) {
        this.id = id;
        this.idInstituicao = idInstituicao;
        this.nome = nome;
        this.descricao = descricao;
        this.idProfessor = idProfessor;
        this.creditos = creditos;
    }

    /** Lê um id opcional do JSON: vazio ou nulo significa "nenhum". */
    public static Long idOpcional(Object valor, String campo) {
        return valor == null || valor.toString().isBlank() ? null : Validadores.converterInteiro(valor, campo);
    }

    public static Long professorDe(Object valor) {
        return idOpcional(valor, "professor");
    }

    public static int creditosDe(Object valor) {
        return (int) Validadores.validarNumero(valor, "créditos", 1, 30);
    }

    private void validar() {
        nome = Validadores.exigirTexto(nome, "nome", 150, true);
        descricao = Validadores.exigirTexto(descricao, "descrição", 2000, false);
        if (idProfessor != null) {
            Map<String, Object> professor = DatabaseConnection.getInstance()
                    .consultarUm("SELECT id_instituicao FROM professores WHERE id_professor = ?", idProfessor);
            if (professor == null) {
                throw new NaoEncontradoException("Professor não encontrado.");
            }
            if (Util.longValor(professor.get("id_instituicao")) != idInstituicao) {
                throw new ValidacaoException("O professor pertence a outra instituição.");
            }
        }
    }

    public Disciplina salvar() {
        validar();
        this.id = DatabaseConnection.getInstance().executar(
                "INSERT INTO disciplinas (id_instituicao, id_professor, nome, descricao, creditos) VALUES (?, ?, ?, ?, ?)",
                idInstituicao, idProfessor, nome, descricao, creditos);
        return this;
    }

    public Disciplina atualizar(Map<String, Object> dados) {
        if (dados.containsKey("nome")) {
            nome = Util.texto(dados.get("nome"));
        }
        if (dados.containsKey("descricao")) {
            descricao = Util.texto(dados.get("descricao"));
        }
        if (dados.containsKey("id_professor")) {
            idProfessor = professorDe(dados.get("id_professor"));
        }
        if (dados.containsKey("creditos")) {
            creditos = creditosDe(dados.get("creditos"));
        }
        validar();
        DatabaseConnection.getInstance().executar(
                "UPDATE disciplinas SET nome = ?, descricao = ?, id_professor = ?, creditos = ? WHERE id_disciplina = ?",
                nome, descricao, idProfessor, creditos, id);
        return this;
    }

    public void excluir() {
        DatabaseConnection.getInstance().executar("DELETE FROM disciplinas WHERE id_disciplina = ?", id);
    }

    public void matricular(long idEstudante) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Map<String, Object> estudante = db.consultarUm(
                "SELECT id_instituicao FROM estudantes WHERE id_estudante = ?", idEstudante);
        if (estudante == null) {
            throw new NaoEncontradoException("Estudante não encontrado.");
        }
        if (Util.longValor(estudante.get("id_instituicao")) != idInstituicao) {
            throw new ValidacaoException("O estudante pertence a outra instituição.");
        }
        Map<String, Object> jaMatriculado = db.consultarUm(
                "SELECT 1 AS ok FROM matriculas WHERE id_estudante = ? AND id_disciplina = ?", idEstudante, id);
        if (jaMatriculado == null) {
            db.executar("INSERT INTO matriculas (id_estudante, id_disciplina) VALUES (?, ?)", idEstudante, id);
        }
    }

    public void desmatricular(long idEstudante) {
        DatabaseConnection.getInstance().executar(
                "DELETE FROM matriculas WHERE id_estudante = ? AND id_disciplina = ?", idEstudante, id);
    }

    /** Quem recebe notificações e eventos de calendário desta disciplina. */
    public List<Map<String, Object>> estudantesMatriculados() {
        return DatabaseConnection.getInstance().consultar("""
                SELECT e.id_estudante, e.nome, e.email
                  FROM matriculas m
                  JOIN estudantes e ON e.id_estudante = m.id_estudante
                 WHERE m.id_disciplina = ?
                 ORDER BY e.nome""", id);
    }

    /** Todos os estudantes da instituição, indicando quais estão matriculados nesta disciplina. */
    public List<Map<String, Object>> situacaoDasMatriculas() {
        List<Map<String, Object>> saida = new ArrayList<>();
        for (Map<String, Object> l : DatabaseConnection.getInstance().consultar("""
                SELECT e.id_estudante AS id, e.nome, e.matricula,
                       (SELECT COUNT(*) FROM matriculas m
                         WHERE m.id_disciplina = ? AND m.id_estudante = e.id_estudante) AS matriculado
                  FROM estudantes e
                 WHERE e.id_instituicao = ?
                 ORDER BY e.nome""", id, idInstituicao)) {
            Map<String, Object> item = new LinkedHashMap<>(l);
            item.put("matriculado", Util.longValor(l.get("matriculado")) > 0);
            saida.add(item);
        }
        return saida;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", id);
        dados.put("id_instituicao", idInstituicao);
        dados.put("id_professor", idProfessor);
        dados.put("nome", nome);
        dados.put("descricao", descricao);
        dados.put("creditos", creditos);
        return dados;
    }

    public Long getId() {
        return id;
    }

    public long getIdInstituicao() {
        return idInstituicao;
    }

    public String getNome() {
        return nome;
    }

    public static Disciplina buscar(long idDisciplina) {
        Map<String, Object> l = DatabaseConnection.getInstance()
                .consultarUm("SELECT * FROM disciplinas WHERE id_disciplina = ?", idDisciplina);
        if (l == null) {
            throw new NaoEncontradoException("Disciplina não encontrada.");
        }
        return new Disciplina(Util.longValor(l.get("id_instituicao")), (String) l.get("nome"),
                (String) l.get("descricao"), Util.longOuNulo(l.get("id_professor")),
                (int) Util.longValor(l.get("creditos")), Util.longValor(l.get("id_disciplina")));
    }

    /** Lista com professor, quantidade de estudantes e de tarefas. Com idEstudante, só as matriculadas. */
    public static List<Map<String, Object>> listarDetalhado(long idInstituicao, Long idEstudante) {
        String sql = """
                SELECT d.id_disciplina AS id, d.id_instituicao, d.id_professor, d.nome, d.descricao, d.creditos,
                       p.nome AS professor_nome,
                       (SELECT COUNT(*) FROM matriculas m WHERE m.id_disciplina = d.id_disciplina) AS total_estudantes,
                       (SELECT COUNT(*) FROM tarefas t WHERE t.id_disciplina = d.id_disciplina) AS total_tarefas
                  FROM disciplinas d
                  LEFT JOIN professores p ON p.id_professor = d.id_professor
                 WHERE d.id_instituicao = ?""";
        if (idEstudante == null) {
            return DatabaseConnection.getInstance().consultar(sql + " ORDER BY d.nome", idInstituicao);
        }
        return DatabaseConnection.getInstance().consultar(
                sql + " AND d.id_disciplina IN (SELECT id_disciplina FROM matriculas WHERE id_estudante = ?) ORDER BY d.nome",
                idInstituicao, idEstudante);
    }
}
