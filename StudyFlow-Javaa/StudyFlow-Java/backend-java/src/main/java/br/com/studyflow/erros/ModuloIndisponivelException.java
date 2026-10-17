package br.com.studyflow.erros;

public class ModuloIndisponivelException extends ErroDeNegocio {
    private static final long serialVersionUID = 1L;

    public ModuloIndisponivelException(String mensagem) {
        super(mensagem);
    }

    @Override
    public int status() {
        return 403;
    }
}
