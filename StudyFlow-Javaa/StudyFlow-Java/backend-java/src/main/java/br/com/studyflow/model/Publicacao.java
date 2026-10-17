package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.core.Validadores;
import br.com.studyflow.erros.NaoEncontradoException;
import br.com.studyflow.observer.Evento;
import br.com.studyflow.observer.Observer;
import br.com.studyflow.observer.Subject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe base de Tarefa e Aviso. É o SUBJECT do padrão Observer:
 * ao ser criada ou alterada, avisa os observers anexados pela factory.
 */
public abstract class Publicacao implements Subject {
    private static final Logger LOG = Logger.getLogger("studyflow.observer");

    protected Long id;
    protected final long idDisciplina;
    protected String titulo;
    protected String descricao;
    private final List<Observer> observers = new ArrayList<>();

    protected Publicacao(long idDisciplina, String titulo, String descricao, Long id) {
        this.id = id;
        this.idDisciplina = idDisciplina;
        this.titulo = titulo;
        this.descricao = descricao;
    }

    // --- pontos de variação das subclasses --------------------------------
    public abstract String tabela();

    public abstract String pk();

    public abstract String tipo();

    /** Colunas próprias da subclasse. */
    protected abstract Map<String, Object> camposExtras();

    /** Copia os campos próprios vindos de uma edição. */
    protected abstract void aplicarExtras(Map<String, Object> dados);

    /** Texto usado nas notificações. */
    public abstract String mensagem(Evento evento);

    protected boolean descricaoObrigatoria() {
        return false;
    }

    /** Data que vira evento no calendário dos estudantes; null se a publicação não tem data. */
    public java.time.LocalDate dataDoEvento() {
        return null;
    }

    /** Título mostrado no calendário. */
    public String tituloNoCalendario() {
        return titulo;
    }

    // --- Subject ------------------------------------------------------------
    @Override
    public void anexar(Observer observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void desanexar(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notificar(Evento evento) {
        for (Observer observer : new ArrayList<>(observers)) {
            try {
                observer.atualizar(this, evento);
            } catch (RuntimeException e) { // um canal com falha não pode derrubar os demais
                LOG.log(Level.SEVERE, "Falha no observer " + observer.getClass().getSimpleName(), e);
            }
        }
    }

    // --- comportamento comum -------------------------------------------------
    public void validar() {
        titulo = Validadores.exigirTexto(titulo, "título", 150, true);
        descricao = Validadores.exigirTexto(descricao, "descrição", 5000, descricaoObrigatoria());
    }

    private Map<String, Object> campos() {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("id_disciplina", idDisciplina);
        campos.put("titulo", titulo);
        campos.put("descricao", descricao);
        campos.putAll(camposExtras());
        return campos;
    }

    public Publicacao salvar() {
        validar();
        Map<String, Object> campos = campos();
        String colunas = String.join(", ", campos.keySet());
        String marcadores = String.join(", ", campos.keySet().stream().map(c -> "?").toList());
        this.id = DatabaseConnection.getInstance().executar(
                "INSERT INTO " + tabela() + " (" + colunas + ") VALUES (" + marcadores + ")",
                campos.values().toArray());
        notificar(Evento.CRIADO);
        return this;
    }

    public Publicacao atualizar(Map<String, Object> dados) {
        if (dados.containsKey("titulo")) {
            titulo = Util.texto(dados.get("titulo"));
        }
        if (dados.containsKey("descricao")) {
            descricao = Util.texto(dados.get("descricao"));
        }
        aplicarExtras(dados);
        validar();
        Map<String, Object> campos = campos();
        campos.remove("id_disciplina"); // a disciplina não muda numa edição
        String atribuicoes = String.join(", ", campos.keySet().stream().map(c -> c + " = ?").toList());
        List<Object> parametros = new ArrayList<>(campos.values());
        parametros.add(id);
        DatabaseConnection.getInstance().executar(
                "UPDATE " + tabela() + " SET " + atribuicoes + " WHERE " + pk() + " = ?", parametros.toArray());
        notificar(Evento.ALTERADO);
        return this;
    }

    public void excluir() {
        DatabaseConnection.getInstance().executar("DELETE FROM " + tabela() + " WHERE " + pk() + " = ?", id);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", id);
        dados.put("tipo", tipo());
        dados.put("id_disciplina", idDisciplina);
        dados.put("titulo", titulo);
        dados.put("descricao", descricao);
        dados.putAll(camposExtras());
        return Util.serializar(dados);
    }

    public Long getId() {
        return id;
    }

    public long getIdDisciplina() {
        return idDisciplina;
    }

    public String getTitulo() {
        return titulo;
    }

    // --- consultas (usadas pelas subclasses) ------------------------------------
    protected static Map<String, Object> buscarRegistro(String tabela, String pk, String naoEncontrada, long id) {
        Map<String, Object> linha = DatabaseConnection.getInstance()
                .consultarUm("SELECT * FROM " + tabela + " WHERE " + pk + " = ?", id);
        if (linha == null) {
            throw new NaoEncontradoException(naoEncontrada);
        }
        return linha;
    }

    /** Publicações da instituição; com idEstudante, só as das disciplinas em que ele está matriculado. */
    protected static List<Map<String, Object>> listarPorInstituicao(String tabela, String pk, String ordem,
                                                                    long idInstituicao, Long idEstudante) {
        String sql = "SELECT p.*, p." + pk + " AS id, d.nome AS disciplina_nome FROM " + tabela + " p "
                + "JOIN disciplinas d ON d.id_disciplina = p.id_disciplina WHERE d.id_instituicao = ?";
        List<Map<String, Object>> linhas = idEstudante == null
                ? DatabaseConnection.getInstance().consultar(sql + " ORDER BY " + ordem, idInstituicao)
                : DatabaseConnection.getInstance().consultar(sql
                        + " AND p.id_disciplina IN (SELECT id_disciplina FROM matriculas WHERE id_estudante = ?)"
                        + " ORDER BY " + ordem, idInstituicao, idEstudante);
        return linhas.stream().map(Util::serializar).toList();
    }
}
