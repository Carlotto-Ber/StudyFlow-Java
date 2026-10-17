package br.com.studyflow.model;

public class Administrador extends Usuario {
    public static final String TABELA = "administradores";
    public static final String PK = "id_administrador";

    public Administrador(long idInstituicao, String nome, String email, String telefone, String cpf) {
        super(idInstituicao, nome, email, telefone, cpf);
    }

    @Override
    public String tabela() {
        return TABELA;
    }

    @Override
    public String tipo() {
        return "administrador";
    }

    @Override
    public boolean podePublicar() {
        return true;
    }

    @Override
    public String descricaoPerfil() {
        return "Administrador(a) " + nome;
    }
}
