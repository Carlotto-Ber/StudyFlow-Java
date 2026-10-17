package br.com.studyflow.web;

import br.com.studyflow.core.Util;
import br.com.studyflow.core.Validadores;
import br.com.studyflow.model.Disciplina;
import br.com.studyflow.model.Instituicao;

import java.util.Map;

public class RotasDisciplina implements Rotas {
    @Override
    public void registrar(Roteador r) {
        r.get("/disciplinas", c -> Resposta.ok(
                Disciplina.listarDetalhado(c.numero("instituicao"), c.numeroOpcional("estudante"))));

        r.post("/disciplinas", c -> {
            Map<String, Object> d = c.json();
            long idInstituicao = Validadores.converterInteiro(d.get("id_instituicao"), "instituição");
            Instituicao.buscar(idInstituicao);
            Disciplina disciplina = new Disciplina(idInstituicao, Util.texto(d.get("nome")), Util.texto(d.get("descricao")),
                    Disciplina.professorDe(d.get("id_professor")), Disciplina.creditosDe(d.getOrDefault("creditos", 4)), null)
                    .salvar();
            // StudyFlow Individual: quem cria a disciplina já fica matriculado nela
            Long estudante = Disciplina.idOpcional(d.get("matricular_estudante"), "estudante");
            if (estudante != null) {
                disciplina.matricular(estudante);
            }
            return Resposta.criado(disciplina.toMap());
        });

        r.put("/disciplinas/{id}", c -> Resposta.ok(Disciplina.buscar(c.id("id")).atualizar(c.json()).toMap()));

        r.delete("/disciplinas/{id}", c -> {
            Disciplina.buscar(c.id("id")).excluir();
            return Resposta.semConteudo();
        });

        r.get("/disciplinas/{id}/matriculas", c -> Resposta.ok(Disciplina.buscar(c.id("id")).situacaoDasMatriculas()));

        r.put("/disciplinas/{id}/matriculas", c -> {
            Disciplina disciplina = Disciplina.buscar(c.id("id"));
            Map<String, Object> d = c.json();
            long estudante = Validadores.converterInteiro(d.get("id_estudante"), "estudante");
            if (Boolean.TRUE.equals(d.get("matriculado"))) {
                disciplina.matricular(estudante);
            } else {
                disciplina.desmatricular(estudante);
            }
            return Resposta.ok(disciplina.situacaoDasMatriculas());
        });
    }
}
