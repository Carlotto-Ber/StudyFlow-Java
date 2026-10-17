package br.com.studyflow.model;

import br.com.studyflow.core.Senhas;

import java.util.LinkedHashMap;
import java.util.Map;

/** Classe base de Professor e Estudante. Cada subclasse aponta para sua tabela. */
public abstract class Usuario {
    protected Long id;
    protected final long idInstituicao;
    protected final String nome;
    protected final String email;
    protected final String telefone;
    protected final String cpf;
    protected String senhaHash;

    protected Usuario(long idInstituicao, String nome, String email, String telefone, String cpf) {
        this.idInstituicao = idInstituicao;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.cpf = cpf;
    }

    public abstract String tabela();

    public abstract String tipo();

    /** Quem pode criar tarefas e avisos. */
    public abstract boolean podePublicar();

    /** Texto curto que identifica o perfil. */
    public abstract String descricaoPerfil();

    public void definirSenha(String senha) {
        this.senhaHash = Senhas.gerarHash(senha);
    }

    public boolean verificarSenha(String senha) {
        return senhaHash != null && Senhas.verificar(senha, senhaHash);
    }

    /** Colunas gravadas no INSERT. As subclasses acrescentam as suas. */
    public Map<String, Object> colunas() {
        Map<String, Object> colunas = new LinkedHashMap<>();
        colunas.put("id_instituicao", idInstituicao);
        colunas.put("nome", nome);
        colunas.put("email", email);
        colunas.put("senha", senhaHash);
        colunas.put("telefone", telefone);
        colunas.put("cpf", cpf);
        return colunas;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("id", id);
        dados.put("tipo", tipo());
        dados.put("id_instituicao", idInstituicao);
        dados.put("nome", nome);
        dados.put("email", email);
        dados.put("telefone", telefone);
        dados.put("cpf", cpf);
        return dados;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getIdInstituicao() {
        return idInstituicao;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getCpf() {
        return cpf;
    }
}
