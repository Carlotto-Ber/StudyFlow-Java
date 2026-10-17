package br.com.studyflow.core;

public class ErroBancoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ErroBancoException(Throwable causa) {
        super(causa);
    }
}
