package br.com.studyflow.web;

/** Resposta da API: status HTTP e corpo (convertido para JSON) Corpo nulo = sem conteúdo */
public record Resposta(int status, Object corpo) {
    public static Resposta ok(Object corpo) {
        return new Resposta(200, corpo);
    }

    public static Resposta criado(Object corpo) {
        return new Resposta(201, corpo);
    }

    public static Resposta semConteudo() {
        return new Resposta(204, null);
    }
}
