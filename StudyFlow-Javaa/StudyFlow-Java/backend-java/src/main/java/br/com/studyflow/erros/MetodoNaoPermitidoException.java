package br.com.studyflow.erros;

public class MetodoNaoPermitidoException extends ErroDeNegocio {
    private static final long serialVersionUID = 1L;

    public MetodoNaoPermitidoException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int status() {
        return 405;
    }
}
