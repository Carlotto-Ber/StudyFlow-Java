package br.com.studyflow.erros;

public abstract class ErroDeNegocio extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ErroDeNegocio(String mensagem) {
        super(mensagem);
    }

    public abstract int status();
}
