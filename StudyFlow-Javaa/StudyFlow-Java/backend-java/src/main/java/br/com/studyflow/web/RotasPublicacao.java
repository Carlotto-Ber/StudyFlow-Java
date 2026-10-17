package br.com.studyflow.web;

import br.com.studyflow.core.Validadores;
import br.com.studyflow.erros.ValidacaoException;
import br.com.studyflow.factory.AvisoFactory;
import br.com.studyflow.factory.ProvaFactory;
import br.com.studyflow.factory.PublicacaoFactory;
import br.com.studyflow.factory.TarefaFactory;
import br.com.studyflow.model.EventoCalendario;
import br.com.studyflow.model.Notificacao;
import br.com.studyflow.model.Nota;
import br.com.studyflow.model.Publicacao;

import java.util.Map;

/** CRUD de tarefas, avisos e provas todos criados pela PublicacaoFactory e o que os observers geram */
public class RotasPublicacao implements Rotas {

    // Cada recurso tem uma factory que sabe criar e carregar o objeto certo.
    private final Map<String, PublicacaoFactory> fabricas = Map.of(
            "tarefas", new TarefaFactory(),
            "avisos", new AvisoFactory(),
            "provas", new ProvaFactory());

    @Override
    public void registrar(Roteador r) {
        fabricas.forEach((recurso, fabrica) -> {
            r.get("/" + recurso, c -> Resposta.ok(fabrica.listar(c.numero("instituicao"), c.numeroOpcional("estudante"))));

            r.post("/" + recurso, c -> {
                Publicacao publicacao = fabrica.criar(c.json(), null); // Factory Method
                publicacao.salvar();                                   // dispara os Observers
                return Resposta.criado(publicacao.toMap());
            });

            r.put("/" + recurso + "/{id}", c -> {
                Publicacao publicacao = fabrica.carregar(c.id("id"));
                publicacao.atualizar(c.json());                        // dispara os Observers
                return Resposta.ok(publicacao.toMap());
            });

            r.delete("/" + recurso + "/{id}", c -> {
                fabrica.carregar(c.id("id")).excluir();
                return Resposta.semConteudo();
            });
        });

        // Notas das provas
        r.get("/provas/{id}/notas", c -> Resposta.ok(Nota.daProva(c.id("id"))));
        r.put("/provas/{id}/notas", c -> {
            Map<String, Object> d = c.json();
            Nota.lancar(c.id("id"), Validadores.converterInteiro(d.get("id_estudante"), "estudante"), d.get("nota"));
            return Resposta.ok(Nota.daProva(c.id("id")));
        });
        r.get("/notas", c -> Resposta.ok(Nota.doEstudante(c.numero("estudante"))));

        // Resultado dos observers
        r.get("/notificacoes", c -> {
            String tipo = c.texto("tipo", "tarefa");
            if (!tipo.equals("tarefa") && !tipo.equals("aviso") && !tipo.equals("prova")) {
                throw new ValidacaoException("Tipo de notificação inválido.");
            }
            return Resposta.ok(Notificacao.listarRecentes(c.numero("instituicao"), tipo, 12, c.numeroOpcional("estudante")));
        });
        r.get("/eventos", c -> {
            String tipo = c.texto("tipo", null);
            if (tipo != null && !tipo.equals("tarefa") && !tipo.equals("prova")) {
                throw new ValidacaoException("Tipo de evento inválido.");
            }
            return Resposta.ok(EventoCalendario.listar(c.numero("instituicao"), tipo, 12, c.numeroOpcional("estudante")));
        });
    }
}
