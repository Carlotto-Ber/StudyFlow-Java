package br.com.studyflow.erros;

public class ConflitoException extends ErroDeNegocio {
    private static final long serialVersionUID = 1L;

    public ConflitoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int status() {
        return 409;
    }
}
