package br.com.studyflow.web;

import br.com.studyflow.model.Instituicao;
import br.com.studyflow.model.SessaoEstudo;

import java.util.LinkedHashMap;
import java.util.Map;

public class RotasSessao implements Rotas {
    @Override
    public void registrar(Roteador r) {
        r.get("/sessoes", c -> {
            Instituicao instituicao = Instituicao.buscar(c.numero("instituicao"));
            instituicao.exigirModulo("sessoes");
            Long estudante = c.numeroOpcional("estudante");
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("sessoes", SessaoEstudo.listar(instituicao.getId(), estudante, 30));
            corpo.put("resumo", SessaoEstudo.resumoSemanal(instituicao.getId(), estudante));
            return Resposta.ok(corpo);
        });

        r.post("/sessoes", c -> {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("id", SessaoEstudo.registrar(c.json()));
            return Resposta.criado(corpo);
        });

        r.delete("/sessoes/{id}", c -> {
            SessaoEstudo.excluir(c.id("id"));
            return Resposta.semConteudo();
        });
    }
}
