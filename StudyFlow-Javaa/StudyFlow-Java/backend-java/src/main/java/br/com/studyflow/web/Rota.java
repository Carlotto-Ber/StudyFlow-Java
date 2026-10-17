package br.com.studyflow.web;

@FunctionalInterface
public interface Rota {
    Resposta tratar(Contexto contexto);
}
