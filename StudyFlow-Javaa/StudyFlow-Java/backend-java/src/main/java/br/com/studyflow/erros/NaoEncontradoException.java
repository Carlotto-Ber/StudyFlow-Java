package br.com.studyflow.erros;

public class NaoEncontradoException extends ErroDeNegocio {
    private static final long serialVersionUID = 1L;

    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int status() {
        return 404;
    }
}
