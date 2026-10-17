package br.com.studyflow;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Validadores;
import br.com.studyflow.factory.AvisoFactory;
import br.com.studyflow.factory.ProvaFactory;
import br.com.studyflow.factory.TarefaFactory;
import br.com.studyflow.model.Disciplina;
import br.com.studyflow.model.Instituicao;
import br.com.studyflow.model.Laboratorio;
import br.com.studyflow.model.Nota;
import br.com.studyflow.model.Publicacao;
import br.com.studyflow.model.ReservaLaboratorio;
import br.com.studyflow.model.SessaoEstudo;
import br.com.studyflow.model.Usuario;
import br.com.studyflow.service.CadastroService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Seed {
    static final String SENHA = "123456";

    record DisciplinaSeed(String nome, int creditos) {
    }

    /** Mesma variabilidade que antes ficava no JS/config.js: cores, termos, média, módulos e canais. */
    record Spec(String codigo, String nome, String cor, String corEscura,
                String termoDisciplina, String termoProva, String termoAluno, String metodoMedia,
                List<String> modulos, List<String> canais,
                String administrador, String professor, List<String> estudantes) {
    }

    static final List<DisciplinaSeed> DISCIPLINAS = List.of(
            new DisciplinaSeed("Algoritmos", 4), new DisciplinaSeed("Banco de Dados", 6));

    static final List<Spec> INSTITUICOES = List.of(
            new Spec("individual", "StudyFlow Individual", "#4f46c8", "#39319e",
                    "Disciplina", "Prova", "Estudante", "nenhuma",
                    List.of(), List.of("sistema"),
                    "Admin Individual", null, List.of("Lucas Martins")),
            new Spec("horizonte", "Colégio Horizonte", "#1d7a66", "#13594b",
                    "Matéria", "Avaliação", "Aluno", "aritmetica",
                    List.of("provas", "avisos", "usuarios", "personalizacao"), List.of("sistema", "email"),
                    "Carla Lima", "Ana Souza", List.of("Bruno Alves", "Carla Dias")),
            new Spec("aurora", "Universidade Aurora", "#a14b18", "#753510",
                    "Componente curricular", "Prova", "Discente", "ponderada",
                    List.of("sessoes", "desempenho", "provas", "avisos", "personalizacao"),
                    List.of("sistema", "email", "app"),
                    "Paulo Mendes", "Marcos Ribeiro", List.of("Daniela Lopes", "Eduardo Matos")),
            new Spec("atlas", "Instituto Técnico Atlas", "#1769aa", "#104d7d",
                    "Unidade técnica", "Verificação prática", "Aprendiz", "conceito",
                    List.of("sessoes", "desempenho", "provas", "avisos", "usuarios", "personalizacao", "laboratorio"),
                    List.of("sistema", "email", "sms"),
                    "Helena Costa", "Renato Pires", List.of("Fernanda Cruz", "Gabriel Nunes")));

    private static int contador = 0;

    private Seed() {
    }

    static String cpfValido(int numero) {
        String base = String.format("%09d", numero);
        base += Validadores.digitoCpf(base);
        base += Validadores.digitoCpf(base);
        return base;
    }

    private static String emailDe(Spec spec, String nome) {
        String[] partes = nome.toLowerCase().split(" ");
        return partes[0] + "." + partes[partes.length - 1] + "@" + spec.codigo() + ".studyflow.com";
    }

    private static Map<String, Object> dadosUsuario(Spec spec, long idInstituicao, String nome, String tipo) {
        contador++;
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("tipo", tipo);
        dados.put("id_instituicao", idInstituicao);
        dados.put("nome", nome);
        dados.put("email", emailDe(spec, nome));
        dados.put("telefone", "4199" + String.format("%07d", contador));
        dados.put("cpf", cpfValido(123456780 + contador));
        dados.put("senha", SENHA);
        if (tipo.equals("estudante")) {
            dados.put("matricula", spec.codigo().substring(0, 3).toUpperCase() + (2026000 + contador));
        }
        return dados;
    }

    private static Map<String, Object> mapa(Object... pares) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) {
            m.put((String) pares[i], pares[i + 1]);
        }
        return m;
    }

    public static void semear() {
        DatabaseConnection db = DatabaseConnection.getInstance();
        CadastroService cadastro = CadastroService.getInstance();
        LocalDate hoje = LocalDate.now();
        contador = 0;

        for (Spec spec : INSTITUICOES) {
            if (Instituicao.existeCodigo(spec.codigo())) {
                System.out.println("- " + spec.nome() + ": já existe, ignorada");
                continue;
            }

            long idInst = db.executar("""
                    INSERT INTO instituicoes (codigo, nome, cor, cor_escura, termo_disciplina, termo_prova, termo_aluno, metodo_media)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", spec.codigo(), spec.nome(), spec.cor(), spec.corEscura(),
                    spec.termoDisciplina(), spec.termoProva(), spec.termoAluno(), spec.metodoMedia());
            for (String modulo : spec.modulos()) {
                db.executar("INSERT INTO instituicao_modulos (id_instituicao, modulo) VALUES (?, ?)", idInst, modulo);
            }
            for (String canal : spec.canais()) {
                db.executar("INSERT INTO instituicao_canais (id_instituicao, canal) VALUES (?, ?)", idInst, canal);
            }

            // --- usuários (administrador, professor, estudantes)
            cadastro.registrar("administrador", dadosUsuario(spec, idInst, spec.administrador(), "administrador"));
            Long idProfessor = null;
            if (spec.professor() != null) {
                idProfessor = cadastro.registrar("professor", dadosUsuario(spec, idInst, spec.professor(), "professor")).getId();
            }
            List<Usuario> estudantes = new ArrayList<>();
            for (String nome : spec.estudantes()) {
                estudantes.add(cadastro.registrar("estudante", dadosUsuario(spec, idInst, nome, "estudante")));
            }

            // --- disciplinas e matrículas
            List<Disciplina> disciplinas = new ArrayList<>();
            for (DisciplinaSeed d : DISCIPLINAS) {
                Disciplina disciplina = new Disciplina(idInst, d.nome(), null, idProfessor, d.creditos(), null).salvar();
                for (Usuario estudante : estudantes) {
                    disciplina.matricular(estudante.getId());
                }
                disciplinas.add(disciplina);
            }
            Disciplina d0 = disciplinas.get(0);
            Disciplina d1 = disciplinas.get(1);

            // --- tarefas (factory + observers: notificações e eventos de calendário)
            new TarefaFactory().criar(mapa("id_disciplina", d0.getId(), "titulo", "Lista de exercícios",
                    "descricao", "Resolver os exercícios 1 a 10.", "data_entrega", hoje.plusDays(10).toString()), null).salvar();
            new TarefaFactory().criar(mapa("id_disciplina", d1.getId(), "titulo", "Modelagem do banco",
                    "descricao", "Entregar o diagrama entidade-relacionamento.", "data_entrega", hoje.plusDays(17).toString()), null).salvar();

            if (spec.modulos().contains("avisos")) {
                new AvisoFactory().criar(mapa("id_disciplina", d0.getId(), "titulo", "Aula remarcada",
                        "descricao", "A aula da próxima semana será na sala 12."), null).salvar();
            }

            // --- provas e notas (as já realizadas recebem nota)
            if (spec.modulos().contains("provas")) {
                Object[][] provas = {
                        {d0, "Prova 1", -21, 30}, {d0, "Prova 2", -7, 40}, {d0, "Prova final", 14, 30},
                        {d1, "Prova 1", -14, 50}, {d1, "Projeto prático", 21, 50}};
                for (Object[] p : provas) {
                    Disciplina disciplina = (Disciplina) p[0];
                    int dias = (Integer) p[2];
                    Publicacao prova = new ProvaFactory().criar(mapa("id_disciplina", disciplina.getId(), "titulo", p[1],
                            "data_prova", hoje.plusDays(dias).toString(), "peso", p[3]), null).salvar();
                    if (dias < 0) {
                        for (Usuario estudante : estudantes) {
                            double nota = 6.0 + ((estudante.getId() * 7 + prova.getId() * 3) % 40) / 10.0;
                            Nota.lancar(prova.getId(), estudante.getId(), nota);
                        }
                    }
                }
            }

            // --- sessões de estudo
            if (spec.modulos().contains("sessoes")) {
                for (Usuario estudante : estudantes) {
                    int[][] sessoes = {{1, 40, 0}, {2, 25, 1}, {4, 50, 0}};
                    String[] titulos = {"Revisão de listas", "Estudo de modelagem", "Exercícios de lógica"};
                    for (int i = 0; i < sessoes.length; i++) {
                        LocalDateTime inicio = LocalDateTime.now().minusDays(sessoes[i][0])
                                .withHour(19).withMinute(0).withSecond(0).withNano(0);
                        SessaoEstudo.registrar(mapa("id_estudante", estudante.getId(),
                                "id_disciplina", disciplinas.get(sessoes[i][2]).getId(), "titulo", titulos[i],
                                "data_inicio", inicio.toString(), "data_fim", inicio.plusMinutes(sessoes[i][1]).toString()));
                    }
                }
            }

            // --- laboratório inteligente (exclusivo do Atlas)
            if (spec.modulos().contains("laboratorio")) {
                long lab1 = db.executar("INSERT INTO laboratorios (id_instituicao, nome, equipamento, estado) VALUES (?, ?, ?, ?)",
                        idInst, "Automação 01", "CLP e sensores", "disponivel");
                db.executar("INSERT INTO laboratorios (id_instituicao, nome, equipamento, estado) VALUES (?, ?, ?, ?)",
                        idInst, "Eletrônica 02", "Osciloscópios", "manutencao");
                db.executar("INSERT INTO laboratorios (id_instituicao, nome, equipamento, estado) VALUES (?, ?, ?, ?)",
                        idInst, "Redes 03", "Switches e roteadores", "disponivel");
                ReservaLaboratorio.registrar(mapa("id_laboratorio", lab1, "data_reserva", hoje.plusDays(1).toString(),
                        "hora_inicio", "14:00", "hora_fim", "16:00", "finalidade", "Aula prática de CLP",
                        "responsavel", spec.professor()));
            }
            System.out.println("- " + spec.nome() + ": criada");
        }

        System.out.println("\nCadastros feitos nesta execução: " + cadastro.getTotalCadastros());
        System.out.println("Senha dos usuários de exemplo: " + SENHA);
        System.out.println("Administradores de exemplo:");
        for (Spec spec : INSTITUICOES) {
            System.out.println("  " + spec.administrador() + " <" + emailDe(spec, spec.administrador()) + "> - " + spec.nome());
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT %4$s %3$s: %5$s%6$s%n");
        semear();
        DatabaseConnection.getInstance().fechar();
    }
}
