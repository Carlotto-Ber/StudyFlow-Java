package br.com.studyflow.model;

import java.util.Map;

public class Estudante extends Usuario {
    public static final String TABELA = "estudantes";
    public static final String PK = "id_estudante";

    private final String matricula;

    public Estudante(long idInstituicao, String nome, String email, String telefone, String cpf, String matricula) {
        super(idInstituicao, nome, email, telefone, cpf);
        this.matricula = matricula;
    }

    public String getMatricula() {
        return matricula;
    }

    @Override
    public String tabela() {
        return TABELA;
    }

    @Override
    public String tipo() {
        return "estudante";
    }

    @Override
    public Map<String, Object> colunas() {
        Map<String, Object> colunas = super.colunas();
        colunas.put("matricula", matricula);
        return colunas;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> dados = super.toMap();
        dados.put("matricula", matricula);
        return dados;
    }

    @Override
    public boolean podePublicar() {
        return false;
    }

    @Override
    public String descricaoPerfil() {
        return "Estudante " + nome + " (matrícula " + matricula + ")";
    }
}
