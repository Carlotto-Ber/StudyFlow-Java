package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.core.Validadores;
import br.com.studyflow.erros.ModuloIndisponivelException;
import br.com.studyflow.erros.NaoEncontradoException;
import br.com.studyflow.erros.ValidacaoException;
import br.com.studyflow.strategy.CalculoMedia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Concentra a variabilidade: cada instituição tem seus módulos, canais, termos, cores
 * e método de média. É ela que decide se um aviso pode ser criado e por quais canais notificar.
 */
public class Instituicao {
    /** Módulos comuns a todas as instituições (não ficam na tabela instituicao_modulos). */
    public static final List<String> MODULOS_NUCLEO =
            List.of("dashboard", "disciplinas", "tarefas", "calendario", "arquitetura");
    /** Módulos que a própria instituição pode ligar e desligar na tela de Personalização. */
    public static final List<String> MODULOS_EDITAVEIS = List.of("sessoes", "desempenho", "provas", "avisos");
    public static final List<String> CANAIS_ORDEM = List.of("sistema", "email", "app", "sms");
    private static final List<String> METODOS = List.of("nenhuma", "aritmetica", "ponderada", "conceito");
    private static final Map<String, String> NOMES_CANAIS =
            Map.of("sistema", "Sistema", "email", "e-mail", "app", "aplicativo", "sms", "SMS");

    private final long id;
    private final String codigo;
    private String nome;
    private String cor;
    private String corEscura;
    private String termoDisciplina;
    private String termoProva;
    private String termoAluno;
    private String metodoMedia;
    private final List<String> modulos;
    private final List<String> canais;

    private Instituicao(Map<String, Object> l, List<String> modulos, List<String> canais) {
        this.id = Util.longValor(l.get("id_instituicao"));
        this.codigo = (String) l.get("codigo");
        this.nome = (String) l.get("nome");
        this.cor = (String) l.get("cor");
        this.corEscura = (String) l.get("cor_escura");
        this.termoDisciplina = (String) l.get("termo_disciplina");
        this.termoProva = (String) l.get("termo_prova");
        this.termoAluno = (String) l.get("termo_aluno");
        this.metodoMedia = (String) l.get("metodo_media");
        this.modulos = modulos;
        this.canais = canais.stream().sorted((a, b) -> CANAIS_ORDEM.indexOf(a) - CANAIS_ORDEM.indexOf(b)).toList();
    }

    public boolean possuiModulo(String modulo) {
        return MODULOS_NUCLEO.contains(modulo) || modulos.contains(modulo);
    }

    public void exigirModulo(String modulo) {
        if (!possuiModulo(modulo)) {
            throw new ModuloIndisponivelException(nome + " não possui o módulo '" + modulo + "'.");
        }
    }

    public List<String> canaisAtivos() {
        return new ArrayList<>(canais);
    }

    public long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getMetodoMedia() {
        return metodoMedia;
    }

    public CalculoMedia calculoMedia() {
        return CalculoMedia.paraMetodo(metodoMedia);
    }

    /** Texto para a tela: "Somente dentro do sistema", "Sistema e e-mail", "Sistema, e-mail e SMS"... */
    public String rotuloLembretes() {
        if (canais.size() <= 1) {
            return "Somente dentro do sistema";
        }
        List<String> nomes = canais.stream().map(NOMES_CANAIS::get).toList();
        if (nomes.size() == 2) {
            return nomes.get(0) + " e " + nomes.get(1);
        }
        return String.join(", ", nomes.subList(0, nomes.size() - 1)) + " e " + nomes.get(nomes.size() - 1);
    }

    public Map<String, Object> toMap() {
        List<String> todos = new ArrayList<>(MODULOS_NUCLEO);
        todos.addAll(modulos);
        Map<String, Object> termos = new LinkedHashMap<>();
        termos.put("disciplina", termoDisciplina);
        termos.put("prova", termoProva);
        termos.put("aluno", termoAluno);

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", id);
        dados.put("codigo", codigo);
        dados.put("nome", nome);
        dados.put("cor", cor);
        dados.put("cor_escura", corEscura);
        dados.put("termos", termos);
        dados.put("metodo_media", metodoMedia);
        dados.put("media", calculoMedia().descricao());
        dados.put("lembretes", rotuloLembretes());
        dados.put("modulos", todos);
        dados.put("editaveis", MODULOS_EDITAVEIS);
        dados.put("canais", canais);
        return dados;
    }

    // ------------------------------------------------------------------ tela de Personalização
    @SuppressWarnings("unchecked")
    public void atualizarConfiguracao(Map<String, Object> dados) {
        String novoNome = dados.containsKey("nome") ? Validadores.exigirTexto(dados.get("nome"), "nome", 150, true) : nome;
        String novaCor = dados.containsKey("cor") ? Validadores.validarCor(dados.get("cor")) : cor;
        String novaCorEscura = dados.containsKey("cor") ? escurecer(novaCor) : corEscura;

        String disciplina = termoDisciplina;
        String prova = termoProva;
        String aluno = termoAluno;
        if (dados.get("termos") instanceof Map<?, ?> t) {
            Map<String, Object> termos = (Map<String, Object>) t;
            disciplina = termos.containsKey("disciplina") ? Validadores.exigirTexto(termos.get("disciplina"), "termo de disciplina", 40, true) : disciplina;
            prova = termos.containsKey("prova") ? Validadores.exigirTexto(termos.get("prova"), "termo de prova", 40, true) : prova;
            aluno = termos.containsKey("aluno") ? Validadores.exigirTexto(termos.get("aluno"), "termo de aluno", 40, true) : aluno;
        }

        String novoMetodo = metodoMedia;
        if (dados.containsKey("metodo_media")) {
            novoMetodo = String.valueOf(dados.get("metodo_media"));
            if (!METODOS.contains(novoMetodo)) {
                throw new ValidacaoException("Método de média inválido.");
            }
        }

        List<String> novosModulos = null;
        if (dados.get("modulos") instanceof List<?> lista) {
            novosModulos = new ArrayList<>();
            for (Object m : lista) {
                if (!MODULOS_EDITAVEIS.contains(String.valueOf(m))) {
                    throw new ValidacaoException("Módulo inválido ou que não pode ser alterado: " + m);
                }
                novosModulos.add(String.valueOf(m));
            }
        }

        List<String> novosCanais = null;
        if (dados.get("canais") instanceof List<?> lista) {
            novosCanais = new ArrayList<>();
            for (Object c : lista) {
                if (!CANAIS_ORDEM.contains(String.valueOf(c))) {
                    throw new ValidacaoException("Canal inválido: " + c);
                }
                novosCanais.add(String.valueOf(c));
            }
            if (!novosCanais.contains("sistema")) {
                throw new ValidacaoException("O canal 'sistema' não pode ser desativado.");
            }
        }

        DatabaseConnection db = DatabaseConnection.getInstance();
        db.executar("""
                UPDATE instituicoes SET nome = ?, cor = ?, cor_escura = ?, termo_disciplina = ?,
                       termo_prova = ?, termo_aluno = ?, metodo_media = ?
                 WHERE id_instituicao = ?""",
                novoNome, novaCor, novaCorEscura, disciplina, prova, aluno, novoMetodo, id);

        if (novosModulos != null) {
            String marcadores = String.join(", ", MODULOS_EDITAVEIS.stream().map(m -> "?").toList());
            List<Object> params = new ArrayList<>();
            params.add(id);
            params.addAll(MODULOS_EDITAVEIS);
            db.executar("DELETE FROM instituicao_modulos WHERE id_instituicao = ? AND modulo IN (" + marcadores + ")",
                    params.toArray());
            for (String modulo : novosModulos) {
                db.executar("INSERT INTO instituicao_modulos (id_instituicao, modulo) VALUES (?, ?)", id, modulo);
            }
        }
        if (novosCanais != null) {
            db.executar("DELETE FROM instituicao_canais WHERE id_instituicao = ?", id);
            for (String canal : novosCanais) {
                db.executar("INSERT INTO instituicao_canais (id_instituicao, canal) VALUES (?, ?)", id, canal);
            }
        }
    }

    /** Escurece uma cor  */
    static String escurecer(String cor) {
        int r = (int) (Integer.parseInt(cor.substring(1, 3), 16) * 0.75);
        int g = (int) (Integer.parseInt(cor.substring(3, 5), 16) * 0.75);
        int b = (int) (Integer.parseInt(cor.substring(5, 7), 16) * 0.75);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    // ------------------------------------------------------------------ consultas
    public static Instituicao buscar(long idInstituicao) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        Map<String, Object> linha = db.consultarUm("SELECT * FROM instituicoes WHERE id_instituicao = ?", idInstituicao);
        if (linha == null) {
            throw new NaoEncontradoException("Instituição não encontrada.");
        }
        List<String> modulos = db.consultar(
                "SELECT modulo FROM instituicao_modulos WHERE id_instituicao = ? ORDER BY modulo", idInstituicao)
                .stream().map(l -> (String) l.get("modulo")).toList();
        List<String> canais = db.consultar(
                "SELECT canal FROM instituicao_canais WHERE id_instituicao = ?", idInstituicao)
                .stream().map(l -> (String) l.get("canal")).toList();
        return new Instituicao(linha, modulos, canais);
    }

    public static List<Instituicao> listar() {
        List<Instituicao> lista = new ArrayList<>();
        for (Map<String, Object> linha : DatabaseConnection.getInstance()
                .consultar("SELECT id_instituicao FROM instituicoes ORDER BY id_instituicao")) {
            lista.add(buscar(Util.longValor(linha.get("id_instituicao"))));
        }
        return lista;
    }

    public static boolean existeCodigo(String codigo) {
        return DatabaseConnection.getInstance()
                .consultarUm("SELECT 1 AS ok FROM instituicoes WHERE codigo = ?", codigo) != null;
    }
}
