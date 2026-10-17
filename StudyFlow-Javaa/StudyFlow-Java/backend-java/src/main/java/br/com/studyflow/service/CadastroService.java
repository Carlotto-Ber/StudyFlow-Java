package br.com.studyflow.service;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.erros.ConflitoException;
import br.com.studyflow.erros.NaoEncontradoException;
import br.com.studyflow.erros.ValidacaoException;
import br.com.studyflow.factory.AdministradorFactory;
import br.com.studyflow.factory.EstudanteFactory;
import br.com.studyflow.factory.ProfessorFactory;
import br.com.studyflow.factory.UsuarioFactory;
import br.com.studyflow.model.Administrador;
import br.com.studyflow.model.Estudante;
import br.com.studyflow.model.Instituicao;
import br.com.studyflow.model.Professor;
import br.com.studyflow.model.Usuario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Padrão SINGLETON #2
 * Ponto único de cadastro de usuários. Mantém as factories e o contador de cadastros
 * da execução, por isso precisa existir uma só instância.
 */
public final class CadastroService {

    private static volatile CadastroService instancia;

    private final DatabaseConnection db = DatabaseConnection.getInstance();
    private final Map<String, UsuarioFactory> fabricas = new LinkedHashMap<>();
    private final AtomicInteger totalCadastros = new AtomicInteger();

    private CadastroService() {
        fabricas.put("administrador", new AdministradorFactory());
        fabricas.put("professor", new ProfessorFactory());
        fabricas.put("estudante", new EstudanteFactory());
    }

    public static CadastroService getInstance() {
        if (instancia == null) {
            synchronized (CadastroService.class) {
                if (instancia == null) {
                    instancia = new CadastroService();
                }
            }
        }
        return instancia;
    }

    public int getTotalCadastros() {
        return totalCadastros.get();
    }

    public List<String> tiposDisponiveis() {
        return List.copyOf(fabricas.keySet());
    }

    public Usuario registrar(String tipo, Map<String, Object> dados) {
        UsuarioFactory fabrica = tipo == null ? null : fabricas.get(tipo);
        if (fabrica == null) {
            throw new ValidacaoException("Tipo de usuário inválido. Use: " + String.join(", ", fabricas.keySet()) + ".");
        }

        Usuario usuario = fabrica.criarUsuario(dados);      // Factory Method
        Instituicao.buscar(usuario.getIdInstituicao());      // garante que a instituição existe
        garantirUnicidade(usuario);

        Map<String, Object> colunas = usuario.colunas();
        String nomes = String.join(", ", colunas.keySet());
        String marcadores = String.join(", ", colunas.keySet().stream().map(c -> "?").toList());
        usuario.setId(db.executar("INSERT INTO " + usuario.tabela() + " (" + nomes + ") VALUES (" + marcadores + ")",
                colunas.values().toArray()));
        totalCadastros.incrementAndGet();
        return usuario;
    }

    /** E-mail, CPF e telefone UNIQUE */
    private void garantirUnicidade(Usuario usuario) {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("email", usuario.getEmail());
        valores.put("cpf", usuario.getCpf());
        valores.put("telefone", usuario.getTelefone());
        for (Map.Entry<String, String> campo : valores.entrySet()) {
            for (String tabela : List.of(Administrador.TABELA, Professor.TABELA, Estudante.TABELA)) {
                if (db.consultarUm("SELECT 1 AS ok FROM " + tabela + " WHERE " + campo.getKey() + " = ?",
                        campo.getValue()) != null) {
                    throw new ConflitoException("Já existe um usuário com este " + campo.getKey() + ".");
                }
            }
        }
        if (usuario instanceof Estudante estudante
                && db.consultarUm("SELECT 1 AS ok FROM estudantes WHERE matricula = ?", estudante.getMatricula()) != null) {
            throw new ConflitoException("Já existe um estudante com esta matrícula.");
        }
    }

    /** Usuários da instituição (administradores, professores, estudantes) */
    public List<Map<String, Object>> listar(Long idInstituicao, String tipo) {
        List<Map<String, Object>> saida = new ArrayList<>();
        for (String t : List.of("administrador", "professor", "estudante")) {
            if (tipo != null && !tipo.equals(t)) {
                continue;
            }
            String tabela = tabelaDe(t);
            String pk = pkDe(t);
            String extra = t.equals("estudante") ? ", matricula" : "";
            for (Map<String, Object> l : db.consultar("SELECT " + pk + " AS id, nome, email, telefone" + extra
                    + " FROM " + tabela + " WHERE id_instituicao = ? ORDER BY nome", idInstituicao)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("tipo", t);
                item.putAll(l);
                saida.add(item);
            }
        }
        if (tipo != null && saida.isEmpty() && !fabricas.containsKey(tipo)) {
            throw new ValidacaoException("Tipo de usuário inválido.");
        }
        return saida;
    }

    /** Remove um usuário. ADM */
    public void remover(String tipo, long id) {
        if (!fabricas.containsKey(tipo)) {
            throw new ValidacaoException("Tipo de usuário inválido.");
        }
        String tabela = tabelaDe(tipo);
        String pk = pkDe(tipo);
        Map<String, Object> usuario = db.consultarUm(
                "SELECT id_instituicao FROM " + tabela + " WHERE " + pk + " = ?", id);
        if (usuario == null) {
            throw new NaoEncontradoException("Usuário não encontrado.");
        }
        if (tipo.equals("administrador")) {
            long total = Util.longValor(db.consultarUm(
                    "SELECT COUNT(*) AS c FROM administradores WHERE id_instituicao = ?",
                    usuario.get("id_instituicao")).get("c"));
            if (total <= 1) {
                throw new ConflitoException("Não é possível remover o último administrador da instituição.");
            }
        }
        db.executar("DELETE FROM " + tabela + " WHERE " + pk + " = ?", id);
    }

    private static String tabelaDe(String tipo) {
        return switch (tipo) {
            case "administrador" -> Administrador.TABELA;
            case "professor" -> Professor.TABELA;
            default -> Estudante.TABELA;
        };
    }

    private static String pkDe(String tipo) {
        return switch (tipo) {
            case "administrador" -> Administrador.PK;
            case "professor" -> Professor.PK;
            default -> Estudante.PK;
        };
    }
}
