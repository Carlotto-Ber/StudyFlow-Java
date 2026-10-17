package br.com.studyflow.erros;

public class ValidacaoException extends ErroDeNegocio {
    private static final long serialVersionUID = 1L;

    public ValidacaoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int status() {
        return 400;
    }
}
