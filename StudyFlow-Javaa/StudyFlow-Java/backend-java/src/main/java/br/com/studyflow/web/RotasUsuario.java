package br.com.studyflow.web;

import br.com.studyflow.core.Util;
import br.com.studyflow.service.CadastroService;

import java.util.Map;

public class RotasUsuario implements Rotas {
    @Override
    public void registrar(Roteador r) {
        r.get("/usuarios", c -> Resposta.ok(
                CadastroService.getInstance().listar(c.numero("instituicao"), c.texto("tipo", null))));

        // Singleton + Factory Method: o CadastroService escolhe a factory do tipo pedido
        r.post("/cadastro", c -> {
            Map<String, Object> dados = c.json();
            return Resposta.criado(CadastroService.getInstance().registrar(Util.texto(dados.get("tipo")), dados).toMap());
        });

        r.delete("/usuarios/{tipo}/{id}", c -> {
            CadastroService.getInstance().remover(c.caminho().get("tipo"), c.id("id"));
            return Resposta.semConteudo();
        });
    }
}
