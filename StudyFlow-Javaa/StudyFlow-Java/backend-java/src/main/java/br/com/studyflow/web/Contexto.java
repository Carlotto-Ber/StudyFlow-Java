package br.com.studyflow.web;

import br.com.studyflow.core.Json;
import br.com.studyflow.core.Validadores;
import br.com.studyflow.erros.NaoEncontradoException;
import br.com.studyflow.erros.ValidacaoException;

import java.util.Map;
import java.util.function.Supplier;

/** Tudo o que uma rota precisa saber sobre a requisição */
public record Contexto(Map<String, String> caminho, Map<String, String> consulta, Supplier<String> corpo) {

    /** Id numérico de um trecho do caminho (/tarefas/{id}) Não numérico = 404 */
    public long id(String nome) {
        try {
            return Long.parseLong(caminho.get(nome));
        } catch (NumberFormatException e) {
            throw new NaoEncontradoException("Recurso não encontrado.");
        }
    }

    /** Parâmetro obrigatório da query string, como número */
    public long numero(String nome) {
        return Validadores.converterInteiro(consulta.get(nome), nome);
    }

    /** Parâmetro opcional da query string, como número (null se ausente) */
    public Long numeroOpcional(String nome) {
        String valor = consulta.get(nome);
        return valor == null || valor.isBlank() ? null : Validadores.converterInteiro(valor, nome);
    }

    public String texto(String nome, String padrao) {
        String valor = consulta.get(nome);
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    /** Corpo da requisição como objeto JSON */
    @SuppressWarnings("unchecked")
    public Map<String, Object> json() {
        Object dado = null;
        try {
            String texto = corpo.get();
            if (texto != null && !texto.isBlank()) {
                dado = Json.parse(texto);
            }
        } catch (IllegalArgumentException e) {
            dado = null;
        }
        if (!(dado instanceof Map<?, ?> mapa)) {
            throw new ValidacaoException("Envie os dados em formato JSON.");
        }
        return (Map<String, Object>) mapa;
    }
}
