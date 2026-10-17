package br.com.studyflow.web;

import br.com.studyflow.model.Instituicao;

public class RotasInstituicao implements Rotas {
    @Override
    public void registrar(Roteador r) {
        r.get("/instituicoes", c -> Resposta.ok(Instituicao.listar().stream().map(Instituicao::toMap).toList()));

        // Tela de Personalização: nome, cor, termos, método de média, módulos e canais
        r.put("/instituicoes/{id}", c -> {
            Instituicao instituicao = Instituicao.buscar(c.id("id"));
            instituicao.atualizarConfiguracao(c.json());
            return Resposta.ok(Instituicao.buscar(c.id("id")).toMap());
        });
    }
}
