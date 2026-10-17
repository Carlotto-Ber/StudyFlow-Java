package br.com.studyflow.model;

public class Professor extends Usuario {
    public static final String TABELA = "professores";
    public static final String PK = "id_professor";

    public Professor(long idInstituicao, String nome, String email, String telefone, String cpf) {
        super(idInstituicao, nome, email, telefone, cpf);
    }

    @Override
    public String tabela() {
        return TABELA;
    }

    @Override
    public String tipo() {
        return "professor";
    }

    @Override
    public boolean podePublicar() {
        return true;
    }

    @Override
    public String descricaoPerfil() {
        return "Professor(a) " + nome;
    }
}
