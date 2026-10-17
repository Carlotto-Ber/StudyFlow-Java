package br.com.studyflow.web;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Reúne os grupos de rotas. Não conhece HTTP recebe método, caminho, consulta e corpo e devolve uma Resposta */
public class Api {
    private static final String PREFIXO = "/api";

    private final Roteador roteador = new Roteador();

    public Api() {
        List<Rotas> grupos = List.of(
                new RotasInstituicao(),
                new RotasDisciplina(),
                new RotasPublicacao(),
                new RotasUsuario(),
                new RotasSessao(),
                new RotasLaboratorio(),
                new RotasResumo());
        grupos.forEach(g -> g.registrar(roteador));
    }

    public Resposta tratar(String metodo, String caminho, Map<String, String> consulta, Supplier<String> corpo) {
        return roteador.despachar(metodo, caminho.substring(PREFIXO.length()), consulta, corpo);
    }
}
