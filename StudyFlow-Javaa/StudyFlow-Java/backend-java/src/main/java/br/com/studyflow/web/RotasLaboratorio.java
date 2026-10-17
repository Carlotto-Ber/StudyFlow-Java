package br.com.studyflow.web;

import br.com.studyflow.model.Instituicao;
import br.com.studyflow.model.Laboratorio;
import br.com.studyflow.model.ReservaLaboratorio;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Módulo exclusivo do Instituto Atlas toda rota exige o módulo laboratorio */
public class RotasLaboratorio implements Rotas {
    @Override
    public void registrar(Roteador r) {
        r.get("/laboratorios", c -> {
            Instituicao.buscar(c.numero("instituicao")).exigirModulo("laboratorio");
            return Resposta.ok(Laboratorio.listar(c.numero("instituicao")));
        });

        r.put("/laboratorios/{id}", c -> {
            Laboratorio.alterarEstado(c.id("id"), c.json().get("estado"));
            return Resposta.ok(Laboratorio.buscar(c.id("id")));
        });

        r.get("/reservas", c -> {
            Instituicao.buscar(c.numero("instituicao")).exigirModulo("laboratorio");
            return Resposta.ok(ReservaLaboratorio.listar(c.numero("instituicao"), LocalDate.now(), 30));
        });

        r.post("/reservas", c -> {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("id", ReservaLaboratorio.registrar(c.json()));
            return Resposta.criado(corpo);
        });

        r.delete("/reservas/{id}", c -> {
            ReservaLaboratorio.excluir(c.id("id"));
            return Resposta.semConteudo();
        });
    }
}
