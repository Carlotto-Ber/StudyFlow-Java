package br.com.studyflow.web;

import br.com.studyflow.erros.MetodoNaoPermitidoException;
import br.com.studyflow.erros.NaoEncontradoException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Liga método e caminho a uma rota. Caminhos aceitam variáveis /disciplinas/{id}/matriculas */
public class Roteador {
    private record Entrada(String metodo, Pattern padrao, List<String> variaveis, Rota rota) {
    }

    private final List<Entrada> entradas = new ArrayList<>();

    public void adicionar(String metodo, String padrao, Rota rota) {
        List<String> variaveis = new ArrayList<>();
        StringBuilder regex = new StringBuilder("^");
        Matcher m = Pattern.compile("\\{(\\w+)}").matcher(padrao);
        int ultimo = 0;
        while (m.find()) {
            regex.append(Pattern.quote(padrao.substring(ultimo, m.start()))).append("([^/]+)");
            variaveis.add(m.group(1));
            ultimo = m.end();
        }
        regex.append(Pattern.quote(padrao.substring(ultimo))).append("$");
        entradas.add(new Entrada(metodo, Pattern.compile(regex.toString()), variaveis, rota));
    }

    public void get(String padrao, Rota rota) {
        adicionar("GET", padrao, rota);
    }

    public void post(String padrao, Rota rota) {
        adicionar("POST", padrao, rota);
    }

    public void put(String padrao, Rota rota) {
        adicionar("PUT", padrao, rota);
    }

    public void delete(String padrao, Rota rota) {
        adicionar("DELETE", padrao, rota);
    }

    public Resposta despachar(String metodo, String caminho, Map<String, String> consulta, Supplier<String> corpo) {
        boolean caminhoExiste = false;
        for (Entrada entrada : entradas) {
            Matcher m = entrada.padrao().matcher(caminho);
            if (!m.matches()) {
                continue;
            }
            caminhoExiste = true;
            if (!entrada.metodo().equals(metodo)) {
                continue;
            }
            Map<String, String> variaveis = new LinkedHashMap<>();
            for (int i = 0; i < entrada.variaveis().size(); i++) {
                variaveis.put(entrada.variaveis().get(i), m.group(i + 1));
            }
            return entrada.rota().tratar(new Contexto(variaveis, consulta, corpo));
        }
        if (caminhoExiste) {
            throw new MetodoNaoPermitidoException("Método não permitido.");
        }
        throw new NaoEncontradoException("Recurso não encontrado.");
    }
}
